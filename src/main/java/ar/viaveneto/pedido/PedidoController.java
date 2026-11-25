package ar.viaveneto.pedido;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import ar.viaveneto.acceso.UsuarioActual;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

	private final PedidoService pedidoService;

	@GetMapping("/sin-mesa")
	public List<PedidoSinMesa> sinMesa() {
		return pedidoService.sinMesaEnCurso();
	}

	@PostMapping("/sin-mesa")
	public PedidoDetalle abrirSinMesa(@Valid @RequestBody CantidadForm.NuevoSinMesa form,
			@RequestAttribute(UsuarioActual.ATRIBUTO) UsuarioActual usuario) {
		return pedidoService.abrirSinMesa(form, usuario.id());
	}

	@PutMapping("/{id}/entrega")
	public PedidoDetalle marcarEntrega(@PathVariable Long id, @Valid @RequestBody CantidadForm.Entrega form) {
		return pedidoService.marcarEntrega(id, form);
	}

	@PutMapping("/{id}/atiende")
	public PedidoDetalle pasarA(@PathVariable Long id, @Valid @RequestBody CantidadForm.Atiende form) {
		return pedidoService.pasarA(id, form);
	}

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

	@PutMapping("/{id}/items/{itemId}/nota")
	public PedidoDetalle ponerNota(@PathVariable Long id, @PathVariable Long itemId,
			@Valid @RequestBody CantidadForm.Nota form) {
		return pedidoService.ponerNota(id, itemId, form);
	}

	@PutMapping("/{id}/descuento")
	public PedidoDetalle aplicarDescuento(@PathVariable Long id, @Valid @RequestBody CantidadForm.Descuento form) {
		return pedidoService.aplicarDescuento(id, form);
	}

	@PostMapping("/{id}/pagos")
	public PedidoDetalle cobrar(@PathVariable Long id, @Valid @RequestBody CantidadForm.Cobro form,
			@RequestAttribute(UsuarioActual.ATRIBUTO) UsuarioActual usuario) {
		return pedidoService.cobrar(id, form, usuario.nombre());
	}

	@DeleteMapping("/{id}/pagos/{pagoId}")
	public PedidoDetalle anularPago(@PathVariable Long id, @PathVariable Long pagoId) {
		return pedidoService.anularPago(id, pagoId);
	}

	@PostMapping("/{id}/cerrar")
	public PedidoDetalle cerrar(@PathVariable Long id) {
		return pedidoService.cerrar(id);
	}
}
