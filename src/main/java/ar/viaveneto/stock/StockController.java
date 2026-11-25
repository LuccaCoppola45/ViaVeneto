package ar.viaveneto.stock;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.viaveneto.stock.StockDtos.Alertas;
import ar.viaveneto.stock.StockDtos.CantidadForm;
import ar.viaveneto.stock.StockDtos.InsumoForm;
import ar.viaveneto.stock.StockDtos.InsumoVista;
import ar.viaveneto.stock.StockDtos.RecetaForm;
import ar.viaveneto.stock.StockDtos.StockVista;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

	private final StockService stockService;

	@GetMapping
	public StockVista stock() {
		return stockService.stock();
	}

	@GetMapping("/alertas")
	public Alertas alertas() {
		return stockService.alertas();
	}

	@PostMapping("/insumos")
	@ResponseStatus(HttpStatus.CREATED)
	public InsumoVista crear(@Valid @RequestBody InsumoForm form) {
		return stockService.crearInsumo(form);
	}

	@PutMapping("/insumos/{id}")
	public InsumoVista editar(@PathVariable Long id, @Valid @RequestBody InsumoForm form) {
		return stockService.editarInsumo(id, form);
	}

	@PostMapping("/insumos/{id}/compra")
	public InsumoVista compra(@PathVariable Long id, @Valid @RequestBody CantidadForm form) {
		return stockService.cargarCompra(id, form.cantidad());
	}

	@PostMapping("/insumos/{id}/conteo")
	public InsumoVista conteo(@PathVariable Long id, @Valid @RequestBody CantidadForm form) {
		return stockService.registrarConteo(id, form.cantidad());
	}

	@DeleteMapping("/insumos/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitar(@PathVariable Long id) {
		stockService.quitarInsumo(id);
	}

	@PutMapping("/recetas/{productoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void guardarReceta(@PathVariable Long productoId, @Valid @RequestBody RecetaForm form) {
		stockService.guardarReceta(productoId, form);
	}

	@PostMapping("/recetas/{productoId}/por-unidad")
	public InsumoVista porUnidad(@PathVariable Long productoId) {
		return stockService.controlarPorUnidad(productoId);
	}
}
