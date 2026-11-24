import { Component, computed, input } from '@angular/core';
import { Medidas, TipoElemento } from '../comun/modelos';

type ComoSeVe = { nombre: string; icono: string; ancho: number; alto: number };

/** Nombre, dibujito y tamaño con el que aparece cada cosa nueva que se agrega al plano. */
export const ELEMENTOS: Record<TipoElemento, ComoSeVe> = {
  PUERTA: { nombre: 'Puerta', icono: '🚪', ancho: 150, alto: 30 },
  BARRA: { nombre: 'Barra', icono: '🍺', ancho: 300, alto: 90 },
  CAJA: { nombre: 'Caja', icono: '💵', ancho: 130, alto: 80 },
  COCINA: { nombre: 'Cocina', icono: '🍳', ancho: 220, alto: 150 },
  ESCALERA: { nombre: 'Escalera', icono: '🪜', ancho: 160, alto: 160 },
  BANIO: { nombre: 'Baño', icono: '🚻', ancho: 150, alto: 120 },
  VENTANA: { nombre: 'Ventana', icono: '🪟', ancho: 220, alto: 30 },
  PARED: { nombre: 'Pared', icono: '', ancho: 300, alto: 24 },
  PLANTA: { nombre: 'Planta', icono: '🌿', ancho: 80, alto: 80 },
};

export const TIPOS_ELEMENTO = Object.keys(ELEMENTOS) as TipoElemento[];

/** Posición y tamaño en porcentaje del plano, así se escala a cualquier pantalla. */
export const estiloPieza = (medidas: Medidas, x: number, y: number, ancho: number, alto: number) => ({
  left: `${(x / medidas.ancho) * 100}%`,
  top: `${(y / medidas.alto) * 100}%`,
  width: `${(ancho / medidas.ancho) * 100}%`,
  height: `${(alto / medidas.alto) * 100}%`,
});

/** Proporción del plano para el CSS: así el dibujo nunca se deforma si cambia el tamaño en el backend. */
export const estiloPlano = (medidas: Medidas) => ({
  '--plano-ratio': `${medidas.ancho} / ${medidas.alto}`,
  '--plano-proporcion': medidas.ancho / medidas.alto,
});

/** Las mesitas van en fila: cuantas más tiene la mesa, más larga es. */
export const tamanioMesa = (medidas: Medidas, vertical: boolean, mesitas: number) => {
  const largo = medidas.mesitaLargo * mesitas;
  return vertical ? { ancho: medidas.mesaCorto, alto: largo } : { ancho: largo, alto: medidas.mesaCorto };
};

/**
 * Mesa vista desde arriba: las mesitas individuales en fila (se ven las juntas)
 * con una silla de cada lado por mesita.
 */
@Component({
  selector: 'app-mesa-dibujo',
  host: { class: 'mesa-dibujo', '[class.vertical]': 'vertical()', '[style.--mesitas]': 'mesitas()' },
  template: `
    <span class="fila lado-a">@for (silla of sillas(); track silla) { <span class="silla"></span> }</span>
    <span class="tablero"><ng-content /></span>
    <span class="fila lado-b">@for (silla of sillas(); track silla) { <span class="silla"></span> }</span>
  `,
})
export class MesaDibujo {
  readonly vertical = input(false);
  readonly mesitas = input(2);
  protected readonly sillas = computed(() => Array.from({ length: this.mesitas() }, (_, i) => i));
}
