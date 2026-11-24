/* © 2026 Ayoub — Tous droits réservés. */
import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppStateService, Conformity, calculateComplianceScore } from '../../core/app-state.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  imports: [RouterLink],
  selector: 'app-dashboard',
  styleUrl: './dashboard.component.scss',
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  readonly state = inject(AppStateService);
  private readonly i18n = inject(I18nService);
  readonly score = computed(() => calculateComplianceScore(this.state.texts()));
  readonly openActions = computed(() => this.state.actions().filter(action => action.status !== 'Réalisée'));
  readonly statCounts = computed(() => ({
    texts: this.state.texts().length,
    unanalyzed: this.state.texts().filter(text => text.conformity === 'Non analysé').length,
    noncompliant: this.state.texts().filter(text => text.conformity === 'Non conforme').length,
    overdue: this.state.actions().filter(action => action.status !== 'Réalisée' && action.due < new Date().toISOString().slice(0, 10)).length,
  }));
  readonly segments = computed(() => {
    const texts = this.state.texts();
    return [
      { key: 'common.conforming', count: texts.filter(t => t.conformity === 'Conforme').length, color: '#16a34a' },
      { key: 'common.candidate', count: texts.filter(t => t.conformity === 'Candidat conforme').length, color: '#2563eb' },
      { key: 'common.partial', count: texts.filter(t => t.conformity === 'Partiellement conforme').length, color: '#d97706' },
      { key: 'common.nonconforming', count: texts.filter(t => t.conformity === 'Non conforme').length, color: '#dc2626' },
      { key: 'common.unanalyzed', count: texts.filter(t => t.conformity === 'Non analysé').length, color: '#64748b' },
    ];
  });
  readonly departmentBars = [
    { name: 'Production', value: 0, color: 'var(--teal)' },
    { name: 'QHSE', value: 0, color: '#4673a0' },
    { name: 'Maintenance', value: 0, color: '#7995b2' },
    { name: 'Énergie', value: 0, color: '#9ab6aa' },
  ];
  readonly deadlines = computed(() => this.state.actions()
    .filter(action => action.status !== 'Réalisée')
    .sort((left, right) => left.due.localeCompare(right.due))
    .slice(0, 3)
    .map(action => {
      const date = new Date(`${action.due}T12:00:00`);
      return {
        day: new Intl.DateTimeFormat('fr-TN', { day: '2-digit' }).format(date),
        month: new Intl.DateTimeFormat('fr-TN', { month: 'short' }).format(date).replace('.', '').toUpperCase(),
        title: action.title,
        text: action.text,
        state: action.priority === 'Haute' ? 'urgent' : action.priority === 'Moyenne' ? 'soon' : 'normal',
      };
    }));

  t(key: string): string {
    return this.i18n.t(key);
  }

  statusLabel(status: Conformity): string {
    const key: Record<Conformity, string> = {
      Conforme: 'common.conforming', 'Candidat conforme': 'common.candidate',
      'Partiellement conforme': 'common.partial', 'Non conforme': 'common.nonconforming', 'Non analysé': 'common.unanalyzed',
    };
    return this.t(key[status]);
  }

  get donutGradient(): string {
    const total = this.segments().reduce((sum, item) => sum + item.count, 0) || 1;
    let start = 0;
    const stops = this.segments().map(segment => {
      const end = start + (segment.count / total) * 100;
      const stop = `${segment.color} ${start}% ${end}%`;
      start = end;
      return stop;
    });
    return `conic-gradient(${stops.join(', ')})`;
  }
}
