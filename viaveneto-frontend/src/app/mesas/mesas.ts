import { Component, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { PlataPipe, tiempoDesde } from '../comun/formato';
import { MesaResumen, Salon, Sector } from '../comun/modelos';
import { refrescarCadaRato } from '../comun/refresco';
import { ELEMENTOS, MesaDibujo, estiloPieza, tamanioMesa } from '../salon/dibujo';
import { sectorElegido } from '../salon/sector-elegido';

const FINO = 50;

@Component({
  selector: 'app-mesas',
  imports: [PlataPipe, MesaDibujo],
  styleUrl: './mesas.css',
  templateUrl: './mesas.html',
})
export class Mesas {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);
  private readonly router = inject(Router);
  private readonly sector = sectorElegido();

  protected readonly salon = signal<Salon | null>(null);
  protected readonly mesas = signal<MesaResumen[]>([]);
  protected readonly abriendo = signal<number | null>(null);
  private readonly ahora = signal(Date.now());

  protected readonly ELEMENTOS = ELEMENTOS;

  protected readonly sectorActual = computed<Sector | undefined>(() => {
    const sectores = this.salon()?.sectores ?? [];
    return sectores.find((s) => s.id === this.sector.elegido()) ?? sectores[0];
  });

  /** Las mesas sin sector (creadas antes de que existiera el plano) se muestran en el primero. */
  private readonly sectorDeMesa = (mesa: MesaResumen) => mesa.sectorId ?? this.salon()?.sectores[0]?.id;

  protected readonly mesasDelSector = computed(() =>
    this.mesas().filter((m) => this.sectorDeMesa(m) === this.sectorActual()?.id),
  );

  protected readonly libresTotal = computed(() => this.mesas().filter((m) => !m.ocupada).length);

  constructor() {
    this.cargarTodo();
    refrescarCadaRato(() => this.cargarMesas());
  }

  protected elegirSector(sector: Sector) {
    this.sector.elegir(sector.id);
  }

  protected ocupadasEn(sector: Sector) {
    return this.mesas().filter((m) => m.ocupada && this.sectorDeMesa(m) === sector.id).length;
  }

  protected estiloMesa(mesa: MesaResumen) {
    const medidas = this.salon()!.medidas;
    const { ancho, alto } = tamanioMesa(medidas, mesa.vertical);
    return estiloPieza(medidas, mesa.x, mesa.y, ancho, alto);
  }

  protected estiloElemento(e: { x: number; y: number; ancho: number; alto: number }) {
    return estiloPieza(this.salon()!.medidas, e.x, e.y, e.ancho, e.alto);
  }

  protected esFino(e: { ancho: number; alto: number }) {
    return Math.min(e.ancho, e.alto) < FINO;
  }

  protected hace(mesa: MesaResumen) {
    return mesa.abiertaDesde ? tiempoDesde(mesa.abiertaDesde, this.ahora()) : '';
  }

  /** Mesa libre: la abre. Mesa ocupada: muestra su pedido. */
  protected async entrar(mesa: MesaResumen) {
    if (mesa.pedidoId) {
      this.router.navigate(['/pedidos', mesa.pedidoId]);
      return;
    }
    this.abriendo.set(mesa.id);
    try {
      const pedido = await this.api.abrirMesa(mesa.id);
      this.router.navigate(['/pedidos', pedido.id]);
    } catch (error) {
      this.avisos.error((error as Error).message);
      this.abriendo.set(null);
    }
  }

  private async cargarTodo() {
    try {
      const [salon, mesas] = await Promise.all([this.api.salon(), this.api.mesas()]);
      this.salon.set(salon);
      this.mesas.set(mesas);
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async cargarMesas() {
    try {
      this.mesas.set(await this.api.mesas());
      this.ahora.set(Date.now());
    } catch {
      // El próximo refresco lo reintenta; no llenamos la pantalla de avisos.
    }
  }
}
