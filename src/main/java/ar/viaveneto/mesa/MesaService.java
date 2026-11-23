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
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MesaService {

	private final MesaRepository mesas;
	private final PedidoRepository pedidos;

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
		Mesa mesa = mesas.findByNumero(nueva.numero())
				.map(existente -> {
					if (existente.isActiva()) {
						throw new ReglaNegocioException("Ya existe la mesa " + nueva.numero() + ".");
					}
					existente.reactivar();
					return existente;
				})
				.orElseGet(() -> mesas.save(new Mesa(nueva.numero())));
		return resumir(mesa, null);
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
		if (abierto == null) {
			return new MesaResumen(mesa.getId(), mesa.getNumero(), false, null, null, 0, BigDecimal.ZERO);
		}
		return new MesaResumen(mesa.getId(), mesa.getNumero(), true, abierto.getId(),
				abierto.getAbiertoEn(), abierto.cantidadDeUnidades(), abierto.total());
	}
}
