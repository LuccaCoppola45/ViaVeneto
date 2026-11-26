package ar.viaveneto.pedido;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.acceso.Usuario;
import ar.viaveneto.acceso.UsuarioRepository;
import ar.viaveneto.caja.CajaService;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.carta.Producto;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.Mesa;
import ar.viaveneto.mesa.MesaRepository;
import ar.viaveneto.stock.StockService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PedidoService {

	private final PedidoRepository pedidos;
	private final MesaRepository mesas;
	private final CartaService cartaService;
	private final CajaService cajaService;
	private final StockService stockService;
	private final UsuarioRepository usuarios;
	private final Clock reloj;

	@Transactional
	public PedidoDetalle abrirMesa(Long mesaId) {
		return abrirMesa(mesaId, null);
	}

	/**
	 * Si la mesa ya estaba abierta devuelve ese pedido en lugar de fallar.
	 * La mesa queda a nombre de quien la abre (como la libreta dividida por moza).
	 */
	@Transactional
	public PedidoDetalle abrirMesa(Long mesaId, Long usuarioId) {
		Mesa mesa = mesas.findByIdParaActualizar(mesaId)
				.filter(Mesa::isActiva)
				.orElseThrow(() -> new NoEncontradoException("No existe esa mesa."));
		Pedido pedido = pedidos.findAbiertoDeMesa(mesaId)
				.orElseGet(() -> pedidos.save(Pedido.deMesa(mesa, usuario(usuarioId), ahora())));
		return PedidoDetalle.de(pedido);
	}

	/** Un pedido para llevar o de Rappi. */
	@Transactional
	public PedidoDetalle abrirSinMesa(CantidadForm.NuevoSinMesa form, Long usuarioId) {
		if (form.tipo() == TipoPedido.MESA) {
			throw new ReglaNegocioException("Los pedidos de mesa se abren tocando la mesa.");
		}
		return PedidoDetalle.de(pedidos.save(Pedido.sinMesa(form.tipo(), form.cliente(), usuario(usuarioId), ahora())));
	}

	@Transactional(readOnly = true)
	public List<PedidoSinMesa> sinMesaEnCurso() {
		return pedidos.findSinMesaEnCurso().stream().map(PedidoSinMesa::de).toList();
	}

	@Transactional
	public PedidoDetalle marcarEntrega(Long pedidoId, CantidadForm.Entrega form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.marcarEntrega(form.estado());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle marcarFacturado(Long pedidoId, CantidadForm.Facturado form) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.marcarFacturado(form.facturado());
		return guardar(pedido);
	}

	/** Cuando una moza le pasa la mesa a otra. */
	@Transactional
	public PedidoDetalle pasarA(Long pedidoId, CantidadForm.Atiende form) {
		Pedido pedido = paraActualizar(pedidoId);
		Usuario otra = usuarios.findById(form.usuarioId())
				.filter(Usuario::isActivo)
				.orElseThrow(() -> new NoEncontradoException("No existe esa persona."));
		pedido.pasarA(otra);
		return guardar(pedido);
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
		stockService.consumir(producto, form.cantidad());
		pedido.agregar(producto, form.cantidad());
		return guardar(pedido);
	}

	@Transactional
	public PedidoDetalle cambiarCantidad(Long pedidoId, Long itemId, CantidadForm.CambiarCantidad form) {
		Pedido pedido = paraActualizar(pedidoId);
		ItemPedido item = pedido.getItems().stream()
				.filter(i -> i.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new NoEncontradoException("Ese producto ya no está en el pedido."));
		int diferencia = form.cantidad() - item.getCantidad();
		Producto producto = item.getProducto();
		pedido.cambiarCantidad(itemId, form.cantidad());
		// Si suben la cantidad, descuenta; si la bajan o sacan el plato, el stock vuelve.
		stockService.consumir(producto, diferencia);
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
		return cobrar(pedidoId, form, null);
	}

	@Transactional
	public PedidoDetalle cobrar(Long pedidoId, CantidadForm.Cobro form, String quienCobra) {
		Pedido pedido = paraActualizar(pedidoId);
		pedido.cobrar(cajaService.turnoParaCobrar(quienCobra), form.medio(), form.monto(), form.propina(), form.recibido(),
				ahora());
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

	private Usuario usuario(Long usuarioId) {
		return usuarioId == null ? null : usuarios.findById(usuarioId).orElse(null);
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
