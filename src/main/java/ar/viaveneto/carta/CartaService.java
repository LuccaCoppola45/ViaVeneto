package ar.viaveneto.carta;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.carta.CartaDtos.CategoriaForm;
import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaDtos.ProductoForm;
import ar.viaveneto.carta.CartaDtos.ProductoVista;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.stock.StockService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartaService {

	private final CategoriaRepository categorias;
	private final ProductoRepository productos;
	private final StockService stockService;

	/**
	 * La carta completa agrupada por categoría.
	 * @param soloActivos true para la pantalla de pedidos, false para editar la carta
	 */
	@Transactional(readOnly = true)
	public List<CategoriaVista> carta(boolean soloActivos) {
		Map<Long, Integer> disponibles = stockService.disponibles();
		Map<Long, List<ProductoVista>> productosPorCategoria = productos.findAllConCategoria().stream()
				.filter(p -> !soloActivos || p.isActivo())
				.map(p -> ProductoVista.de(p, disponibles.get(p.getId())))
				.collect(Collectors.groupingBy(ProductoVista::categoriaId));

		return categorias.findAllByOrderByOrdenAscNombreAsc().stream()
				.map(c -> new CategoriaVista(c.getId(), c.getNombre(), c.getOrden(),
						productosPorCategoria.getOrDefault(c.getId(), List.of())))
				.filter(c -> !soloActivos || !c.productos().isEmpty())
				.toList();
	}

	@Transactional
	public CategoriaVista crearCategoria(CategoriaForm form) {
		String nombre = form.nombre().trim();
		boolean repetida = categorias.findAll().stream().anyMatch(c -> c.getNombre().equalsIgnoreCase(nombre));
		if (repetida) {
			throw new ReglaNegocioException("Ya existe la categoría " + nombre + ".");
		}
		int orden = (int) categorias.count() + 1;
		Categoria categoria = categorias.save(new Categoria(nombre, orden));
		return new CategoriaVista(categoria.getId(), categoria.getNombre(), categoria.getOrden(), List.of());
	}

	@Transactional
	public ProductoVista crearProducto(ProductoForm form) {
		Producto producto = productos.save(new Producto(form.nombre(), form.precio(), buscarCategoria(form.categoriaId())));
		return ProductoVista.de(producto);
	}

	@Transactional
	public ProductoVista editarProducto(Long id, ProductoForm form) {
		Producto producto = buscarProducto(id);
		producto.actualizar(form.nombre(), form.precio(), buscarCategoria(form.categoriaId()), form.activo());
		return ProductoVista.de(producto);
	}

	public Producto buscarProducto(Long id) {
		return productos.findById(id).orElseThrow(() -> new NoEncontradoException("No existe ese producto."));
	}

	private Categoria buscarCategoria(Long id) {
		return categorias.findById(id).orElseThrow(() -> new NoEncontradoException("No existe esa categoría."));
	}
}
