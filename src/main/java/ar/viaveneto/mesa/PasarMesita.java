package ar.viaveneto.mesa;

import jakarta.validation.constraints.NotNull;

public record PasarMesita(@NotNull(message = "Elegí a qué mesa va la mesita.") Long destinoId) {
}
