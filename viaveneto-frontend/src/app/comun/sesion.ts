import { Injectable, computed, signal } from '@angular/core';

export type UsuarioSesion = { id: number; nombre: string };

const CLAVE_TOKEN = 'viaveneto.token';
const CLAVE_USUARIO = 'viaveneto.usuario';

const leer = (clave: string) => {
  try {
    return localStorage.getItem(clave);
  } catch {
    return null;
  }
};

const escribir = (clave: string, valor: string | null) => {
  try {
    if (valor === null) localStorage.removeItem(clave);
    else localStorage.setItem(clave, valor);
  } catch {
    // Sin almacenamiento (modo privado): la sesión dura mientras esté abierta la pestaña.
  }
};

const leerUsuario = (): UsuarioSesion | null => {
  try {
    return JSON.parse(leer(CLAVE_USUARIO) ?? 'null');
  } catch {
    return null;
  }
};

/**
 * Quién está usando esta tablet. El token queda guardado en el dispositivo
 * para que no haya que poner el PIN cada vez que se recarga la página.
 */
@Injectable({ providedIn: 'root' })
export class Sesion {
  private readonly tokenActual = signal<string | null>(leer(CLAVE_TOKEN));
  readonly usuario = signal<UsuarioSesion | null>(leerUsuario());
  readonly activa = computed(() => this.tokenActual() !== null);

  token() {
    return this.tokenActual();
  }

  iniciar(token: string, usuario: UsuarioSesion) {
    escribir(CLAVE_TOKEN, token);
    escribir(CLAVE_USUARIO, JSON.stringify(usuario));
    this.tokenActual.set(token);
    this.usuario.set(usuario);
  }

  terminar() {
    escribir(CLAVE_TOKEN, null);
    escribir(CLAVE_USUARIO, null);
    this.tokenActual.set(null);
    this.usuario.set(null);
  }
}
