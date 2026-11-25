import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { Sesion } from './sesion';

/** Las llamadas de ingreso no llevan token y un 401 ahí es "PIN incorrecto", no "sesión vencida". */
const ES_INGRESO = /\/api\/auth\/(estado|ingresar|primer-usuario)$/;

/** Le agrega el token a cada llamada y, si el servidor dice que la sesión no sirve, manda a poner el PIN. */
export const conToken: HttpInterceptorFn = (pedido, siguiente) => {
  const sesion = inject(Sesion);
  const router = inject(Router);
  const token = sesion.token();
  const conEncabezado = token ? pedido.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : pedido;

  return siguiente(conEncabezado).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !ES_INGRESO.test(pedido.url)) {
        sesion.terminar();
        router.navigate(['/ingresar'], { queryParams: { volver: router.url } });
      }
      return throwError(() => error);
    }),
  );
};

/** Sin sesión no se ve ninguna pantalla: se va a ingresar y después vuelve a donde quería ir. */
export const conSesion: CanActivateFn = (_ruta, estado) => {
  if (inject(Sesion).activa()) return true;
  return inject(Router).createUrlTree(['/ingresar'], { queryParams: { volver: estado.url } });
};
