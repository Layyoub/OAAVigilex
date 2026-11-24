/* © 2026 Ayoub — Tous droits réservés. */
import { Injectable, computed, signal } from '@angular/core';

export type Locale = 'fr' | 'en' | 'ar';
export type Theme = 'light' | 'dark';
export type Conformity = 'Conforme' | 'Candidat conforme' | 'Partiellement conforme' | 'Non conforme' | 'Non analysé';
export type ActionStatus = 'À faire' | 'En cours' | 'Réalisée';

export interface RegulatoryText {
  id: number;
  reference: string;
  title: string;
  category: string;
  authority: string;
  date: string;
  theme: string;
  conformity: Conformity;
  isNew?: boolean;
}

export interface CorrectiveAction {
  id: number;
  title: string;
  text: string;
  owner: string;
  due: string;
  priority: 'Haute' | 'Moyenne' | 'Basse';
  status: ActionStatus;
}

export function calculateComplianceScore(texts: RegulatoryText[]): number {
  const weights: Record<Conformity, number> = {
    Conforme: 100,
    'Candidat conforme': 75,
    'Partiellement conforme': 50,
    'Non conforme': 0,
    'Non analysé': 0,
  };
  const reviewed = texts.filter(text => text.conformity !== 'Non analysé');
  return reviewed.length
    ? Math.round(reviewed.reduce((sum, text) => sum + weights[text.conformity], 0) / reviewed.length)
    : 0;
}

@Injectable({ providedIn: 'root' })
export class AppStateService {
  readonly organization = {
    name: 'DECOPLAST',
    sector: 'Plasturgie',
    location: 'Tunisie',
  };
  readonly authenticated = signal(localStorage.getItem('vigilex-session') === 'active');
  readonly locale = signal<Locale>((localStorage.getItem('vigilex-locale') as Locale) || 'fr');
  readonly theme = signal<Theme>((localStorage.getItem('vigilex-theme') as Theme) || 'light');
  readonly sidebarCollapsed = signal(localStorage.getItem('vigilex-sidebar') === 'collapsed');
  readonly toast = signal('');
  readonly isRtl = computed(() => this.locale() === 'ar');

  readonly texts = signal<RegulatoryText[]>([
    { id: 1, reference: 'Loi n° 96-41', title: 'Déchets : gestion et élimination', category: 'Loi', authority: 'République tunisienne', date: '1996-06-10', theme: 'Déchets industriels', conformity: 'Non analysé' },
    { id: 2, reference: 'Décret n° 2005-1991', title: 'Étude d’impact sur l’environnement', category: 'Décret', authority: 'République tunisienne', date: '2005-07-11', theme: 'Environnement', conformity: 'Non analysé' },
    { id: 3, reference: 'Loi n° 2007-34', title: 'Qualité de l’air', category: 'Loi', authority: 'République tunisienne', date: '2007-06-04', theme: 'Émissions atmosphériques', conformity: 'Non analysé' },
    { id: 4, reference: 'Loi n° 2004-72', title: 'Maîtrise de l’énergie', category: 'Loi', authority: 'République tunisienne', date: '2004-08-02', theme: 'Énergie', conformity: 'Non analysé' },
  ]);
  readonly actions = signal<CorrectiveAction[]>([
    { id: 1, title: 'Cartographier les déchets de production', text: 'Loi n° 96-41', owner: 'Sarra Ben Salah', due: '2026-12-15', priority: 'Haute', status: 'À faire' },
    { id: 2, title: 'Vérifier les exigences environnementales du site', text: 'Décret n° 2005-1991', owner: 'Mehdi Trabelsi', due: '2026-12-22', priority: 'Moyenne', status: 'À faire' },
    { id: 3, title: 'Recenser les sources d’émissions atmosphériques', text: 'Loi n° 2007-34', owner: 'Nour Gharbi', due: '2027-01-08', priority: 'Haute', status: 'À faire' },
    { id: 4, title: 'Établir le suivi des consommations énergétiques', text: 'Loi n° 2004-72', owner: 'Ayoub Ben Salem', due: '2027-01-15', priority: 'Moyenne', status: 'À faire' },
  ]);

  login(email: string, password: string): boolean {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) || password.length < 8) return false;
    localStorage.setItem('vigilex-session', 'active');
    this.authenticated.set(true);
    return true;
  }

  logout(): void {
    localStorage.removeItem('vigilex-session');
    this.authenticated.set(false);
  }

  setLocale(locale: Locale): void {
    this.locale.set(locale);
    localStorage.setItem('vigilex-locale', locale);
    document.documentElement.lang = locale;
    document.documentElement.dir = locale === 'ar' ? 'rtl' : 'ltr';
  }

  toggleTheme(): void {
    this.setTheme(this.theme() === 'light' ? 'dark' : 'light');
  }

  setTheme(theme: Theme): void {
    this.theme.set(theme);
    localStorage.setItem('vigilex-theme', theme);
    document.documentElement.dataset['theme'] = theme;
  }

  toggleSidebar(): void {
    const collapsed = !this.sidebarCollapsed();
    this.sidebarCollapsed.set(collapsed);
    localStorage.setItem('vigilex-sidebar', collapsed ? 'collapsed' : 'expanded');
  }

  notify(message: string): void {
    this.toast.set(message);
    window.setTimeout(() => this.toast.set(''), 3200);
  }

  addToMyTexts(id: number): void {
    this.notify(this.locale() === 'en' ? 'Text added to your library' : this.locale() === 'ar' ? 'تمت إضافة النص إلى مكتبتك' : 'Texte ajouté à Mes textes');
  }

  completeAction(id: number): void {
    this.actions.update(items => items.map(action => action.id === id ? { ...action, status: 'Réalisée' } : action));
    this.notify(this.locale() === 'en' ? 'Action marked as complete' : this.locale() === 'ar' ? 'تم إكمال الإجراء' : 'Action marquée comme réalisée');
  }
}
