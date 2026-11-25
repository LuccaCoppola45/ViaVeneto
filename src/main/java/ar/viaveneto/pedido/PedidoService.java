package ar.viaveneto.pedido;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.caja.CajaService;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.carta.Producto;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.Mesa;
import ar.viaveneto.mesa.MesaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

	private final PedidoRepository pedidos;
	private final MesaRepository mesas;
	private final CartaService cartaService;
	private final CajaService cajaService;
	private final Clock reloj;

	/** Si la mesa ya estaba abierta devuelve ese pedido en lugar de fallar. */
	@Transactional
	public PedidoDetalle abrirMesa(Long mesaId) {
		Mesa mesa = mesas.findByIdParaActualizar(mesaId)
				.filter(Mesa::isActiva)
				.orElseThrow(() -> new NoEncontradoException("No existe esa mesa."));
		Pedido pedido = pedidos.findAbiertoDeMesa(mesaId)
				.orElseGet(() -> pedidos.save(Pedido.deMesa(mesa, ahora())));
		return PedidoDetalle.de(pedido);
	}

	@Transactional(readOnly = true)
	public PedidoDetalle ver(Long pedidoId) {
		return PedidoDetalle.de(pedidos.findDetalle(pedidoId)
				.orElseThrow(() -> new NoEncontradoException("No existe ese pedido.")));
	}

	@Transactional
	public PedidoDetalle agregarItem(Long pedidoId, CantidadForm.AgregarItem form) {
		Pedido pedido = paraActualizar(pedidoId);
		Producto producto = cartaService.buscarProducto(form.productoId());
		if (!producto.isActivo()) {
			throw new ReglaNegocioException(producto.getNombre() + " no está disponible en la carta.");
		}
		pedido.agregar(producto, form.cantidad());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle cambiarCantidad(Long pedidoId, Long itemId, CantidadForm.CambiarCantidad form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.cambiarCantidad(itemId, form.cantidad());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle ponerNota(Long pedidoId, Long itemId, CantidadForm.Nota form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.ponerNota(itemId, form.nota(), form.soloUna());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle aplicarDescuento(Long pedidoId, CantidadForm.Descuento form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.aplicarDescuento(form.porcentaje());
		return guardar(pedido);
	}

	/** Registra un pago en el turno de caja abierto (si no hay ninguno, lo abre solo). */
	@Transactional
	public PedidoDetalle cobrar(Long pedidoId, CantidadForm.Cobro form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.cobrar(cajaService.turnoParaCobrar(), form.medio(), form.monto(), form.propina(), form.recibido(), ahora());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle anularPago(Long pedidoId, Long pagoId) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.anularPago(pagoId);
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle cerrar(Long pedidoId) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.cerrar(ahora());
		return guardar(pedido);
	}

	private Pedido paraActualizar(Long pedidoId) {
		return pedidos.findByIdParaActualizar(pedidoId)
				.orElseThrow(() -> new NoEncontradoException("No existe ese pedido."));
	}

	/** saveAndFlush para que los ítems nuevos tengan id en la respuesta. */
	private PedidoDetalle guardar(Pedido pedido) {
		return PedidoDetalle.de(pedidos.saveAndFlush(pedido));
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj);
	}
}
