/* © 2026 Ayoub — Tous droits réservés. */
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AppStateService } from './app-state.service';

export const authGuard: CanActivateFn = () => {
  const state = inject(AppStateService);
  return state.authenticated() || inject(Router).createUrlTree(['/login']);
};
