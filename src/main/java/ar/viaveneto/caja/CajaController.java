package ar.viaveneto.caja;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.viaveneto.caja.CajaDtos.AbrirTurno;
import ar.viaveneto.caja.CajaDtos.CajaVista;
import ar.viaveneto.caja.CajaDtos.CerrarTurno;
import ar.viaveneto.caja.CajaDtos.TurnoVista;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/caja")
@RequiredArgsConstructor
public class CajaController {

	private final CajaService cajaService;

	@GetMapping
	public CajaVista caja() {
		return cajaService.caja();
	}

	@PostMapping("/turnos")
	@ResponseStatus(HttpStatus.CREATED)
	public TurnoVista abrir(@Valid @RequestBody AbrirTurno form) {
		return cajaService.abrir(form);
	}

	@GetMapping("/turnos/{id}")
	public TurnoVista ver(@PathVariable Long id) {
		return cajaService.ver(id);
	}

	@PostMapping("/turnos/{id}/cerrar")
	public TurnoVista cerrar(@PathVariable Long id, @Valid @RequestBody CerrarTurno form) {
		return cajaService.cerrar(id, form);
	}
}
