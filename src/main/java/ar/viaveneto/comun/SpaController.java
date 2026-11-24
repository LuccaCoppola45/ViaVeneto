package ar.viaveneto.comun;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** En producción Angular se sirve desde /static: las rutas del front devuelven index.html. */
@Controller
public class SpaController {

	@GetMapping({ "/mesas", "/mesas/**", "/pedidos/**", "/carta", "/carta/**", "/salon" })
	public String index() {
		return "forward:/index.html";
	}
}
