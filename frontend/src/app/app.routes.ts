/* © 2026 Ayoub — Tous droits réservés. */
import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { LoginComponent } from './features/login/login.component';
import { WorkspaceComponent } from './features/workspace/workspace.component';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'library', component: WorkspaceComponent, data: { page: 'library' } },
      { path: 'my-texts', component: WorkspaceComponent, data: { page: 'my-texts' } },
      { path: 'analysis', component: WorkspaceComponent, data: { page: 'analysis' } },
      { path: 'actions', component: WorkspaceComponent, data: { page: 'actions' } },
      { path: 'evidence', component: WorkspaceComponent, data: { page: 'evidence' } },
      { path: 'reports', component: WorkspaceComponent, data: { page: 'reports' } },
      { path: 'admin', component: WorkspaceComponent, data: { page: 'admin' } },
      { path: 'profile', component: WorkspaceComponent, data: { page: 'profile' } },
    ],
  },
  { path: '**', redirectTo: '' },
];
