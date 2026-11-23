import { DestroyRef, inject } from '@angular/core';

export const REFRESCO_MS = 5000;

/**
 * Repite una carga cada pocos segundos mientras la pantalla está abierta,
 * así lo que carga un mozo aparece en las otras tablets sin tocar nada.
 */
export const refrescarCadaRato = (cargar: () => void) => {
  const intervalo = setInterval(cargar, REFRESCO_MS);
  inject(DestroyRef).onDestroy(() => clearInterval(intervalo));
};
