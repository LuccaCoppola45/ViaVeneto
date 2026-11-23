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
import lombok.RequiredArgsConstructor;

/** Con la base vacía carga 15 mesas y una carta de ejemplo para poder probar. Se edita desde la pantalla Carta. */
@Component
@ConditionalOnProperty(name = "viaveneto.datos-iniciales", havingValue = "true")
@RequiredArgsConstructor
public class DatosIniciales implements ApplicationRunner {

	private static final int MESAS_DE_EJEMPLO = 15;

	private final MesaRepository mesas;
	private final CategoriaRepository categorias;
	private final ProductoRepository productos;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (mesas.count() == 0) {
			for (int numero = 1; numero <= MESAS_DE_EJEMPLO; numero++) {
				mesas.save(new Mesa(numero));
			}
		}
		if (categorias.count() == 0) {
			cargarCarta();
		}
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
