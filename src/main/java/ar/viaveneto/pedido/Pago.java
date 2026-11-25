package ar.viaveneto.pedido;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import ar.viaveneto.caja.Turno;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Una parte del cobro de un pedido. Una mesa puede pagar con varios pagos (dividir la cuenta). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pago {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Pedido pedido;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	private Turno turno;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 15)
	private MedioPago medio;

	/** Lo que cancela de la cuenta. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal monto;

	/** Se registra aparte: no es venta, se reparte. */
	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal propina;

	/** Con cuánto pagó en efectivo, para calcular el vuelto en el ticket. */
	@Column(precision = 12, scale = 2)
	private BigDecimal recibido;

	@Column(nullable = false)
	private LocalDateTime fecha;

	Pago(Pedido pedido, Turno turno, MedioPago medio, BigDecimal monto, BigDecimal propina, BigDecimal recibido,
			LocalDateTime fecha) {
		this.pedido = pedido;
		this.turno = turno;
		this.medio = medio;
		this.monto = monto;
		this.propina = propina;
		this.recibido = recibido;
		this.fecha = fecha;
	}

	public BigDecimal vuelto() {
		return recibido == null ? BigDecimal.ZERO : recibido.subtract(monto).subtract(propina).max(BigDecimal.ZERO);
	}
}
