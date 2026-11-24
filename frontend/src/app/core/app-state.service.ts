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
  readonly authenticated = signal(localStorage.getItem('vigilex-session') === 'active');
  readonly locale = signal<Locale>((localStorage.getItem('vigilex-locale') as Locale) || 'fr');
  readonly theme = signal<Theme>((localStorage.getItem('vigilex-theme') as Theme) || 'light');
  readonly sidebarCollapsed = signal(localStorage.getItem('vigilex-sidebar') === 'collapsed');
  readonly toast = signal('');
  readonly isRtl = computed(() => this.locale() === 'ar');

  readonly texts = signal<RegulatoryText[]>([
    { id: 1, reference: 'Loi n° 2019-25', title: 'Sécurité et santé au travail', category: 'Loi', authority: 'Assemblée des représentants du peuple', date: '2019-03-26', theme: 'Santé & sécurité', conformity: 'Partiellement conforme', isNew: true },
    { id: 2, reference: 'Décret n° 2006-2687', title: 'Étude d’impact sur l’environnement', category: 'Décret', authority: 'Ministère de l’Environnement', date: '2006-10-09', theme: 'Environnement', conformity: 'Conforme' },
    { id: 3, reference: 'Loi n° 2015-12', title: 'Production de l’électricité à partir des énergies renouvelables', category: 'Loi', authority: 'Ministère de l’Énergie', date: '2015-05-11', theme: 'Énergie', conformity: 'Candidat conforme' },
    { id: 4, reference: 'Arrêté du 15/01/2022', title: 'Valeurs limites des rejets industriels', category: 'Arrêté', authority: 'Ministère de l’Environnement', date: '2022-01-15', theme: 'Environnement', conformity: 'Non conforme' },
    { id: 5, reference: 'Décret n° 2018-447', title: 'Gestion des déchets dangereux', category: 'Décret', authority: 'Ministère de l’Environnement', date: '2018-04-18', theme: 'Déchets', conformity: 'Non analysé' },
  ]);
  readonly actions = signal<CorrectiveAction[]>([
    { id: 1, title: 'Mettre à jour le plan de prévention', text: 'Loi n° 2019-25', owner: 'S. Ben Salah', due: '2026-11-27', priority: 'Haute', status: 'En cours' },
    { id: 2, title: 'Former les équipes au tri sélectif', text: 'Décret n° 2018-447', owner: 'M. Trabelsi', due: '2026-11-29', priority: 'Moyenne', status: 'À faire' },
    { id: 3, title: 'Réaliser les mesures de rejets', text: 'Arrêté du 15/01/2022', owner: 'A. Gharbi', due: '2026-12-03', priority: 'Haute', status: 'À faire' },
    { id: 4, title: 'Documenter la revue énergétique', text: 'Loi n° 2015-12', owner: 'N. Mansour', due: '2026-12-08', priority: 'Basse', status: 'Réalisée' },
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
