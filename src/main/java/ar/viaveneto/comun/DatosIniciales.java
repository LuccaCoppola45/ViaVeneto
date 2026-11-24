package ar.viaveneto.comun;

import java.math.BigDecimal;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.carta.Categoria;
import ar.viaveneto.carta.CategoriaRepository;
import ar.viaveneto.carta.Producto;
import ar.viaveneto.carta.ProductoRepository;
import ar.viaveneto.mesa.Mesa;
import ar.viaveneto.mesa.MesaRepository;
import ar.viaveneto.salon.ElementoPlano;
import ar.viaveneto.salon.ElementoPlanoRepository;
import ar.viaveneto.salon.Sector;
import ar.viaveneto.salon.SectorRepository;
import ar.viaveneto.salon.TipoElemento;
import lombok.RequiredArgsConstructor;

/**
 * Con la base vacía arma el local como es hoy (planta baja 5, arriba 4, cerramiento 6, afuera 4)
 * y una carta de ejemplo. La ubicación de las mesas se ajusta desde "Acomodar salón".
 */
@Component
@ConditionalOnProperty(name = "viaveneto.datos-iniciales", havingValue = "true")
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

	private final MesaRepository mesas;
	private final SectorRepository sectores;
	private final ElementoPlanoRepository elementos;
	private final CategoriaRepository categorias;
	private final ProductoRepository productos;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (sectores.count() == 0 && mesas.count() == 0) {
			armarLocal();
		}
		if (categorias.count() == 0) {
			cargarCarta();
		}
	}

	private void armarLocal() {
		Sector abajo = sectores.save(new Sector("Planta baja", 1));
		mesa(1, abajo, 270, 60);
		mesa(2, abajo, 520, 60);
		mesa(3, abajo, 270, 250);
		mesa(4, abajo, 520, 250);
		mesa(5, abajo, 270, 440);
		elemento(abajo, TipoElemento.COCINA, 0, 0, 200, 150);
		elemento(abajo, TipoElemento.BARRA, 800, 40, 130, 330);
		elemento(abajo, TipoElemento.CAJA, 800, 390, 130, 80);
		elemento(abajo, TipoElemento.ESCALERA, 0, 460, 160, 165);
		elemento(abajo, TipoElemento.PUERTA, 560, 595, 160, 30);

		Sector arriba = sectores.save(new Sector("Arriba", 2));
		mesa(6, arriba, 230, 80);
		mesa(7, arriba, 520, 80);
		mesa(8, arriba, 230, 330);
		mesa(9, arriba, 520, 330);
		elemento(arriba, TipoElemento.ESCALERA, 0, 460, 160, 165);
		elemento(arriba, TipoElemento.BANIO, 840, 0, 160, 130);

		Sector cerramiento = sectores.save(new Sector("Cerramiento", 3));
		mesa(10, cerramiento, 130, 60);
		mesa(11, cerramiento, 420, 60);
		mesa(12, cerramiento, 710, 60);
		mesa(13, cerramiento, 130, 400);
		mesa(14, cerramiento, 420, 400);
		mesa(15, cerramiento, 710, 400);
		elemento(cerramiento, TipoElemento.PUERTA, 0, 240, 30, 150);
		elemento(cerramiento, TipoElemento.VENTANA, 970, 120, 30, 380);

		Sector afuera = sectores.save(new Sector("Afuera", 4));
		mesa(16, afuera, 80, 250);
		mesa(17, afuera, 310, 250);
		mesa(18, afuera, 540, 250);
		mesa(19, afuera, 770, 250);
		elemento(afuera, TipoElemento.PARED, 0, 0, 1000, 30);
		elemento(afuera, TipoElemento.PLANTA, 0, 520, 90, 90);
		elemento(afuera, TipoElemento.PLANTA, 910, 520, 90, 90);
	}

	private void mesa(int numero, Sector sector, int x, int y) {
		mesas.save(new Mesa(numero, sector, x, y));
	}

	private void elemento(Sector sector, TipoElemento tipo, int x, int y, int ancho, int alto) {
		elementos.save(new ElementoPlano(sector, tipo, x, y, ancho, alto));
	}

	private void cargarCarta() {
		Categoria entradas = categorias.save(new Categoria("Entradas", 1));
		Categoria principales = categorias.save(new Categoria("Principales", 2));
		Categoria pastas = categorias.save(new Categoria("Pastas", 3));
		Categoria postres = categorias.save(new Categoria("Postres", 4));
		Categoria bebidas = categorias.save(new Categoria("Bebidas", 5));

		producto("Empanada de carne", 2500, entradas);
		producto("Provoleta", 9000, entradas);
		producto("Milanesa napolitana con papas", 18000, principales);
		producto("Bife de chorizo", 24000, principales);
		producto("Sorrentinos con salsa", 15000, pastas);
		producto("Ñoquis con tuco", 13000, pastas);
		producto("Flan con dulce de leche", 6000, postres);
		producto("Tiramisú", 7500, postres);
		producto("Agua sin gas 500 ml", 3000, bebidas);
		producto("Gaseosa 500 ml", 3500, bebidas);
		producto("Cerveza 1 L", 8000, bebidas);
		producto("Vino de la casa (copa)", 5000, bebidas);
	}

	private void producto(String nombre, int precio, Categoria categoria) {
		productos.save(new Producto(nombre, BigDecimal.valueOf(precio), categoria));
	}
}
