package ar.viaveneto.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ar.viaveneto.carta.Producto;
import ar.viaveneto.carta.ProductoRepository;
import ar.viaveneto.comun.NoEncontradoException;
import ar.viaveneto.comun.ReglaNegocioException;
import ar.viaveneto.stock.StockDtos.Alertas;
import ar.viaveneto.stock.StockDtos.InsumoForm;
import ar.viaveneto.stock.StockDtos.InsumoVista;
import ar.viaveneto.stock.StockDtos.LineaReceta;
import ar.viaveneto.stock.StockDtos.RecetaForm;
import ar.viaveneto.stock.StockDtos.RecetaVista;
import ar.viaveneto.stock.StockDtos.StockVista;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StockService {

	private final InsumoRepository insumos;
	private final RecetaItemRepository recetas;
	private final ProductoRepository productos;

	@Transactional(readOnly = true)
	public StockVista stock() {
		List<RecetaItem> todas = recetas.findTodas();
		Map<Long, List<String>> usadoEn = todas.stream()
				.collect(Collectors.groupingBy(r -> r.getInsumo().getId(),
						Collectors.mapping(r -> r.getProducto().getNombre(), Collectors.toList())));
		List<InsumoVista> vistas = insumos.findByActivoTrueOrderByNombre().stream()
				.map(i -> new InsumoVista(i.getId(), i.getNombre(), i.getUnidad(), i.getCantidad(), i.getMinimo(), i.estado(),
						usadoEn.getOrDefault(i.getId(), List.of())))
				.toList();

		Map<Long, List<LineaReceta>> lineasPorProducto = todas.stream()
				.collect(Collectors.groupingBy(r -> r.getProducto().getId(), Collectors.mapping(
						r -> new LineaReceta(r.getInsumo().getId(), r.getInsumo().getNombre(), r.getInsumo().getUnidad(), r.getCantidad()),
						Collectors.toList())));
		List<RecetaVista> deProductos = productos.findAllConCategoria().stream()
				.map(p -> new RecetaVista(p.getId(), p.getNombre(), p.getCategoria().getNombre(), p.isActivo(),
						lineasPorProducto.getOrDefault(p.getId(), List.of())))
				.toList();
		return new StockVista(vistas, deProductos, alertas(vistas));
	}

	@Transactional(readOnly = true)
	public Alertas alertas() {
		List<EstadoStock> estados = insumos.findByActivoTrueOrderByNombre().stream().map(Insumo::estado).toList();
		return new Alertas((int) estados.stream().filter(e -> e == EstadoStock.BAJO).count(),
				(int) estados.stream().filter(e -> e == EstadoStock.SIN_STOCK).count());
	}

	/**
	 * Cuántas unidades de cada plato se pueden hacer con lo que hay.
	 * Los platos sin receta no controlan stock y no aparecen.
	 */
	@Transactional(readOnly = true)
	public Map<Long, Integer> disponibles() {
		return recetas.findTodas().stream()
				.collect(Collectors.groupingBy(r -> r.getProducto().getId(),
						Collectors.collectingAndThen(Collectors.toList(), StockService::unidadesPosibles)));
	}

	/**
	 * Descuenta lo que gasta un plato (o lo devuelve si {@code unidades} es negativo).
	 * Si no alcanza, no deja cargarlo: eso eligió el negocio.
	 */
	@Transactional
	public void consumir(Producto producto, int unidades) {
		if (unidades == 0) {
			return;
		}
		List<RecetaItem> receta = recetas.findDeProducto(producto.getId());
		if (receta.isEmpty()) {
			return;
		}
		Map<Long, Insumo> bloqueados = insumos.findParaActualizar(receta.stream().map(r -> r.getInsumo().getId()).toList())
				.stream().collect(Collectors.toMap(Insumo::getId, Function.identity()));
		if (unidades > 0) {
			receta.stream()
					.filter(r -> !bloqueados.get(r.getInsumo().getId()).alcanza(r.para(unidades)))
					.findFirst()
					.ifPresent(r -> {
						String insumo = r.getInsumo().getNombre();
						String falta = insumo.equalsIgnoreCase(producto.getNombre())
								? "Se acabó " + insumo + "."
								: "No hay más " + insumo + " para " + producto.getNombre() + ".";
						throw new ReglaNegocioException(falta + " Si llegó mercadería, cargala en Stock.");
					});
		}
		receta.forEach(r -> bloqueados.get(r.getInsumo().getId()).sumar(r.para(unidades).negate()));
	}

	@Transactional
	public InsumoVista crearInsumo(InsumoForm form) {
		String nombre = form.nombre().trim();
		BigDecimal cantidad = Optional.ofNullable(form.cantidad()).orElse(BigDecimal.ZERO);
		Insumo insumo = insumos.findByNombreIgnoreCase(nombre)
				.map(existente -> {
					if (existente.isActivo()) {
						throw new ReglaNegocioException("Ya existe " + nombre + " en el stock.");
					}
					existente.reactivar(form.unidad(), cantidad, form.minimo());
					return existente;
				})
				.orElseGet(() -> insumos.save(new Insumo(nombre, form.unidad(), cantidad, form.minimo())));
		return vista(insumo);
	}

	@Transactional
	public InsumoVista editarInsumo(Long id, InsumoForm form) {
		Insumo insumo = buscar(id);
		insumos.findByNombreIgnoreCase(form.nombre().trim())
				.filter(otro -> !otro.getId().equals(id))
				.ifPresent(otro -> {
					throw new ReglaNegocioException("Ya existe " + otro.getNombre() + " en el stock.");
				});
		insumo.editar(form.nombre(), form.unidad(), form.minimo());
		return vista(insumo);
	}

	/** Llegó mercadería: se suma a lo que había. */
	@Transactional
	public InsumoVista cargarCompra(Long id, BigDecimal cantidad) {
		Insumo insumo = buscar(id);
		insumo.sumar(cantidad);
		return vista(insumo);
	}

	/** Se contó lo que hay de verdad: reemplaza la cantidad. */
	@Transactional
	public InsumoVista registrarConteo(Long id, BigDecimal cantidad) {
		Insumo insumo = buscar(id);
		insumo.contar(cantidad);
		return vista(insumo);
	}

	/** Deja de controlarse: los platos que lo usaban ya no lo descuentan. */
	@Transactional
	public void quitarInsumo(Long id) {
		Insumo insumo = buscar(id);
		recetas.borrarDeInsumo(id);
		insumo.desactivar();
	}

	@Transactional
	public void guardarReceta(Long productoId, RecetaForm form) {
		Producto producto = productos.findById(productoId)
				.orElseThrow(() -> new NoEncontradoException("No existe ese producto."));
		Set<Long> vistos = new HashSet<>();
		form.lineas().forEach(l -> {
			if (!vistos.add(l.insumoId())) {
				throw new ReglaNegocioException("Hay un insumo repetido en la receta.");
			}
		});
		recetas.borrarDeProducto(productoId);
		recetas.flush();
		form.lineas().forEach(l -> recetas.save(new RecetaItem(producto, buscar(l.insumoId()), l.cantidad())));
	}

	/**
	 * Para lo que se vende tal cual se compra (latas, botellas, empanadas):
	 * crea un insumo con el mismo nombre y cada venta descuenta 1.
	 */
	@Transactional
	public InsumoVista controlarPorUnidad(Long productoId) {
		Producto producto = productos.findById(productoId)
				.orElseThrow(() -> new NoEncontradoException("No existe ese producto."));
		Insumo insumo = insumos.findByNombreIgnoreCase(producto.getNombre())
				.map(existente -> {
					if (!existente.isActivo()) {
						existente.reactivar(Unidad.UNIDAD, BigDecimal.ZERO, BigDecimal.ZERO);
					}
					return existente;
				})
				.orElseGet(() -> insumos.save(new Insumo(producto.getNombre(), Unidad.UNIDAD, BigDecimal.ZERO, BigDecimal.ZERO)));
		recetas.borrarDeProducto(productoId);
		recetas.flush();
		recetas.save(new RecetaItem(producto, insumo, BigDecimal.ONE));
		return vista(insumo);
	}

	private static int unidadesPosibles(List<RecetaItem> receta) {
		return receta.stream()
				.map(r -> r.getInsumo().getCantidad().max(BigDecimal.ZERO).divide(r.getCantidad(), 0, RoundingMode.FLOOR).intValue())
				.min(Comparator.naturalOrder())
				.orElse(0);
	}

	private static Alertas alertas(List<InsumoVista> vistas) {
		int bajos = (int) vistas.stream().filter(v -> v.estado() == EstadoStock.BAJO).count();
		int sin = (int) vistas.stream().filter(v -> v.estado() == EstadoStock.SIN_STOCK).count();
		return new Alertas(bajos, sin);
	}

	private InsumoVista vista(Insumo i) {
		List<String> usadoEn = recetas.findTodas().stream()
				.filter(r -> r.getInsumo().getId().equals(i.getId()))
				.map(r -> r.getProducto().getNombre())
				.toList();
		return new InsumoVista(i.getId(), i.getNombre(), i.getUnidad(), i.getCantidad(), i.getMinimo(), i.estado(), usadoEn);
	}

	private Insumo buscar(Long id) {
		return insumos.findById(id)
				.filter(Insumo::isActivo)
				.orElseThrow(() -> new NoEncontradoException("Ese insumo ya no está en el stock."));
	}
}
