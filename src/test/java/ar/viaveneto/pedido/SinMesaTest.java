package ar.viaveneto.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.acceso.AccesoDtos.NuevoUsuario;
import ar.viaveneto.acceso.AccesoService;
import ar.viaveneto.caja.CajaDtos.PorMedio;
import ar.viaveneto.caja.CajaService;
import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.MesaResumen;
import ar.viaveneto.mesa.MesaService;

@SpringBootTest
@Transactional
class SinMesaTest {

	@Autowired PedidoService pedidoService;
	@Autowired MesaService mesaService;
	@Autowired CartaService cartaService;
	@Autowired CajaService cajaService;
	@Autowired AccesoService accesoService;

	@Test
	void paraLlevarSigueEnLaListaHastaQueLoRetiran() {
		PedidoDetalle pedido = pedidoService.abrirSinMesa(new CantidadForm.NuevoSinMesa(TipoPedido.PARA_LLEVAR, "Juan"), null);
		assertThat(pedido.titulo()).isEqualTo("Para llevar · Juan");
		assertThat(pedido.entrega()).isEqualTo(EstadoEntrega.PREPARANDO);

		PedidoDetalle conAlgo = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Empanada"), 6));
		pedidoService.cobrar(pedido.id(), new CantidadForm.Cobro(MedioPago.EFECTIVO, conAlgo.total(), null, null));
		assertThat(enCurso()).as("cobrado pero sin retirar").contains(pedido.id());

		pedidoService.marcarEntrega(pedido.id(), new CantidadForm.Entrega(EstadoEntrega.ENTREGADO));
		assertThat(enCurso()).doesNotContain(pedido.id());
	}

	@Test
	void rappiSeCobraConRappiYNoSumaAlCajon() {
		PedidoDetalle pedido = pedidoService.abrirSinMesa(new CantidadForm.NuevoSinMesa(TipoPedido.DELIVERY, "4821"), null);
		PedidoDetalle conAlgo = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Milanesa"), 1));
		pedidoService.cobrar(pedido.id(), new CantidadForm.Cobro(MedioPago.RAPPI, conAlgo.total(), null, null));

		var turno = cajaService.caja().turnoAbierto();
		BigDecimal rappi = turno.resumen().porMedio().stream().filter(m -> m.medio() == MedioPago.RAPPI)
				.map(PorMedio::monto).findFirst().orElseThrow();
		assertThat(rappi).isEqualByComparingTo(conAlgo.total());
		assertThat(turno.efectivoEsperado()).as("Rappi no va al cajón").isEqualByComparingTo(turno.fondoInicial());
	}

	@Test
	void unaMesaNoSePuedeCobrarConRappi() {
		PedidoDetalle pedido = pedidoService.abrirMesa(mesaService.listar().getFirst().id());
		PedidoDetalle conAlgo = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Tiramisú"), 1));
		assertThatThrownBy(() -> pedidoService.cobrar(pedido.id(), new CantidadForm.Cobro(MedioPago.RAPPI, conAlgo.total(), null, null)))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void laMesaQuedaANombreDeQuienLaAbreYSePuedePasar() {
		Long lucia = accesoService.crearPrimerUsuario(new NuevoUsuario("Lucía", "1357")).usuario().id();
		Long sofia = accesoService.crear(new NuevoUsuario("Sofía", "2468")).id();
		Long mesaId = mesaService.listar().getFirst().id();

		PedidoDetalle pedido = pedidoService.abrirMesa(mesaId, lucia);
		assertThat(mesa(mesaId).atiendeNombre()).isEqualTo("Lucía");

		pedidoService.pasarA(pedido.id(), new CantidadForm.Atiende(sofia));
		assertThat(mesa(mesaId).atiendeNombre()).isEqualTo("Sofía");
	}

	private List<Long> enCurso() {
		return pedidoService.sinMesaEnCurso().stream().map(PedidoSinMesa::id).toList();
	}

	private MesaResumen mesa(Long id) {
		return mesaService.listar().stream().filter(m -> m.id().equals(id)).findFirst().orElseThrow();
	}

	private Long producto(String empiezaCon) {
		return cartaService.carta(true).stream().map(CategoriaVista::productos).flatMap(List::stream)
				.filter(p -> p.nombre().startsWith(empiezaCon)).findFirst().orElseThrow().id();
	}
}
