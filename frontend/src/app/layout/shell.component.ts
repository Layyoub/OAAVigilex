/* © 2026 Ayoub — Tous droits réservés. */
import { Component, HostListener, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AppStateService, Locale } from '../core/app-state.service';
import { I18nService } from '../core/i18n.service';

interface NavigationItem {
  label: string;
  path: string;
  icon: string;
  badge?: string;
}

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatButtonModule, MatTooltipModule],
  selector: 'app-shell',
  styleUrl: './shell.component.scss',
  templateUrl: './shell.component.html',
})
export class ShellComponent {
  readonly state = inject(AppStateService);
  readonly i18n = inject(I18nService);
  readonly search = signal('');
  readonly commandOpen = signal(false);
  readonly notificationsOpen = signal(false);
  readonly userMenuOpen = signal(false);
  readonly mobileNavOpen = signal(false);
  private readonly router = inject(Router);

  readonly workspaceItems: NavigationItem[] = [
    { label: 'nav.dashboard', path: '/dashboard', icon: 'grid' },
    { label: 'nav.library', path: '/library', icon: 'book' },
    { label: 'nav.myTexts', path: '/my-texts', icon: 'files', badge: '4' },
    { label: 'nav.analysis', path: '/analysis', icon: 'chart' },
    { label: 'nav.actions', path: '/actions', icon: 'checklist', badge: '4' },
    { label: 'nav.evidence', path: '/evidence', icon: 'folder' },
    { label: 'nav.reports', path: '/reports', icon: 'report' },
  ];
  readonly manageItems: NavigationItem[] = [
    { label: 'nav.admin', path: '/admin', icon: 'settings' },
    { label: 'nav.profile', path: '/profile', icon: 'user' },
  ];

  constructor() {
    document.documentElement.dataset['theme'] = this.state.theme();
    this.state.setLocale(this.state.locale());
    window.addEventListener('keydown', this.handleGlobalKeys);
  }

  ngOnDestroy(): void {
    window.removeEventListener('keydown', this.handleGlobalKeys);
  }

  private readonly handleGlobalKeys = (event: KeyboardEvent): void => {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault();
      this.commandOpen.update(open => !open);
    }
    if (event.key === 'Escape') {
      this.commandOpen.set(false);
      this.notificationsOpen.set(false);
      this.userMenuOpen.set(false);
      this.mobileNavOpen.set(false);
    }
  };

  @HostListener('document:keydown', ['$event'])
  focusCommand(event: KeyboardEvent): void {
    if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k') {
      window.setTimeout(() => document.getElementById('command-search')?.focus(), 0);
    }
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  get currentPageLabel(): string {
    const page = this.router.url.split('?')[0].split('/').filter(Boolean).pop() || 'dashboard';
    const label: Record<string, string> = {
      dashboard: 'nav.dashboard',
      library: 'nav.library',
      'my-texts': 'nav.myTexts',
      analysis: 'nav.analysis',
      actions: 'nav.actions',
      evidence: 'nav.evidence',
      reports: 'nav.reports',
      admin: 'nav.admin',
      profile: 'nav.profile',
    };
    return this.t(label[page] ?? 'nav.dashboard');
  }

  navigate(path: string): void {
    void this.router.navigateByUrl(path);
    this.commandOpen.set(false);
    this.mobileNavOpen.set(false);
  }

  filteredItems(): NavigationItem[] {
    const items = [...this.workspaceItems, ...this.manageItems];
    const query = this.search().trim().toLocaleLowerCase();
    return query ? items.filter(item => this.t(item.label).toLocaleLowerCase().includes(query)) : items;
  }

  setLocale(locale: Locale): void {
    this.state.setLocale(locale);
  }

  logout(): void {
    this.state.logout();
    this.userMenuOpen.set(false);
    void this.router.navigateByUrl('/login');
  }
}
