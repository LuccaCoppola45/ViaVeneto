package ar.viaveneto.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaDtos.ProductoVista;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.MesaResumen;
import ar.viaveneto.mesa.MesaService;

@SpringBootTest
@Transactional
class MesasYPedidosTest {

	@Autowired MesaService mesaService;
	@Autowired PedidoService pedidoService;
	@Autowired CartaService cartaService;

	@Test
	void abrirCargarYCerrarUnaMesa() {
		MesaResumen mesa = mesaService.listar().getFirst();
		assertThat(mesa.ocupada()).isFalse();

		PedidoDetalle pedido = pedidoService.abrirMesa(mesa.id());
		ProductoVista empanada = producto("Empanada de carne");
		pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(empanada.id(), 3));
		pedido = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(empanada.id(), 2));

		assertThat(pedido.items()).hasSize(1);
		assertThat(pedido.items().getFirst().cantidad()).isEqualTo(5);
		assertThat(pedido.total()).isEqualByComparingTo(empanada.precio().multiply(BigDecimal.valueOf(5)));

		MesaResumen ocupada = mesaService.listar().getFirst();
		assertThat(ocupada.ocupada()).isTrue();
		assertThat(ocupada.cantidadItems()).isEqualTo(5);

		assertThatThrownBy(() -> pedidoService.cerrar(pedidoId(ocupada))).as("no se cierra sin cobrar")
				.isInstanceOf(ReglaNegocioException.class);
		PedidoDetalle cobrado = pedidoService.cobrar(pedido.id(),
				new CantidadForm.Cobro(MedioPago.EFECTIVO, pedido.total(), null, null));
		assertThat(cobrado.estado()).as("al cobrar todo se cierra sola").isEqualTo(EstadoPedido.CERRADO);
		assertThat(mesaService.listar().getFirst().ocupada()).isFalse();
	}

	private static Long pedidoId(MesaResumen mesa) {
		return mesa.pedidoId();
	}

	@Test
	void abrirDosVecesDevuelveElMismoPedido() {
		Long mesaId = mesaService.listar().getFirst().id();
		assertThat(pedidoService.abrirMesa(mesaId).id()).isEqualTo(pedidoService.abrirMesa(mesaId).id());
	}

	@Test
	void cerrarUnaMesaSinNadaLaAnula() {
		PedidoDetalle pedido = pedidoService.abrirMesa(mesaService.listar().getFirst().id());
		assertThat(pedidoService.cerrar(pedido.id()).estado()).isEqualTo(EstadoPedido.ANULADO);
	}

	@Test
	void cantidadCeroBorraLaLinea() {
		PedidoDetalle pedido = pedidoService.abrirMesa(mesaService.listar().getFirst().id());
		pedido = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Tiramisú").id(), 1));
		pedido = pedidoService.cambiarCantidad(pedido.id(), pedido.items().getFirst().id(), new CantidadForm.CambiarCantidad(0));
		assertThat(pedido.items()).isEmpty();
	}

	@Test
	void noSePuedeCargarEnUnPedidoCerrado() {
		PedidoDetalle pedido = pedidoService.abrirMesa(mesaService.listar().getFirst().id());
		Long productoId = producto("Tiramisú").id();
		PedidoDetalle conAlgo = pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(productoId, 1));
		pedidoService.cobrar(pedido.id(), new CantidadForm.Cobro(MedioPago.DEBITO, conAlgo.total(), null, null));

		assertThatThrownBy(() -> pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(productoId, 1)))
				.isInstanceOf(ReglaNegocioException.class);
	}

	@Test
	void noSePuedeQuitarUnaMesaOcupada() {
		Long mesaId = mesaService.listar().getFirst().id();
		pedidoService.abrirMesa(mesaId);
		assertThatThrownBy(() -> mesaService.quitar(mesaId)).isInstanceOf(ReglaNegocioException.class);
	}

	private ProductoVista producto(String nombre) {
		return cartaService.carta(true).stream()
				.map(CategoriaVista::productos)
				.flatMap(java.util.List::stream)
				.filter(p -> p.nombre().equals(nombre))
				.findFirst()
				.orElseThrow();
	}
}
