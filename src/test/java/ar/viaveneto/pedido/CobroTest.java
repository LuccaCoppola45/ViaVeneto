package ar.viaveneto.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.caja.CajaDtos.AbrirTurno;
import ar.viaveneto.caja.CajaDtos.CajaVista;
import ar.viaveneto.caja.CajaDtos.CerrarTurno;
import ar.viaveneto.caja.CajaDtos.PorMedio;
import ar.viaveneto.caja.CajaDtos.TurnoVista;
import ar.viaveneto.caja.CajaService;
import ar.viaveneto.caja.TipoTurno;
import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.MesaService;

@SpringBootTest
@Transactional
class CobroTest {

	@Autowired MesaService mesaService;
	@Autowired PedidoService pedidoService;
	@Autowired CartaService cartaService;
	@Autowired CajaService cajaService;

	@Test
	void dividirEnPartesIgualesConDistintosMedios() {
		PedidoDetalle pedido = mesaConMilanesas(2); // 2 x 18.000 = 36.000
		pedidoService.cobrar(pedido.id(), cobro(MedioPago.EFECTIVO, 12000, 0, 15000));
		PedidoDetalle parcial = pedidoService.cobrar(pedido.id(), cobro(MedioPago.DEBITO, 12000, 1000, null));

		assertThat(parcial.estado()).isEqualTo(EstadoPedido.ABIERTO);
		assertThat(parcial.saldo()).isEqualByComparingTo("12000");
		assertThat(parcial.pagos().getFirst().vuelto()).isEqualByComparingTo("3000");

		PedidoDetalle cerrado = pedidoService.cobrar(pedido.id(), cobro(MedioPago.TRANSFERENCIA, 12000, 0, null));
		assertThat(cerrado.estado()).isEqualTo(EstadoPedido.CERRADO);
	}

	@Test
	void descuentoDelDiezPorCientoEnEfectivo() {
		PedidoDetalle pedido = mesaConMilanesas(2);
		PedidoDetalle conDescuento = pedidoService.aplicarDescuento(pedido.id(), new CantidadForm.Descuento(10));

		assertThat(conDescuento.descuento()).isEqualByComparingTo("3600");
		assertThat(conDescuento.total()).isEqualByComparingTo("32400");
	}

	@Test
	void noSePuedeCobrarDeMas() {
		PedidoDetalle pedido = mesaConMilanesas(1);
		assertThatThrownBy(() -> pedidoService.cobrar(pedido.id(), cobro(MedioPago.EFECTIVO, 20000, 0, null)))
				.isInstanceOf(ReglaNegocioException.class);
		assertThatThrownBy(() -> pedidoService.cobrar(pedido.id(), cobro(MedioPago.EFECTIVO, 18000, 0, 10000)))
				.as("con 10.000 no alcanza").isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void notaParaUnaSolaUnidadSeparaLaLinea() {
		PedidoDetalle pedido = mesaConMilanesas(3);
		Long itemId = pedido.items().getFirst().id();
		PedidoDetalle conNota = pedidoService.ponerNota(pedido.id(), itemId, new CantidadForm.Nota("sin sal", true));

		assertThat(conNota.items()).hasSize(2);
		assertThat(conNota.items()).extracting(PedidoDetalle.Item::cantidad).containsExactly(2, 1);
		assertThat(conNota.items().get(1).nota()).isEqualTo("sin sal");

		// Si se le saca la nota vuelve a juntarse con las otras dos
		PedidoDetalle sinNota = pedidoService.ponerNota(pedido.id(), conNota.items().get(1).id(), new CantidadForm.Nota("", false));
		assertThat(sinNota.items()).hasSize(1);
		assertThat(sinNota.items().getFirst().cantidad()).isEqualTo(3);
	}

	@Test
	void laCajaSumaPorMedioSeparaPropinasYCalculaLaDiferencia() {
		TurnoVista turno = cajaService.abrir(new AbrirTurno(TipoTurno.NOCHE, "Mamá", new BigDecimal("10000")));
		PedidoDetalle pedido = mesaConMilanesas(2);
		pedidoService.cobrar(pedido.id(), cobro(MedioPago.EFECTIVO, 20000, 0, null));
		pedidoService.cobrar(pedido.id(), cobro(MedioPago.CREDITO, 16000, 2000, null));

		CajaVista caja = cajaService.caja();
		assertThat(caja.turnoAbierto().id()).isEqualTo(turno.id());
		assertThat(monto(caja.turnoAbierto(), MedioPago.EFECTIVO)).isEqualByComparingTo("20000");
		assertThat(monto(caja.turnoAbierto(), MedioPago.CREDITO)).isEqualByComparingTo("16000");
		assertThat(caja.turnoAbierto().resumen().propinas()).isEqualByComparingTo("2000");
		assertThat(caja.turnoAbierto().efectivoEsperado()).isEqualByComparingTo("30000");

		TurnoVista cerrado = cajaService.cerrar(turno.id(), new CerrarTurno(new BigDecimal("29500"), "faltan 500"));
		assertThat(cerrado.diferencia()).isEqualByComparingTo("-500");
	}

	@Test
	void laCajaMuestraLoCobradoSinFacturarHastaQueSeMarca() {
		PedidoDetalle pedido = mesaConMilanesas(1);
		assertThatThrownBy(() -> pedidoService.marcarFacturado(pedido.id(), new CantidadForm.Facturado(true)))
				.as("sin cobrar no se factura").isInstanceOf(ReglaNegocioException.class);

		pedidoService.cobrar(pedido.id(), cobro(MedioPago.DEBITO, 18000, 0, null));
		assertThat(cajaService.caja().turnoAbierto().resumen().sinFacturar())
				.extracting(s -> s.pedidoId()).containsExactly(pedido.id());

		assertThat(pedidoService.marcarFacturado(pedido.id(), new CantidadForm.Facturado(true)).facturado()).isTrue();
		assertThat(cajaService.caja().turnoAbierto().resumen().sinFacturar()).isEmpty();
	}

	@Test
	void siNadieAbrioLaCajaSeAbreSolaAlCobrar() {
		PedidoDetalle pedido = mesaConMilanesas(1);
		pedidoService.cobrar(pedido.id(), cobro(MedioPago.DEBITO, 18000, 0, null));
		assertThat(cajaService.caja().turnoAbierto().automatico()).isTrue();
	}

	private PedidoDetalle mesaConMilanesas(int cantidad) {
		PedidoDetalle pedido = pedidoService.abrirMesa(mesaService.listar().getFirst().id());
		Long milanesa = cartaService.carta(true).stream()
				.map(CategoriaVista::productos)
				.flatMap(java.util.List::stream)
				.filter(p -> p.nombre().startsWith("Milanesa"))
				.findFirst().orElseThrow().id();
		return pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(milanesa, cantidad));
	}

	private static CantidadForm.Cobro cobro(MedioPago medio, int monto, int propina, Integer recibido) {
		return new CantidadForm.Cobro(medio, BigDecimal.valueOf(monto), BigDecimal.valueOf(propina),
				recibido == null ? null : BigDecimal.valueOf(recibido));
	}

	private static BigDecimal monto(TurnoVista turno, MedioPago medio) {
		return turno.resumen().porMedio().stream().filter(m -> m.medio() == medio).map(PorMedio::monto).findFirst().orElseThrow();
	}
}
