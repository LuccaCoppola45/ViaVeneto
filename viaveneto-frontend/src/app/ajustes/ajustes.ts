import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Api } from '../comun/api';
import { Avisos } from '../comun/avisos';
import { Categoria, MesaResumen, Producto } from '../comun/modelos';

type ProductoEditable = Producto & { cambiado: boolean };

/** Pantalla para el dueño o la encargada: precios, productos nuevos y cantidad de mesas. */
@Component({
  selector: 'app-ajustes',
  imports: [FormsModule, RouterLink],
  styleUrl: './ajustes.css',
  templateUrl: './ajustes.html',
})
export class Ajustes {
  private readonly api = inject(Api);
  private readonly avisos = inject(Avisos);

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly productos = signal<ProductoEditable[]>([]);
  protected readonly mesas = signal<MesaResumen[]>([]);
  protected readonly guardando = signal(false);

  protected nuevo = { nombre: '', precio: null as number | null, categoriaId: null as number | null };
  protected nuevaCategoria = '';
  protected nuevaMesa: number | null = null;

  protected readonly productosPorCategoria = computed(() =>
    this.categorias().map((c) => ({ ...c, editables: this.productos().filter((p) => p.categoriaId === c.id) })),
  );
  protected readonly siguienteMesa = computed(() => Math.max(0, ...this.mesas().map((m) => m.numero)) + 1);

  constructor() {
    this.cargar();
  }

  protected marcarCambio(producto: ProductoEditable) {
    producto.cambiado = true;
  }

  protected async guardarProducto(producto: ProductoEditable) {
    await this.hacer(async () => {
      const { id, cambiado, ...form } = producto;
      await this.api.editarProducto(id, form);
      producto.cambiado = false;
      this.avisos.ok(`${producto.nombre} guardado`);
    });
  }

  protected async agregarProducto() {
    const { nombre, precio, categoriaId } = this.nuevo;
    if (!nombre.trim() || precio === null || categoriaId === null) {
      this.avisos.error('Completá nombre, precio y categoría.');
      return;
    }
    await this.hacer(async () => {
      await this.api.crearProducto({ nombre, precio, categoriaId, activo: true });
      this.avisos.ok(`${nombre.trim()} agregado a la carta`);
      this.nuevo = { nombre: '', precio: null, categoriaId };
      await this.cargar();
    });
  }

  protected async agregarCategoria() {
    if (!this.nuevaCategoria.trim()) return;
    await this.hacer(async () => {
      const creada = await this.api.crearCategoria(this.nuevaCategoria);
      this.avisos.ok(`Categoría ${creada.nombre} creada`);
      this.nuevaCategoria = '';
      this.nuevo.categoriaId = creada.id;
      await this.cargar();
    });
  }

  protected async agregarMesa() {
    const numero = this.nuevaMesa ?? this.siguienteMesa();
    await this.hacer(async () => {
      await this.api.agregarMesa(numero);
      this.avisos.ok(`Mesa ${numero} agregada`);
      this.nuevaMesa = null;
      this.mesas.set(await this.api.mesas());
    });
  }

  protected async quitarMesa(mesa: MesaResumen) {
    if (!confirm(`¿Quitar la mesa ${mesa.numero} del salón?`)) return;
    await this.hacer(async () => {
      await this.api.quitarMesa(mesa.id);
      this.avisos.ok(`Mesa ${mesa.numero} quitada`);
      this.mesas.set(await this.api.mesas());
    });
  }

  private async cargar() {
    try {
      const [carta, mesas] = await Promise.all([this.api.carta(false), this.api.mesas()]);
      this.categorias.set(carta);
      this.productos.set(carta.flatMap((c) => c.productos.map((p) => ({ ...p, cambiado: false }))));
      this.mesas.set(mesas);
      this.nuevo.categoriaId ??= carta[0]?.id ?? null;
    } catch (error) {
      this.avisos.error((error as Error).message);
    }
  }

  private async hacer(accion: () => Promise<void>) {
    this.guardando.set(true);
    try {
      await accion();
    } catch (error) {
      this.avisos.error((error as Error).message);
    } finally {
      this.guardando.set(false);
    }
  }
}
