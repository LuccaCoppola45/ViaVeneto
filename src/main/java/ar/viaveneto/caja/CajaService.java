package ar.viaveneto.caja;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.caja.CajaDtos.AbrirTurno;
import ar.viaveneto.caja.CajaDtos.CajaVista;
import ar.viaveneto.caja.CajaDtos.CerrarTurno;
import ar.viaveneto.caja.CajaDtos.PorMedio;
import ar.viaveneto.caja.CajaDtos.Resumen;
import ar.viaveneto.caja.CajaDtos.Sugerencia;
import ar.viaveneto.caja.CajaDtos.TurnoVista;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.Plata;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.pedido.EstadoPedido;
import ar.viaveneto.pedido.MedioPago;
import ar.viaveneto.pedido.Pago;
import ar.viaveneto.pedido.PagoRepository;
import ar.viaveneto.pedido.Pedido;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CajaService {

	/** Antes de esta hora el turno sugerido es el del mediodía. */
	private static final LocalTime EMPIEZA_LA_NOCHE = LocalTime.of(17, 0);
	/** La jornada arranca a las 5 de la mañana: lo que se cobra a la 1 am sigue siendo "la noche de hoy". */
	private static final LocalTime EMPIEZA_LA_JORNADA = LocalTime.of(5, 0);
	private static final String SIN_NOMBRE = "Sin nombre";

	private final TurnoRepository turnos;
	private final PagoRepository pagos;
	private final Clock reloj;

	@Transactional(readOnly = true)
	public CajaVista caja() {
		List<Turno> deHoy = turnos.findByAbiertoEnGreaterThanEqualOrderByAbiertoEn(inicioDeJornada());
		Map<Long, List<Pago>> pagosPorTurno = pagosDe(deHoy.stream().map(Turno::getId).toList());
		List<TurnoVista> vistas = deHoy.stream()
				.map(t -> vista(t, pagosPorTurno.getOrDefault(t.getId(), List.of())))
				.toList();
		TurnoVista abierto = vistas.stream().filter(v -> v.cerradoEn() == null).findFirst()
				.orElseGet(() -> turnos.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc()
						.map(t -> vista(t, pagosDe(List.of(t.getId())).getOrDefault(t.getId(), List.of())))
						.orElse(null));
		Resumen dia = resumir(pagosPorTurno.values().stream().flatMap(List::stream).toList());
		return new CajaVista(abierto, vistas, dia, sugerencia());
	}

	@Transactional
	public synchronized TurnoVista abrir(AbrirTurno form) {
		turnos.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc().ifPresent(abierto -> {
			throw new ReglaNegocioException("Ya está abierto el turno " + nombre(abierto.getTipo()) + " de "
					+ abierto.getResponsable() + ". Cerralo antes de abrir otro.");
		});
		Turno turno = turnos.save(new Turno(form.tipo(), form.responsable(), Plata.redondear(form.fondoInicial()), ahora(), false));
		return vista(turno, List.of());
	}

	@Transactional
	public TurnoVista cerrar(Long turnoId, CerrarTurno form) {
		Turno turno = buscar(turnoId);
		turno.cerrar(Plata.redondear(form.efectivoContado()), form.observaciones(), ahora());
		return vista(turno, pagosDe(List.of(turnoId)).getOrDefault(turnoId, List.of()));
	}

	@Transactional(readOnly = true)
	public TurnoVista ver(Long turnoId) {
		return vista(buscar(turnoId), pagosDe(List.of(turnoId)).getOrDefault(turnoId, List.of()));
	}

	/**
	 * El turno donde se anota un cobro. Si nadie abrió la caja, se abre sola:
	 * nunca se traba un cobro por eso. Después se le puede corregir el nombre al cerrar.
	 */
	public synchronized Turno turnoParaCobrar() {
		return turnos.findFirstByCerradoEnIsNullOrderByAbiertoEnDesc()
				.orElseGet(() -> {
					Sugerencia sugerencia = sugerencia();
					return turnos.saveAndFlush(new Turno(sugerencia.tipo(), SIN_NOMBRE, sugerencia.fondoInicial(), ahora(), true));
				});
	}

	/** Se propone el turno según la hora y, como cambio, lo que se contó al cerrar el turno anterior. */
	private Sugerencia sugerencia() {
		TipoTurno tipo = ahora().toLocalTime().isBefore(EMPIEZA_LA_NOCHE) && !ahora().toLocalTime().isBefore(EMPIEZA_LA_JORNADA)
				? TipoTurno.MEDIODIA
				: TipoTurno.NOCHE;
		BigDecimal fondo = turnos.findFirstByCerradoEnIsNotNullOrderByCerradoEnDesc()
				.map(Turno::getEfectivoContado)
				.filter(Objects::nonNull)
				.orElse(BigDecimal.ZERO);
		return new Sugerencia(tipo, fondo);
	}

	private TurnoVista vista(Turno turno, List<Pago> delTurno) {
		Resumen resumen = resumir(delTurno);
		BigDecimal efectivo = resumen.porMedio().stream()
				.filter(m -> m.medio() == MedioPago.EFECTIVO)
				.map(PorMedio::monto)
				.findFirst()
				.orElse(BigDecimal.ZERO);
		BigDecimal esperado = turno.getFondoInicial().add(efectivo);
		BigDecimal diferencia = turno.getEfectivoContado() == null ? null : turno.getEfectivoContado().subtract(esperado);
		return new TurnoVista(turno.getId(), turno.getTipo(), turno.getResponsable(), turno.isAutomatico(),
				turno.getAbiertoEn(), turno.getCerradoEn(), turno.getFondoInicial(), resumen, esperado,
				turno.getEfectivoContado(), diferencia, turno.getObservaciones());
	}

	private Resumen resumir(List<Pago> lista) {
		Map<MedioPago, List<Pago>> porMedio = lista.stream().collect(Collectors.groupingBy(Pago::getMedio));
		List<PorMedio> lineas = Arrays.stream(MedioPago.values())
				.map(medio -> {
					List<Pago> delMedio = porMedio.getOrDefault(medio, List.of());
					return new PorMedio(medio, delMedio.size(), sumar(delMedio, Pago::getMonto), sumar(delMedio, Pago::getPropina));
				})
				.toList();
		List<Pedido> pedidos = lista.stream().map(Pago::getPedido).distinct().toList();
		BigDecimal descuentos = pedidos.stream()
				.filter(p -> p.getEstado() == EstadoPedido.CERRADO)
				.map(Pedido::descuento)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new Resumen(lineas, sumar(lista, Pago::getMonto), sumar(lista, Pago::getPropina), descuentos, pedidos.size());
	}

	private Map<Long, List<Pago>> pagosDe(Collection<Long> turnoIds) {
		if (turnoIds.isEmpty()) {
			return Map.of();
		}
		return pagos.findByTurnos(turnoIds).stream().collect(Collectors.groupingBy(p -> p.getTurno().getId()));
	}

	private static BigDecimal sumar(List<Pago> lista, Function<Pago, BigDecimal> campo) {
		return lista.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private Turno buscar(Long turnoId) {
		return turnos.findById(turnoId).orElseThrow(() -> new NoEncontradoException("No existe ese turno."));
	}

	private LocalDateTime inicioDeJornada() {
		LocalDateTime ahora = ahora();
		LocalDateTime hoyALas5 = ahora.toLocalDate().atTime(EMPIEZA_LA_JORNADA);
		return ahora.isBefore(hoyALas5) ? hoyALas5.minusDays(1) : hoyALas5;
	}

	private LocalDateTime ahora() {
		return LocalDateTime.now(reloj);
	}

	private static String nombre(TipoTurno tipo) {
		return tipo == TipoTurno.MEDIODIA ? "del mediodía" : "de la noche";
	}
}
