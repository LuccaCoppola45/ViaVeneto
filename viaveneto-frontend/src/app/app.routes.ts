import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'mesas' },
  { path: 'mesas', title: 'Mesas · Via Veneto', loadComponent: () => import('./mesas/mesas').then((m) => m.Mesas) },
  {
    path: 'pedidos/:id',
    title: 'Pedido · Via Veneto',
    loadComponent: () => import('./pedido/pedido').then((m) => m.PedidoPagina),
  },
  { path: 'carta', title: 'Carta y mesas · Via Veneto', loadComponent: () => import('./ajustes/ajustes').then((m) => m.Ajustes) },
  {
    path: 'salon',
    title: 'Acomodar salón · Via Veneto',
    loadComponent: () => import('./salon/acomodar').then((m) => m.AcomodarSalon),
    canDeactivate: [
      (pagina: { cambios: () => boolean }) =>
        !pagina.cambios() || confirm('Tenés cambios sin guardar en el plano. ¿Salir igual?'),
    ],
  },
  { path: '**', redirectTo: 'mesas' },
];
