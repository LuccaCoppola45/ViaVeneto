import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Avisos } from './comun/avisos';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="barra">
      <a class="marca" routerLink="/mesas">Via Veneto</a>
      <nav>
        <a routerLink="/mesas" routerLinkActive="activo">Mesas</a>
        <a routerLink="/carta" routerLinkActive="activo">Carta y mesas</a>
      </nav>
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
}
