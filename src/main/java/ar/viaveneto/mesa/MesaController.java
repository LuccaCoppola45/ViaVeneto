package ar.viaveneto.mesa;

import java.util.List;

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

import ar.viaveneto.pedido.PedidoDetalle;
import ar.viaveneto.pedido.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mesas")
@RequiredArgsConstructor
public class MesaController {

	private final MesaService mesaService;
	private final PedidoService pedidoService;

	@GetMapping
	public List<MesaResumen> listar() {
		return mesaService.listar();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public MesaResumen agregar(@Valid @RequestBody NuevaMesa nueva) {
		return mesaService.agregar(nueva);
	}

	@PutMapping("/{id}/numero")
	public MesaResumen cambiarNumero(@PathVariable Long id, @Valid @RequestBody NuevoNumero nuevo) {
		return mesaService.cambiarNumero(id, nuevo);
	}

	/** Devuelve todas las mesas: cambian dos a la vez. */
	@PostMapping("/{id}/pasar-mesita")
	public List<MesaResumen> pasarMesita(@PathVariable Long id, @Valid @RequestBody PasarMesita pase) {
		return mesaService.pasarMesita(id, pase);
	}

	@PostMapping("/{id}/partir")
	public List<MesaResumen> partir(@PathVariable Long id) {
		return mesaService.partir(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitar(@PathVariable Long id) {
		mesaService.quitar(id);
	}

	/** Abre la mesa. Si ya estaba abierta devuelve su pedido, así dos mozos que tocan a la vez ven lo mismo. */
	@PostMapping("/{id}/abrir")
	public PedidoDetalle abrir(@PathVariable Long id) {
		return pedidoService.abrirMesa(id);
	}
}
