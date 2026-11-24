package ar.viaveneto.salon;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.viaveneto.salon.SalonDtos.PlanoForm;
import ar.viaveneto.salon.SalonDtos.SalonVista;
import ar.viaveneto.salon.SalonDtos.SectorForm;
import ar.viaveneto.salon.SalonDtos.SectorVista;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/salon")
@RequiredArgsConstructor
public class SalonController {

	private final SalonService salonService;

	@GetMapping
	public SalonVista salon() {
		return salonService.salon();
	}

	@PutMapping
	public SalonVista guardarPlano(@Valid @RequestBody PlanoForm form) {
		return salonService.guardarPlano(form);
	}

	@PostMapping("/sectores")
	@ResponseStatus(HttpStatus.CREATED)
	public SectorVista crearSector(@Valid @RequestBody SectorForm form) {
		return salonService.crearSector(form);
	}
}
