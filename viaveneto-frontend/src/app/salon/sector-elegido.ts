import { signal } from '@angular/core';

const CLAVE = 'viaveneto.sector';

const leer = (): number | null => {
  try {
    const valor = Number(localStorage.getItem(CLAVE));
    return Number.isFinite(valor) && valor > 0 ? valor : null;
  } catch {
    return null;
  }
};

/**
 * El sector que se está mirando. Cada tablet recuerda el suyo:
 * la que se usa arriba abre directo en "Arriba".
 */
export const sectorElegido = () => {
  const elegido = signal<number | null>(leer());
  const elegir = (id: number) => {
    elegido.set(id);
    try {
      localStorage.setItem(CLAVE, String(id));
    } catch {
      // Sin almacenamiento (modo privado): solo no se recuerda.
    }
  };
  return { elegido: elegido.asReadonly(), elegir };
};
