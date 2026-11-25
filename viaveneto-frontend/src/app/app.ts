import { Component, effect, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Api } from './comun/api';
import { Avisos } from './comun/avisos';
import { Sesion } from './comun/sesion';

const ALERTA_CADA_MS = 30_000;

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">
      <a class="marca" routerLink="/mesas">Via Veneto</a>
      @if (sesion.activa()) {
        <nav>
          <a routerLink="/mesas" routerLinkActive="activo">Mesas</a>
          <a routerLink="/caja" routerLinkActive="activo">Caja</a>
          <a routerLink="/stock" routerLinkActive="activo">
            Stock@if (faltantes()) { <span class="alerta-stock" [attr.aria-label]="faltantes() + ' cosas para comprar'">⚠️ {{ faltantes() }}</span> }
          </a>
          <a routerLink="/carta" routerLinkActive="activo">Carta y mesas</a>
          <button class="quien" (click)="salir()" [attr.aria-label]="'Salir (' + sesion.usuario()?.nombre + ')'">
            {{ sesion.usuario()?.nombre }} · Salir
          </button>
        </nav>
      }
    </header>
    <main>
      <router-outlet />
    </main>
    @if (avisos.actual(); as aviso) {
      <div class="aviso" [class.error]="aviso.tipo === 'error'" role="status" (click)="avisos.cerrar()">
        {{ aviso.texto }}
      </div>
    }
  `,
})
export class App {
  protected readonly avisos = inject(Avisos);
  protected readonly sesion = inject(Sesion);
  private readonly api = inject(Api);
  private readonly router = inject(Router);

  /** Cuántos insumos están bajos o se acabaron: se ve en el menú desde cualquier pantalla. */
  protected readonly faltantes = signal(0);

  constructor() {
    const intervalo = setInterval(() => this.revisarStock(), ALERTA_CADA_MS);
    effect(() => {
      if (this.sesion.activa()) this.revisarStock();
      else this.faltantes.set(0);
    });
    window.addEventListener('beforeunload', () => clearInterval(intervalo));
  }

  private async revisarStock() {
    if (!this.sesion.activa()) return;
    try {
      const alertas = await this.api.alertasStock();
      this.faltantes.set(alertas.bajos + alertas.sinStock);
    } catch {
      // Si falla, se intenta en el próximo repaso.
    }
  }

  /** Cambiar de persona: cierra la sesión de esta tablet y vuelve al teclado del PIN. */
  protected async salir() {
    try {
      await this.api.salir();
    } catch {
      // Si no hay conexión igual se cierra en esta tablet.
    }
    this.sesion.terminar();
    this.router.navigate(['/ingresar']);
  }
}
