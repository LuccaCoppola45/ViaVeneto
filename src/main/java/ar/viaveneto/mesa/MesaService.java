package ar.viaveneto.mesa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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

	private final MesaRepository mesas;
	private final PedidoRepository pedidos;
	private final SalonService salonService;

	private static final int PASO_BUSQUEDA = 20;
	private static final int SEPARACION = 20;

	@Transactional(readOnly = true)
	public List<MesaResumen> listar() {
		Map<Long, Pedido> abiertosPorMesa = pedidos.findAbiertosDeMesasConItems().stream()
				.collect(Collectors.toMap(p -> p.getMesa().getId(), Function.identity()));

		return mesas.findByActivaTrueOrderByNumero().stream()
				.map(mesa -> resumir(mesa, abiertosPorMesa.get(mesa.getId())))
				.toList();
	}

	@Transactional
	public MesaResumen agregar(NuevaMesa nueva) {
		Sector sector = nueva.sectorId() == null ? salonService.primerSector() : salonService.buscarSector(nueva.sectorId());
		int[] lugar = lugarLibre(sector);
		Mesa mesa = mesas.findByNumero(nueva.numero())
				.map(existente -> {
					if (existente.isActiva()) {
						throw new ReglaNegocioException("Ya existe la mesa " + nueva.numero() + ".");
					}
					existente.reactivar();
					existente.ubicar(sector, lugar[0], lugar[1], false);
					return existente;
				})
				.orElseGet(() -> mesas.save(new Mesa(nueva.numero(), sector, lugar[0], lugar[1])));
		return resumir(mesa, null);
	}

	/** Primer hueco del plano (de arriba a la izquierda) donde entra una mesa sin pisar otra. */
	private int[] lugarLibre(Sector sector) {
		List<Mesa> delSector = mesas.findByActivaTrueOrderByNumero().stream()
				.filter(m -> m.getSector() != null && m.getSector().getId().equals(sector.getId()))
				.toList();
		for (int y = SEPARACION; y <= Plano.ALTO - Plano.MESA_CORTO; y += PASO_BUSQUEDA) {
			for (int x = SEPARACION; x <= Plano.ANCHO - Plano.MESA_LARGO; x += PASO_BUSQUEDA) {
				int cx = x;
				int cy = y;
				boolean libre = delSector.stream().noneMatch(m -> seTocan(cx, cy, m));
				if (libre) {
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

	@Transactional
	public MesaResumen cambiarNumero(Long mesaId, NuevoNumero nuevo) {
		Mesa mesa = buscar(mesaId);
		if (mesa.getNumero() == nuevo.numero()) {
			return resumir(mesa, pedidos.findAbiertoDeMesa(mesaId).orElse(null));
		}
		mesas.findByNumero(nuevo.numero()).ifPresent(otra -> {
			throw new ReglaNegocioException(otra.isActiva()
					? "Ya hay otra mesa con el número " + nuevo.numero() + "."
					: "El número " + nuevo.numero() + " lo usó una mesa que se quitó. Agregala de nuevo desde \"Carta y mesas\".");
		});
		mesa.cambiarNumero(nuevo.numero());
		return resumir(mesa, pedidos.findAbiertoDeMesa(mesaId).orElse(null));
	}

	@Transactional
	public void quitar(Long mesaId) {
		Mesa mesa = buscar(mesaId);
		if (pedidos.findAbiertoDeMesa(mesaId).isPresent()) {
			throw new ReglaNegocioException("La mesa " + mesa.getNumero() + " está ocupada. Cerrala antes de quitarla.");
		}
		mesa.desactivar();
	}

	Mesa buscar(Long mesaId) {
		return mesas.findById(mesaId)
				.orElseThrow(() -> new NoEncontradoException("No existe esa mesa."));
	}

	private MesaResumen resumir(Mesa mesa, Pedido abierto) {
		Long sectorId = mesa.getSector() == null ? null : mesa.getSector().getId();
		if (abierto == null) {
			return new MesaResumen(mesa.getId(), mesa.getNumero(), sectorId, mesa.getX(), mesa.getY(), mesa.isVertical(),
					false, null, null, 0, BigDecimal.ZERO);
		}
		return new MesaResumen(mesa.getId(), mesa.getNumero(), sectorId, mesa.getX(), mesa.getY(), mesa.isVertical(),
				true, abierto.getId(), abierto.getAbiertoEn(), abierto.cantidadDeUnidades(), abierto.total());
	}
}
