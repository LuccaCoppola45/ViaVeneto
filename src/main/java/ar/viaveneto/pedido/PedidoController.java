package ar.viaveneto.pedido;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

	private final PedidoService pedidoService;

	@GetMapping("/{id}")
	public PedidoDetalle ver(@PathVariable Long id) {
		return pedidoService.ver(id);
	}

	@PostMapping("/{id}/items")
	public PedidoDetalle agregarItem(@PathVariable Long id, @Valid @RequestBody CantidadForm.AgregarItem form) {
		return pedidoService.agregarItem(id, form);
	}

	@PutMapping("/{id}/items/{itemId}")
	public PedidoDetalle cambiarCantidad(@PathVariable Long id, @PathVariable Long itemId,
			@Valid @RequestBody CantidadForm.CambiarCantidad form) {
		return pedidoService.cambiarCantidad(id, itemId, form);
	}

	@PostMapping("/{id}/cerrar")
	public PedidoDetalle cerrar(@PathVariable Long id) {
		return pedidoService.cerrar(id);
	}
}
