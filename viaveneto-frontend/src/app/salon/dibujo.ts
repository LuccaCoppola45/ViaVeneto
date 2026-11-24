import { Component, input } from '@angular/core';
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

export const tamanioMesa = (medidas: Medidas, vertical: boolean) =>
  vertical
    ? { ancho: medidas.mesaCorto, alto: medidas.mesaLargo }
    : { ancho: medidas.mesaLargo, alto: medidas.mesaCorto };

/** Mesa de 4 lugares vista desde arriba: el tablero con dos sillas de cada lado largo. */
@Component({
  selector: 'app-mesa-dibujo',
  host: { class: 'mesa-dibujo', '[class.vertical]': 'vertical()' },
  template: `
    <span class="silla s1"></span><span class="silla s2"></span>
    <span class="silla s3"></span><span class="silla s4"></span>
    <span class="tablero"><ng-content /></span>
  `,
})
export class MesaDibujo {
  readonly vertical = input(false);
}
