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
  readonly statCounts = computed(() => ({
    texts: this.state.texts().length,
    unanalyzed: this.state.texts().filter(text => text.conformity === 'Non analysé').length,
    noncompliant: this.state.texts().filter(text => text.conformity === 'Non conforme').length,
    overdue: this.state.actions().filter(action => action.status !== 'Réalisée' && action.due < '2026-11-24').length,
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
    { name: 'HSE', value: 82, color: 'var(--teal)' },
    { name: 'Production', value: 68, color: '#4673a0' },
    { name: 'Maintenance', value: 74, color: '#7995b2' },
    { name: 'Ressources humaines', value: 91, color: '#9ab6aa' },
  ];
  readonly deadlines = [
    { day: '27', month: 'NOV', title: 'Mettre à jour le plan de prévention', text: 'Loi n° 2019-25', state: 'urgent' },
    { day: '29', month: 'NOV', title: 'Formation au tri sélectif', text: 'Décret n° 2018-447', state: 'soon' },
    { day: '03', month: 'DÉC', title: 'Mesures de rejets industriels', text: 'Arrêté du 15/01/2022', state: 'normal' },
  ];

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
