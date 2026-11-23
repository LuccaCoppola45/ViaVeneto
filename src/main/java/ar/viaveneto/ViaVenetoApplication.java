package ar.viaveneto;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ViaVenetoApplication {

	public static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

	public static void main(String[] args) {
		SpringApplication.run(ViaVenetoApplication.class, args);
	}

	@Bean
	Clock reloj() {
		return Clock.system(ZONA_ARGENTINA);
	}
}
