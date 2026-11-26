import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe } from '../comun/formato';
import { MEDIOS, nombreTurno } from '../comun/medios';
import { Caja, MedioPago, ResumenCaja, TipoTurno, Turno } from '../comun/modelos';
import { refrescarCadaRato } from '../comun/refresco';
import { abrirCierreDeTurno } from '../ticket/abrir-ticket';

/** Quiénes suelen estar en la caja, para no tener que escribir. */
const RESPONSABLES = ['Abuela', 'Mamá', 'Lucca'];

@Component({
  selector: 'app-caja',
  imports: [PlataPipe, DatePipe],
  styleUrl: './caja.css',
  templateUrl: './caja.html',
})
export class CajaPagina {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly MEDIOS = MEDIOS;
  protected readonly RESPONSABLES = RESPONSABLES;
  protected readonly nombreTurno = nombreTurno;

  protected readonly caja = signal<Caja | null>(null);
  protected readonly trabajando = signal(false);

  // Abrir turno
  protected readonly tipo = signal<TipoTurno>('MEDIODIA');
  protected readonly responsable = signal('');
  protected readonly fondo = signal<number | null>(null);

  // Cerrar turno
  protected readonly cerrando = signal(false);
  protected readonly contado = signal<number | null>(null);
  protected readonly observaciones = signal('');
  protected readonly recienCerrado = signal<Turno | null>(null);

  protected readonly abierto = computed(() => this.caja()?.turnoAbierto ?? null);
  protected readonly diferencia = computed(() => {
    const contado = this.contado();
    const abierto = this.abierto();
    return contado === null || !abierto ? null : contado - abierto.efectivoEsperado;
  });

  constructor() {
    this.cargar(true);
    refrescarCadaRato(() => this.cargar(false));
  }

  protected montoDe(resumen: ResumenCaja, medio: MedioPago) {
    return resumen.porMedio.find((m) => m.medio === medio) ?? { monto: 0, cobros: 0 };
  }

  protected escribirNumero(destino: 'fondo' | 'contado', valor: string) {
    this[destino].set(valor === '' ? null : Number(valor));
  }

  protected async abrir() {
    if (!this.responsable().trim()) {
      this.avisos.error('Elegí o escribí quién está en la caja.');
      return;
    }
    await this.trabajar(async () => {
      await this.api.abrirTurno(this.tipo(), this.responsable(), this.fondo() ?? 0);
      this.avisos.ok(`Caja abierta: turno ${nombreTurno(this.tipo()).toLowerCase()}`);
      this.recienCerrado.set(null);
      await this.cargar(true);
    });
  }

  protected empezarCierre() {
    this.contado.set(null);
    this.observaciones.set('');
    this.cerrando.set(true);
  }

  protected async cerrar() {
    const abierto = this.abierto();
    const contado = this.contado();
    if (!abierto || contado === null) return;
    await this.trabajar(async () => {
      const cerrado = await this.api.cerrarTurno(abierto.id, contado, this.observaciones());
      this.cerrando.set(false);
      this.recienCerrado.set(cerrado);
      this.avisos.ok('Turno cerrado');
      await this.cargar(true);
    });
  }

  protected async facturado(pedidoId: number) {
    await this.trabajar(async () => {
      await this.api.marcarFacturado(pedidoId, true);
      await this.cargar(false);
    });
  }

  protected imprimir(turno: Turno) {
    abrirCierreDeTurno(turno.id);
  }

  private async cargar(prepararFormulario: boolean) {
    try {
      const caja = await this.api.caja();
      this.caja.set(caja);
      if (prepararFormulario) {
        this.tipo.set(caja.sugerencia.tipo);
        this.fondo.set(caja.sugerencia.fondoInicial);
      }
    } catch (error) {
      if (prepararFormulario) this.avisos.error((error as Error).message);
    }
  }

  private async trabajar(accion: () => Promise<void>) {
    this.trabajando.set(true);
    try {
      await accion();
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.trabajando.set(false);
    }
  }
}
