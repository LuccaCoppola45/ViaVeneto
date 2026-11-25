package ar.viaveneto.mesa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.pedido.Pedido;
import ar.viaveneto.pedido.PedidoRepository;
import ar.viaveneto.salon.Plano;
import ar.viaveneto.salon.SalonService;
import ar.viaveneto.salon.Sector;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaService {

	private static final int PASO_BUSQUEDA = 20;
	private static final int SEPARACION = 20;
	private static final String SUFIJOS = "BCDEFGHIJKLMNOPQRSTUVWXYZ";

	private final MesaRepository mesas;
	private final PedidoRepository pedidos;
	private final SalonService salonService;

	@Transactional(readOnly = true)
	public List<MesaResumen> listar() {
		Map<Long, Pedido> abiertosPorMesa = pedidos.findAbiertosDeMesasConItems().stream()
				.collect(Collectors.toMap(p -> p.getMesa().getId(), Function.identity()));

		return mesas.findByActivaTrueOrderByNumeroAscSufijoAsc().stream()
				.map(mesa -> resumir(mesa, abiertosPorMesa.get(mesa.getId())))
				.toList();
	}

	@Transactional
	public MesaResumen agregar(NuevaMesa nueva) {
		Sector sector = nueva.sectorId() == null ? salonService.primerSector() : salonService.buscarSector(nueva.sectorId());
		int[] lugar = lugarLibre(sector);
		Mesa mesa = mesas.findByNumeroAndSufijo(nueva.numero(), "")
				.map(existente -> {
					if (existente.isActiva()) {
						throw new ReglaNegocioException("Ya existe la mesa " + nueva.numero() + ".");
					}
					existente.reactivar(Plano.MESITAS_HABITUALES, sector, lugar[0], lugar[1], false);
					return existente;
				})
				.orElseGet(() -> mesas.save(new Mesa(nueva.numero(), sector, lugar[0], lugar[1])));
		return resumir(mesa, null);
	}

	/**
	 * Saca una mesita individual de una mesa y la suma a otra (por ejemplo, para armar una mesa de 6).
	 * Si la mesa de origen se queda sin mesitas, deja de existir: así la 4B vuelve a juntarse con la 4.
	 */
	@Transactional
	public List<MesaResumen> pasarMesita(Long origenId, PasarMesita pase) {
		if (origenId.equals(pase.destinoId())) {
			throw new ReglaNegocioException("Elegí otra mesa para pasarle la mesita.");
		}
		// Se bloquean siempre en el mismo orden para que dos tablets no se traben entre sí.
		Mesa primera = paraActualizar(Math.min(origenId, pase.destinoId()));
		Mesa segunda = paraActualizar(Math.max(origenId, pase.destinoId()));
		Mesa origen = primera.getId().equals(origenId) ? primera : segunda;
		Mesa destino = origen == primera ? segunda : primera;

		if (destino.getMesitas() >= Plano.MAXIMO_MESITAS) {
			throw new ReglaNegocioException("La mesa " + destino.etiqueta() + " ya tiene " + Plano.MAXIMO_MESITAS
					+ " mesitas, no entra otra.");
		}
		if (origen.getMesitas() == 1 && estaOcupada(origen)) {
			throw new ReglaNegocioException("La mesa " + origen.etiqueta() + " está ocupada y se quedaría sin mesa.");
		}

		origen.sumarMesitas(-1);
		destino.sumarMesitas(1);
		if (origen.getMesitas() == 0) {
			origen.desactivar();
		}
		return listar();
	}

	/** Parte una mesa en dos (la 4 queda como 4 y 4B). Si estaba ocupada, el pedido sigue en la original. */
	@Transactional
	public List<MesaResumen> partir(Long mesaId) {
		Mesa original = paraActualizar(mesaId);
		if (original.getMesitas() < 2) {
			throw new ReglaNegocioException("La mesa " + original.etiqueta() + " tiene una sola mesita, no se puede partir.");
		}
		int mesitasNueva = original.getMesitas() / 2;
		original.sumarMesitas(-mesitasNueva);

		int x = original.isVertical() ? original.getX() : original.getX() + original.ancho() + SEPARACION;
		int y = original.isVertical() ? original.getY() + original.alto() + SEPARACION : original.getY();
		String sufijo = sufijoLibre(original.getNumero());
		mesas.findByNumeroAndSufijo(original.getNumero(), sufijo)
				.ifPresentOrElse(
						vieja -> vieja.reactivar(mesitasNueva, original.getSector(), x, y, original.isVertical()),
						() -> mesas.save(new Mesa(original.getNumero(), sufijo, mesitasNueva, original.getSector(), x, y,
								original.isVertical())));
		return listar();
	}

	@Transactional
	public MesaResumen cambiarNumero(Long mesaId, NuevoNumero nuevo) {
		Mesa mesa = buscar(mesaId);
		if (mesa.getNumero() != nuevo.numero()) {
			mesas.findByNumeroAndSufijo(nuevo.numero(), mesa.getSufijo()).ifPresent(otra -> {
				throw new ReglaNegocioException(otra.isActiva()
						? "Ya hay otra mesa " + otra.etiqueta() + "."
						: "El número " + otra.etiqueta() + " lo usó una mesa que se quitó. Agregala de nuevo desde \"Carta y mesas\".");
			});
			mesa.cambiarNumero(nuevo.numero());
		}
		return resumir(mesa, pedidos.findAbiertoDeMesa(mesaId).orElse(null));
	}

	@Transactional
	public void quitar(Long mesaId) {
		Mesa mesa = buscar(mesaId);
		if (estaOcupada(mesa)) {
			throw new ReglaNegocioException("La mesa " + mesa.etiqueta() + " está ocupada. Cerrala antes de quitarla.");
		}
		mesa.desactivar();
	}

	Mesa buscar(Long mesaId) {
		return mesas.findById(mesaId)
				.orElseThrow(() -> new NoEncontradoException("No existe esa mesa."));
	}

	private Mesa paraActualizar(Long mesaId) {
		return mesas.findByIdParaActualizar(mesaId)
				.filter(Mesa::isActiva)
				.orElseThrow(() -> new NoEncontradoException("Esa mesa ya no existe. Recargá la pantalla."));
	}

	private boolean estaOcupada(Mesa mesa) {
		return pedidos.findAbiertoDeMesa(mesa.getId()).isPresent();
	}

	/** La primera letra que no usa ninguna mesa activa con ese número. */
	private String sufijoLibre(int numero) {
		Set<String> enUso = mesas.findByNumero(numero).stream()
				.filter(Mesa::isActiva)
				.map(Mesa::getSufijo)
				.collect(Collectors.toSet());
		return SUFIJOS.chars()
				.mapToObj(letra -> String.valueOf((char) letra))
				.filter(sufijo -> !enUso.contains(sufijo))
				.findFirst()
				.orElseThrow(() -> new ReglaNegocioException("La mesa " + numero + " ya está partida en demasiadas partes."));
	}

	/** Primer hueco del plano (de arriba a la izquierda) donde entra una mesa sin pisar otra. */
	private int[] lugarLibre(Sector sector) {
		List<Mesa> delSector = mesas.findByActivaTrueOrderByNumeroAscSufijoAsc().stream()
				.filter(m -> m.getSector() != null && m.getSector().getId().equals(sector.getId()))
				.toList();
		for (int y = SEPARACION; y <= Plano.ALTO - Plano.MESA_CORTO; y += PASO_BUSQUEDA) {
			for (int x = SEPARACION; x <= Plano.ANCHO - Plano.MESA_LARGO; x += PASO_BUSQUEDA) {
				int cx = x;
				int cy = y;
				if (delSector.stream().noneMatch(m -> seTocan(cx, cy, m))) {
					return new int[] { x, y };
				}
			}
		}
		return new int[] { SEPARACION, SEPARACION };
	}

	private static boolean seTocan(int x, int y, Mesa otra) {
		return x < otra.getX() + otra.ancho() + SEPARACION && otra.getX() < x + Plano.MESA_LARGO + SEPARACION
				&& y < otra.getY() + otra.alto() + SEPARACION && otra.getY() < y + Plano.MESA_CORTO + SEPARACION;
	}

	private MesaResumen resumir(Mesa mesa, Pedido abierto) {
		Long sectorId = mesa.getSector() == null ? null : mesa.getSector().getId();
		if (abierto == null) {
			return new MesaResumen(mesa.getId(), mesa.getNumero(), mesa.etiqueta(), mesa.getMesitas(), sectorId,
					mesa.getX(), mesa.getY(), mesa.isVertical(), false, null, null, null, null, 0, BigDecimal.ZERO);
		}
		return new MesaResumen(mesa.getId(), mesa.getNumero(), mesa.etiqueta(), mesa.getMesitas(), sectorId,
				mesa.getX(), mesa.getY(), mesa.isVertical(), true, abierto.getId(),
				abierto.getAtiende() == null ? null : abierto.getAtiende().getId(),
				abierto.getAtiende() == null ? null : abierto.getAtiende().getNombre(), abierto.getAbiertoEn(),
				abierto.cantidadDeUnidades(), abierto.total());
	}
}
