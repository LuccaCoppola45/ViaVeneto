package ar.viaveneto.comun;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ar.viaveneto.acceso.NoAutorizadoException;

/** Devuelve los errores como { "detail": "mensaje para mostrar en pantalla" }. */
@RestControllerAdvice
public class ManejadorErrores {

	@ExceptionHandler(NoEncontradoException.class)
	ProblemDetail noEncontrado(NoEncontradoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ReglaNegocioException.class)
	ProblemDetail reglaNegocio(ReglaNegocioException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(NoAutorizadoException.class)
	ProblemDetail noAutorizado(NoAutorizadoException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ProblemDetail datosInvalidos(MethodArgumentNotValidException e) {
		String mensaje = e.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Revisá los datos cargados.");
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, mensaje);
	}
}
