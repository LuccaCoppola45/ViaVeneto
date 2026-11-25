import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { CantidadPipe } from '../comun/formato';
import { EstadoStock, Insumo, InsumoForm, Receta, Stock, Unidad } from '../comun/modelos';

type Pestania = 'insumos' | 'recetas';
type Filtro = 'todos' | 'comprar';
/** Lo que se está haciendo con un insumo en la ventanita. */
type Accion = { tipo: 'compra' | 'conteo' | 'editar'; insumo: Insumo };
type LineaEditable = { insumoId: number | null; cantidad: number | null };

export const UNIDADES: { valor: Unidad; nombre: string }[] = [
  { valor: 'UNIDAD', nombre: 'Unidades' },
  { valor: 'KILO', nombre: 'Kilos' },
  { valor: 'LITRO', nombre: 'Litros' },
];

const ORDEN_ESTADO: Record<EstadoStock, number> = { SIN_STOCK: 0, BAJO: 1, OK: 2 };

/** Panel para ver y cargar el stock de todo, y decidir qué descuenta cada plato. */
@Component({
  selector: 'app-stock',
  imports: [FormsModule, CantidadPipe],
  styleUrl: './stock.css',
  templateUrl: './stock.html',
})
export class StockPagina {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly UNIDADES = UNIDADES;

  protected readonly stock = signal<Stock | null>(null);
  protected readonly pestania = signal<Pestania>('insumos');
  protected readonly filtro = signal<Filtro>('todos');
  protected readonly busqueda = signal('');
  protected readonly trabajando = signal(false);

  protected readonly accion = signal<Accion | null>(null);
  protected valorAccion: number | null = null;
  protected edicion: InsumoForm = { nombre: '', unidad: 'UNIDAD', cantidad: null, minimo: 0 };

  protected readonly agregando = signal(false);
  protected nuevo: InsumoForm = { nombre: '', unidad: 'UNIDAD', cantidad: null, minimo: 0 };

  protected readonly editandoReceta = signal<Receta | null>(null);
  protected lineas: LineaEditable[] = [];

  protected readonly insumosVisibles = computed(() => {
    const texto = this.busqueda().trim().toLowerCase();
    return (this.stock()?.insumos ?? [])
      .filter((i) => this.filtro() === 'todos' || i.estado !== 'OK')
      .filter((i) => !texto || i.nombre.toLowerCase().includes(texto))
      .sort((a, b) => ORDEN_ESTADO[a.estado] - ORDEN_ESTADO[b.estado] || a.nombre.localeCompare(b.nombre));
  });
  protected readonly paraComprar = computed(() => {
    const alertas = this.stock()?.alertas;
    return alertas ? alertas.bajos + alertas.sinStock : 0;
  });
  protected readonly recetasPorCategoria = computed(() => {
    const grupos = new Map<string, Receta[]>();
    for (const receta of this.stock()?.recetas ?? []) {
      if (!receta.activo) continue;
      grupos.set(receta.categoria, [...(grupos.get(receta.categoria) ?? []), receta]);
    }
    return [...grupos.entries()].map(([categoria, recetas]) => ({ categoria, recetas }));
  });

  constructor() {
    this.cargar();
  }

  protected nombreEstado(estado: EstadoStock) {
    return estado === 'OK' ? 'Bien' : estado === 'BAJO' ? 'Hay que comprar' : 'Se acabó';
  }

  protected resumenReceta(receta: Receta) {
    if (!receta.lineas.length) return 'No controla stock';
    const [unica] = receta.lineas;
    if (receta.lineas.length === 1 && unica.cantidad === 1 && unica.insumo === receta.producto) return 'Por unidad';
    return receta.lineas.map((l) => `${l.cantidad.toLocaleString('es-AR')} ${sufijo(l.unidad)} ${l.insumo}`).join(' + ');
  }

  // ---------- Insumos ----------

  protected abrirAccion(tipo: Accion['tipo'], insumo: Insumo) {
    this.valorAccion = tipo === 'conteo' ? insumo.cantidad : null;
    this.edicion = { nombre: insumo.nombre, unidad: insumo.unidad, cantidad: null, minimo: insumo.minimo };
    this.accion.set({ tipo, insumo });
  }

  protected async confirmarAccion() {
    const accion = this.accion();
    if (!accion) return;
    const { tipo, insumo } = accion;
    if (tipo !== 'editar' && (this.valorAccion === null || this.valorAccion < 0)) {
      this.avisos.error('Poné la cantidad.');
      return;
    }
    await this.hacer(async () => {
      if (tipo === 'compra') {
        await this.api.cargarCompra(insumo.id, this.valorAccion!);
        this.avisos.ok(`Se sumó a ${insumo.nombre}`);
      } else if (tipo === 'conteo') {
        await this.api.registrarConteo(insumo.id, this.valorAccion!);
        this.avisos.ok(`${insumo.nombre} actualizado`);
      } else {
        await this.api.editarInsumo(insumo.id, this.edicion);
        this.avisos.ok(`${this.edicion.nombre} guardado`);
      }
      this.accion.set(null);
    });
  }

  protected async quitar(insumo: Insumo) {
    const aviso = insumo.usadoEn.length ? ` Los platos que lo usan (${insumo.usadoEn.join(', ')}) dejan de descontarlo.` : '';
    if (!confirm(`¿Dejar de controlar ${insumo.nombre}?${aviso}`)) return;
    await this.hacer(async () => {
      await this.api.quitarInsumo(insumo.id);
      this.avisos.ok(`${insumo.nombre} se quitó del stock`);
      this.accion.set(null);
    });
  }

  protected async agregarInsumo() {
    if (!this.nuevo.nombre.trim()) {
      this.avisos.error('Poné el nombre.');
      return;
    }
    await this.hacer(async () => {
      await this.api.crearInsumo(this.nuevo);
      this.avisos.ok(`${this.nuevo.nombre.trim()} agregado al stock`);
      this.nuevo = { nombre: '', unidad: this.nuevo.unidad, cantidad: null, minimo: 0 };
      this.agregando.set(false);
    });
  }

  // ---------- Qué descuenta cada plato ----------

  protected async porUnidad(receta: Receta) {
    await this.hacer(async () => {
      await this.api.controlarPorUnidad(receta.productoId);
      this.avisos.ok(`${receta.producto} descuenta 1 por cada venta. Cargá cuántos hay en Insumos.`);
    });
  }

  protected abrirReceta(receta: Receta) {
    this.lineas = receta.lineas.length
      ? receta.lineas.map((l) => ({ insumoId: l.insumoId, cantidad: l.cantidad }))
      : [{ insumoId: null, cantidad: null }];
    this.editandoReceta.set(receta);
  }

  protected sumarLinea() {
    this.lineas = [...this.lineas, { insumoId: null, cantidad: null }];
  }

  protected sacarLinea(indice: number) {
    this.lineas = this.lineas.filter((_, i) => i !== indice);
  }

  protected sufijoDe(insumoId: number | null) {
    const unidad = this.stock()?.insumos.find((i) => i.id === insumoId)?.unidad ?? 'UNIDAD';
    return unidad === 'UNIDAD' ? 'u.' : unidad === 'KILO' ? 'kg' : 'L';
  }

  protected async guardarReceta(sinControl = false) {
    const receta = this.editandoReceta();
    if (!receta) return;
    const completas = sinControl ? [] : this.lineas.filter((l) => l.insumoId !== null && (l.cantidad ?? 0) > 0);
    if (!sinControl && completas.length !== this.lineas.length) {
      this.avisos.error('Completá cada renglón (insumo y cantidad) o sacalo.');
      return;
    }
    await this.hacer(async () => {
      await this.api.guardarReceta(
        receta.productoId,
        completas.map((l) => ({ insumoId: l.insumoId!, cantidad: l.cantidad! })),
      );
      this.avisos.ok(sinControl ? `${receta.producto} ya no controla stock` : `Receta de ${receta.producto} guardada`);
      this.editandoReceta.set(null);
    });
  }

  private async cargar() {
    try {
      this.stock.set(await this.api.stock());
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async hacer(accion: () => Promise<void>) {
    this.trabajando.set(true);
    try {
      await accion();
      await this.cargar();
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.trabajando.set(false);
    }
  }
}

const sufijo = (unidad: Unidad) => (unidad === 'UNIDAD' ? 'x' : unidad === 'KILO' ? 'kg' : 'L');
