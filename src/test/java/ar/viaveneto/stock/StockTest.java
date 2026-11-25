package ar.viaveneto.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaDtos.ProductoVista;
import ar.viaveneto.carta.CartaService;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.mesa.MesaService;
import ar.viaveneto.pedido.CantidadForm;
import ar.viaveneto.pedido.PedidoDetalle;
import ar.viaveneto.pedido.PedidoService;
import ar.viaveneto.stock.StockDtos.InsumoVista;
import ar.viaveneto.stock.StockDtos.LineaForm;
import ar.viaveneto.stock.StockDtos.RecetaForm;

@SpringBootTest
@Transactional
class StockTest {

	@Autowired StockService stockService;
	@Autowired PedidoService pedidoService;
	@Autowired MesaService mesaService;
	@Autowired CartaService cartaService;

	@Test
	void cargarUnaMilanesaDescuentaLaCarneYLasPapas() {
		PedidoDetalle pedido = abrirMesa();
		pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Milanesa").id(), 2));

		assertThat(insumo("Milanesas de carne").cantidad()).isEqualByComparingTo("28");
		assertThat(insumo("Papas").cantidad()).isEqualByComparingTo("9.5");
	}

	@Test
	void sacarElPlatoDevuelveElStock() {
		PedidoDetalle pedido = pedidoService.agregarItem(abrirMesa().id(),
				new CantidadForm.AgregarItem(producto("Cerveza").id(), 3));
		pedidoService.cambiarCantidad(pedido.id(), pedido.items().getFirst().id(), new CantidadForm.CambiarCantidad(1));
		assertThat(insumo("Cerveza 1 L").cantidad()).isEqualByComparingTo("23");

		pedidoService.cambiarCantidad(pedido.id(), pedido.items().getFirst().id(), new CantidadForm.CambiarCantidad(0));
		assertThat(insumo("Cerveza 1 L").cantidad()).isEqualByComparingTo("24");
	}

	@Test
	void sinStockNoDejaCargar() {
		Long cerveza = insumo("Cerveza 1 L").id();
		stockService.registrarConteo(cerveza, BigDecimal.ONE);
		PedidoDetalle pedido = abrirMesa();

		assertThatThrownBy(() -> pedidoService.agregarItem(pedido.id(), new CantidadForm.AgregarItem(producto("Cerveza").id(), 2)))
				.isInstanceOf(ReglaNegocioException.class)
				.hasMessageContaining("Se acabó Cerveza");
		assertThat(insumo("Cerveza 1 L").cantidad()).as("no se descontó nada").isEqualByComparingTo("1");
		assertThat(producto("Cerveza").disponible()).isEqualTo(1);
	}

	@Test
	void laCartaDiceCuantosQuedanYAvisaLoBajo() {
		stockService.registrarConteo(insumo("Papas").id(), new BigDecimal("1"));
		assertThat(producto("Milanesa").disponible()).as("1 kg de papas alcanza para 4").isEqualTo(4);
		assertThat(producto("Tiramisú").disponible()).as("sin receta no controla").isNull();
		assertThat(stockService.alertas().bajos()).isEqualTo(1);
	}

	@Test
	void controlarPorUnidadYArmarUnaReceta() {
		Long flan = producto("Flan").id();
		InsumoVista insumo = stockService.controlarPorUnidad(flan);
		stockService.cargarCompra(insumo.id(), new BigDecimal("10"));
		assertThat(producto("Flan").disponible()).isEqualTo(10);

		InsumoVista dulce = stockService.crearInsumo(new StockDtos.InsumoForm("Dulce de leche", Unidad.KILO, new BigDecimal("2"), BigDecimal.ZERO));
		stockService.guardarReceta(flan, new RecetaForm(List.of(new LineaForm(insumo.id(), BigDecimal.ONE),
				new LineaForm(dulce.id(), new BigDecimal("0.05")))));
		assertThat(producto("Flan").disponible()).isEqualTo(10);
		assertThat(insumo("Dulce de leche").usadoEn()).containsExactly("Flan con dulce de leche");
	}

	private PedidoDetalle abrirMesa() {
		return pedidoService.abrirMesa(mesaService.listar().getFirst().id());
	}

	private ProductoVista producto(String empiezaCon) {
		return cartaService.carta(true).stream().map(CategoriaVista::productos).flatMap(List::stream)
				.filter(p -> p.nombre().startsWith(empiezaCon)).findFirst().orElseThrow();
	}

	private InsumoVista insumo(String nombre) {
		return stockService.stock().insumos().stream().filter(i -> i.nombre().equals(nombre)).findFirst().orElseThrow();
	}
}
