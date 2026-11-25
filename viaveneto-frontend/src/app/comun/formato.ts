import { Pipe, PipeTransform } from '@angular/core';

const pesos = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS', maximumFractionDigits: 0 });

/** 18000 → "$ 18.000" */
@Pipe({ name: 'plata' })
export class PlataPipe implements PipeTransform {
  transform(valor: number | null | undefined): string {
    return pesos.format(valor ?? 0);
  }
}

/** Cuánto hace que se abrió la mesa: "recién", "25 min", "1 h 10 min". */
export const tiempoDesde = (fechaIso: string, ahora: number): string => {
  const minutos = Math.max(0, Math.floor((ahora - new Date(fechaIso).getTime()) / 60000));
  if (minutos < 1) return 'recién';
  if (minutos < 60) return `${minutos} min`;
  const horas = Math.floor(minutos / 60);
  const resto = minutos % 60;
  return resto ? `${horas} h ${resto} min` : `${horas} h`;
};

const numero = new Intl.NumberFormat('es-AR', { maximumFractionDigits: 3 });
const SUFIJO_UNIDAD = { UNIDAD: 'u.', KILO: 'kg', LITRO: 'L' } as const;

/** 9.5 kg → "9,5 kg"; 24 unidades → "24 u." */
@Pipe({ name: 'cantidad' })
export class CantidadPipe implements PipeTransform {
  transform(valor: number | null | undefined, unidad: keyof typeof SUFIJO_UNIDAD): string {
    return `${numero.format(valor ?? 0)} ${SUFIJO_UNIDAD[unidad]}`;
  }
}
