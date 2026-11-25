import { Component, OnInit, inject, input, numberAttribute, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe } from '../comun/formato';
import { nombreMedio, nombreTurno } from '../comun/medios';
import { Turno } from '../comun/modelos';

/** Resumen impreso de un turno de caja: lo que deja anotado la abuela al mediodía o el cierre de la noche. */
@Component({
  selector: 'app-ticket-cierre',
  imports: [PlataPipe, DatePipe],
  styleUrl: './ticket.css',
  template: `
    @if (turno(); as t) {
      <div class="acciones no-imprimir">
        <button class="primario" (click)="imprimir()">🖨️ Imprimir</button>
        <button (click)="cerrarVentana()">Cerrar</button>
      </div>
      <div class="papel">
        <div class="centro marca">VIA VENETO</div>
        <div class="centro grande">CIERRE DE CAJA</div>
        <div class="centro">Turno {{ nombreTurno(t.tipo) }} · {{ t.responsable }}</div>
        <div class="centro">{{ t.abiertoEn | date: 'dd/MM HH:mm' }} a {{ (t.cerradoEn ?? ahora) | date: 'HH:mm' }}</div>
        <div class="corte"></div>
        @for (m of t.resumen.porMedio; track m.medio) {
          <div class="linea"><span>{{ nombreMedio(m.medio) }} ({{ m.cobros }})</span><span>{{ m.monto | plata }}</span></div>
        }
        <div class="corte"></div>
        <div class="linea grande"><span>VENTAS</span><span>{{ t.resumen.ventas | plata }}</span></div>
        <div class="linea"><span>Mesas cobradas</span><span>{{ t.resumen.mesasCobradas }}</span></div>
        @if (t.resumen.descuentos) { <div class="linea"><span>Descuentos</span><span>{{ t.resumen.descuentos | plata }}</span></div> }
        <div class="linea"><span>Propinas (aparte)</span><span>{{ t.resumen.propinas | plata }}</span></div>
        <div class="corte"></div>
        <div class="linea"><span>Cambio inicial</span><span>{{ t.fondoInicial | plata }}</span></div>
        <div class="linea"><span>+ Efectivo vendido</span><span>{{ (t.efectivoEsperado - t.fondoInicial) | plata }}</span></div>
        <div class="linea grande"><span>Debería haber</span><span>{{ t.efectivoEsperado | plata }}</span></div>
        @if (t.efectivoContado !== null) {
          <div class="linea"><span>Contado</span><span>{{ t.efectivoContado | plata }}</span></div>
          <div class="linea grande">
            <span>{{ t.diferencia! > 0 ? 'SOBRA' : t.diferencia! < 0 ? 'FALTA' : 'DA JUSTO' }}</span>
            <span>{{ (t.diferencia! < 0 ? -t.diferencia! : t.diferencia!) | plata }}</span>
          </div>
        }
        @if (t.observaciones) {
          <div class="corte"></div>
          <div>Nota: {{ t.observaciones }}</div>
        }
      </div>
    } @else {
      <p class="cargando">Cargando…</p>
    }
  `,
})
export class TicketCierre implements OnInit {
  readonly id = input.required({ transform: numberAttribute });
  readonly imprimirAlAbrir = input(false, { alias: 'imprimir', transform: (v: unknown) => v === '1' || v === true });

  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly turno = signal<Turno | null>(null);
  protected readonly nombreMedio = nombreMedio;
  protected readonly nombreTurno = nombreTurno;
  protected readonly ahora = new Date().toISOString();

  async ngOnInit() {
    try {
      this.turno.set(await this.api.turno(this.id()));
      if (this.imprimirAlAbrir()) {
        setTimeout(() => this.imprimir(), 300);
      }
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  protected imprimir() {
    window.print();
  }

  protected cerrarVentana() {
    window.close();
    history.back();
  }
}
