package ar.viaveneto.acceso;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.viaveneto.acceso.AccesoDtos.Estado;
import ar.viaveneto.acceso.AccesoDtos.Ingreso;
import ar.viaveneto.acceso.AccesoDtos.NuevoPin;
import ar.viaveneto.acceso.AccesoDtos.NuevoUsuario;
import ar.viaveneto.acceso.AccesoDtos.SesionVista;
import ar.viaveneto.acceso.AccesoDtos.UsuarioVista;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AccesoController {

	private final AccesoService accesoService;

	@GetMapping("/api/auth/estado")
	public Estado estado() {
		return accesoService.estado();
	}

	@PostMapping("/api/auth/primer-usuario")
	public SesionVista primerUsuario(@Valid @RequestBody NuevoUsuario nuevo) {
		return accesoService.crearPrimerUsuario(nuevo);
	}

	@PostMapping("/api/auth/ingresar")
	public SesionVista ingresar(@Valid @RequestBody Ingreso ingreso) {
		return accesoService.ingresar(ingreso);
	}

	@GetMapping("/api/auth/yo")
	public UsuarioActual yo(@RequestAttribute(UsuarioActual.ATRIBUTO) UsuarioActual usuario) {
		return usuario;
	}

	@PostMapping("/api/auth/salir")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void salir(@RequestHeader("Authorization") String encabezado) {
		accesoService.salir(encabezado.substring("Bearer ".length()).trim());
	}

	@GetMapping("/api/usuarios")
	public List<UsuarioVista> usuarios() {
		return accesoService.listar();
	}

	@PostMapping("/api/usuarios")
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioVista crear(@Valid @RequestBody NuevoUsuario nuevo) {
		return accesoService.crear(nuevo);
	}

	@PutMapping("/api/usuarios/{id}/pin")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cambiarPin(@PathVariable Long id, @Valid @RequestBody NuevoPin nuevo) {
		accesoService.cambiarPin(id, nuevo.pin());
	}

	@DeleteMapping("/api/usuarios/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void quitar(@PathVariable Long id, @RequestAttribute(UsuarioActual.ATRIBUTO) UsuarioActual usuario) {
		accesoService.quitar(id, usuario);
	}
}
