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
  { path: '**', redirectTo: 'mesas' },
];
