package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.viaveneto.carta.Producto;
import ar.viaveneto.comun.NoEncontradoException;
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

	@Column(nullable = false)
	private LocalDateTime abiertoEn;

	private LocalDateTime cerradoEn;

	@OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id")
	private List<ItemPedido> items = new ArrayList<>();

	public static Pedido deMesa(Mesa mesa, LocalDateTime ahora) {
		Pedido pedido = new Pedido();
		pedido.tipo = TipoPedido.MESA;
		pedido.mesa = mesa;
		pedido.estado = EstadoPedido.ABIERTO;
		pedido.abiertoEn = ahora;
		return pedido;
	}

	/** Si el producto ya estaba cargado suma unidades en la misma línea. */
	public void agregar(Producto producto, int unidades) {
		verificarAbierto();
		items.stream()
				.filter(item -> item.getProducto().getId().equals(producto.getId()))
				.findFirst()
				.ifPresentOrElse(
						item -> item.cambiarCantidad(limitar(item.getCantidad() + unidades)),
						() -> items.add(new ItemPedido(this, producto, limitar(unidades))));
	}

	/** Con cantidad 0 la línea se borra. */
	public void cambiarCantidad(Long itemId, int cantidad) {
		verificarAbierto();
		ItemPedido item = items.stream()
				.filter(i -> i.getId().equals(itemId))
				.findFirst()
				.orElseThrow(() -> new NoEncontradoException("Ese producto ya no está en el pedido."));
		if (cantidad == 0) {
			items.remove(item);
		} else {
			item.cambiarCantidad(limitar(cantidad));
		}
	}

	public void cerrar(LocalDateTime ahora) {
		verificarAbierto();
		this.estado = items.isEmpty() ? EstadoPedido.ANULADO : EstadoPedido.CERRADO;
		this.cerradoEn = ahora;
	}

	public BigDecimal total() {
		return items.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public int cantidadDeUnidades() {
		return items.stream().mapToInt(ItemPedido::getCantidad).sum();
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
