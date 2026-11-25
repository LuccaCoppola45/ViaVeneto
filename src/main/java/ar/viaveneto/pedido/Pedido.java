package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import ar.viaveneto.acceso.Usuario;
import ar.viaveneto.caja.Turno;
import ar.viaveneto.carta.Producto;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.Plata;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.Mesa;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(indexes = @Index(name = "idx_pedido_estado", columnList = "estado"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pedido {

	public static final int MAXIMO_POR_ITEM = 99;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private TipoPedido tipo;

	/** Solo para pedidos de tipo MESA. */
	@ManyToOne(fetch = FetchType.LAZY)
	private Mesa mesa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private EstadoPedido estado;

	/** La moza que lo atiende (la que lo abrió, salvo que lo pasen a otra). */
	@ManyToOne(fetch = FetchType.LAZY)
	private Usuario atiende;

	/** Para llevar o Rappi: a nombre de quién está (o el número de pedido de Rappi). */
	@Column(length = 60)
	private String cliente;

	/** Solo para pedidos sin mesa. */
	@Enumerated(EnumType.STRING)
	@Column(length = 15)
	private EstadoEntrega entrega;

	@Column(nullable = false)
	private LocalDateTime abiertoEn;

	private LocalDateTime cerradoEn;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ItemPedido> items = new ArrayList<>();

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<Pago> pagos = new ArrayList<>();

	/** Por ejemplo 10 cuando pagan en efectivo. Se aplica sobre toda la cuenta. */
	@Column(nullable = false)
	@ColumnDefault("0")
	private int descuentoPorcentaje;

	public static Pedido deMesa(Mesa mesa, Usuario atiende, LocalDateTime ahora) {
		Pedido pedido = nuevo(TipoPedido.MESA, atiende, ahora);
		pedido.mesa = mesa;
		return pedido;
	}

	/** Para llevar o Rappi: sin mesa, a nombre de alguien. */
	public static Pedido sinMesa(TipoPedido tipo, String cliente, Usuario atiende, LocalDateTime ahora) {
		if (tipo == TipoPedido.MESA) {
			throw new IllegalArgumentException("Los pedidos de mesa se abren desde la mesa");
		}
		Pedido pedido = nuevo(tipo, atiende, ahora);
		pedido.cliente = cliente == null || cliente.isBlank() ? null : cliente.trim();
		pedido.entrega = EstadoEntrega.PREPARANDO;
		return pedido;
	}

	private static Pedido nuevo(TipoPedido tipo, Usuario atiende, LocalDateTime ahora) {
		Pedido pedido = new Pedido();
		pedido.tipo = tipo;
		pedido.atiende = atiende;
		pedido.estado = EstadoPedido.ABIERTO;
		pedido.abiertoEn = ahora;
		return pedido;
	}

	public void pasarA(Usuario otra) {
		this.atiende = otra;
	}

	public void marcarEntrega(EstadoEntrega estado) {
		if (entrega == null) {
			throw new ReglaNegocioException("Las mesas no tienen estado de entrega.");
		}
		if (this.estado == EstadoPedido.ANULADO) {
			throw new ReglaNegocioException("Este pedido se canceló.");
		}
		this.entrega = estado;
	}

	/** Lo que se muestra en pantalla y en el ticket: "Mesa 4", "Para llevar · Juan", "Rappi · 1234". */
	public String titulo() {
		return switch (tipo) {
			case MESA -> "Mesa " + mesa.etiqueta();
			case PARA_LLEVAR -> cliente == null ? "Para llevar" : "Para llevar · " + cliente;
			case DELIVERY -> cliente == null ? "Rappi" : "Rappi · " + cliente;
		};
	}

	/** Si el producto ya estaba cargado (sin nota) suma unidades en la misma línea. */
	public void agregar(Producto producto, int unidades) {
		verificarAbierto();
		items.stream()
				.filter(item -> item.getProducto().getId().equals(producto.getId()) && item.getNota() == null)
				.findFirst()
				.ifPresentOrElse(
						item -> item.cambiarCantidad(limitar(item.getCantidad() + unidades)),
						() -> items.add(new ItemPedido(this, producto, limitar(unidades))));
	}

	/**
	 * Le pone una nota a una línea. Con {@code soloUna} la nota va a una sola unidad:
	 * de "3 milanesas" quedan "2 milanesas" y "1 milanesa · sin sal".
	 */
	public void ponerNota(Long itemId, String nota, boolean soloUna) {
		verificarAbierto();
		ItemPedido item = buscarItem(itemId);
		String limpia = nota == null || nota.isBlank() ? null : nota.trim();
		if (!soloUna || item.getCantidad() == 1) {
			item.ponerNota(limpia);
			juntarIguales(item);
			return;
		}
		item.cambiarCantidad(item.getCantidad() - 1);
		ItemPedido igual = items.stream()
				.filter(i -> i != item && i.esDelMismoProducto(item) && i.tieneNota(limpia))
				.findFirst()
				.orElse(null);
		if (igual != null) {
			igual.cambiarCantidad(limitar(igual.getCantidad() + 1));
		} else {
			items.add(new ItemPedido(item, 1, limpia));
		}
	}

	/** Con cantidad 0 la línea se borra. */
	public void cambiarCantidad(Long itemId, int cantidad) {
		verificarAbierto();
		ItemPedido item = buscarItem(itemId);
		if (cantidad == 0) {
			items.remove(item);
		} else {
			item.cambiarCantidad(limitar(cantidad));
		}
		verificarQueNoSeCobroDeMas();
	}

	public void aplicarDescuento(int porcentaje) {
		verificarAbierto();
		this.descuentoPorcentaje = porcentaje;
		verificarQueNoSeCobroDeMas();
	}

	/** Cobra una parte (o todo). Cuando no falta nada, el pedido se cierra solo. */
	public Pago cobrar(Turno turno, MedioPago medio, BigDecimal monto, BigDecimal propina, BigDecimal recibido,
			LocalDateTime ahora) {
		verificarAbierto();
		if (items.isEmpty()) {
			throw new ReglaNegocioException("La mesa no tiene nada cargado para cobrar.");
		}
		if (medio == MedioPago.RAPPI && tipo != TipoPedido.DELIVERY) {
			throw new ReglaNegocioException("Rappi se usa solo para los pedidos de Rappi.");
		}
		BigDecimal aCobrar = Plata.redondear(monto);
		if (aCobrar.signum() <= 0) {
			throw new ReglaNegocioException("Poné cuánto paga.");
		}
		if (aCobrar.compareTo(saldo()) > 0) {
			throw new ReglaNegocioException("Está pagando más de lo que falta (" + Plata.formato(saldo()) + ").");
		}
		BigDecimal laPropina = propina == null ? BigDecimal.ZERO : Plata.redondear(propina);
		if (recibido != null && Plata.redondear(recibido).compareTo(aCobrar.add(laPropina)) < 0) {
			throw new ReglaNegocioException("Con " + Plata.formato(recibido) + " no alcanza para pagar "
					+ Plata.formato(aCobrar.add(laPropina)) + ".");
		}
		Pago pago = new Pago(this, turno, medio, aCobrar, laPropina, recibido == null ? null : Plata.redondear(recibido), ahora);
		pagos.add(pago);
		if (saldo().signum() == 0) {
			this.estado = EstadoPedido.CERRADO;
			this.cerradoEn = ahora;
		}
		return pago;
	}

	/** Por si se cargó mal un pago: mientras la mesa siga abierta se puede borrar. */
	public void anularPago(Long pagoId) {
		verificarAbierto();
		boolean borrado = pagos.removeIf(p -> p.getId().equals(pagoId));
		if (!borrado) {
			throw new NoEncontradoException("Ese pago ya no está.");
		}
	}

	/** Sin nada cargado la mesa se libera; con consumo, solo se cierra cobrando. */
	public void cerrar(LocalDateTime ahora) {
		verificarAbierto();
		if (!items.isEmpty() && saldo().signum() > 0) {
			throw new ReglaNegocioException("Falta cobrar " + Plata.formato(saldo()) + ".");
		}
		this.estado = items.isEmpty() ? EstadoPedido.ANULADO : EstadoPedido.CERRADO;
		this.cerradoEn = ahora;
	}

	/** Lo que suman los platos, sin descuento. */
	public BigDecimal subtotal() {
		return items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal descuento() {
		return Plata.porcentaje(subtotal(), descuentoPorcentaje);
	}

	/** Lo que hay que cobrar: subtotal menos el descuento. */
	public BigDecimal total() {
		return subtotal().subtract(descuento());
	}

	public BigDecimal pagado() {
		return pagos.stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal saldo() {
		return total().subtract(pagado());
	}

	public int cantidadDeUnidades() {
		return items.stream().mapToInt(ItemPedido::getCantidad).sum();
	}

	private ItemPedido buscarItem(Long itemId) {
		return items.stream()
				.filter(i -> i.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new NoEncontradoException("Ese producto ya no está en el pedido."));
	}

	/** Si al ponerle nota quedó igual a otra línea, las junta en una. */
	private void juntarIguales(ItemPedido item) {
		items.stream()
				.filter(i -> i != item && i.esDelMismoProducto(item) && i.tieneNota(item.getNota()))
				.findFirst()
				.ifPresent(igual -> {
					igual.cambiarCantidad(limitar(igual.getCantidad() + item.getCantidad()));
					items.remove(item);
				});
	}

	private void verificarQueNoSeCobroDeMas() {
		if (saldo().signum() < 0) {
			throw new ReglaNegocioException("Ya se cobraron " + Plata.formato(pagado())
					+ " y la cuenta quedaría en " + Plata.formato(total()) + ". Borrá un pago primero.");
		}
	}

	private void verificarAbierto() {
		if (estado != EstadoPedido.ABIERTO) {
			throw new ReglaNegocioException("Este pedido ya está cerrado.");
		}
	}

	private static int limitar(int cantidad) {
		if (cantidad > MAXIMO_POR_ITEM) {
			throw new ReglaNegocioException("No se pueden cargar más de " + MAXIMO_POR_ITEM + " unidades de un mismo producto.");
		}
		return cantidad;
	}
}
