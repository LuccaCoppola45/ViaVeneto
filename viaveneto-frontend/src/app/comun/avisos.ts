import { Injectable, signal } from '@angular/core';

type Aviso = { texto: string; tipo: 'error' | 'ok' };

const DURACION_MS = 4000;

/** Mensaje corto que aparece abajo de la pantalla y se va solo. */
@Injectable({ providedIn: 'root' })
export class Avisos {
  readonly actual = signal<Aviso | null>(null);
  private temporizador?: ReturnType<typeof setTimeout>;

  error(texto: string) {
    this.mostrar({ texto, tipo: 'error' });
  }

  ok(texto: string) {
    this.mostrar({ texto, tipo: 'ok' });
  }

  cerrar() {
    this.actual.set(null);
  }

  private mostrar(aviso: Aviso) {
    clearTimeout(this.temporizador);
    this.actual.set(aviso);
    this.temporizador = setTimeout(() => this.cerrar(), DURACION_MS);
  }
}
