package ar.viaveneto.carta;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.viaveneto.carta.CartaDtos.CategoriaForm;
import ar.viaveneto.carta.CartaDtos.CategoriaVista;
import ar.viaveneto.carta.CartaDtos.ProductoForm;
import ar.viaveneto.carta.CartaDtos.ProductoVista;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carta")
@RequiredArgsConstructor
public class CartaController {

	private final CartaService cartaService;

	@GetMapping
	public List<CategoriaVista> carta(@RequestParam(defaultValue = "true") boolean soloActivos) {
		return cartaService.carta(soloActivos);
	}

	@PostMapping("/categorias")
	@ResponseStatus(HttpStatus.CREATED)
	public CategoriaVista crearCategoria(@Valid @RequestBody CategoriaForm form) {
		return cartaService.crearCategoria(form);
	}

	@PostMapping("/productos")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductoVista crearProducto(@Valid @RequestBody ProductoForm form) {
		return cartaService.crearProducto(form);
	}

	@PutMapping("/productos/{id}")
	public ProductoVista editarProducto(@PathVariable Long id, @Valid @RequestBody ProductoForm form) {
		return cartaService.editarProducto(id, form);
	}
}
