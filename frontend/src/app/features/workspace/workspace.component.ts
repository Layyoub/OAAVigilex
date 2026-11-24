/* © 2026 Ayoub — Tous droits réservés. */
import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ActionStatus, AppStateService, Conformity, CorrectiveAction, Locale, RegulatoryText } from '../../core/app-state.service';
import { I18nService } from '../../core/i18n.service';

type ActionView = 'table' | 'kanban' | 'calendar';
interface EvidenceFile {
  name: string;
  size: number;
  type: string;
  added: string;
}

@Component({
  imports: [DatePipe, FormsModule, RouterLink],
  selector: 'app-workspace',
  styleUrl: './workspace.component.scss',
  templateUrl: './workspace.component.html',
})
export class WorkspaceComponent {
  readonly state = inject(AppStateService);
  readonly i18n = inject(I18nService);
  readonly page = inject(ActivatedRoute).snapshot.data['page'] as string;
  readonly search = signal('');
  readonly statusFilter = signal('');
  readonly selectedIds = signal<number[]>([]);
  readonly actionView = signal<ActionView>('table');
  readonly dialogOpen = signal(false);
  readonly guideOpen = signal(false);
  readonly newActionTitle = signal('');
  readonly newActionOwner = signal('');
  readonly newActionDue = signal('');
  readonly uploadError = signal('');
  readonly evidenceFiles = signal<EvidenceFile[]>([
    { name: 'Rapport_audit_HSE_2026.pdf', size: 2_450_000, type: 'application/pdf', added: '18 nov. 2026' },
    { name: 'Registre_formation_securite.xlsx', size: 830_000, type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', added: '14 nov. 2026' },
    { name: 'Mesures_rejets_T3.png', size: 1_120_000, type: 'image/png', added: '08 nov. 2026' },
  ]);
  readonly visibleTexts = computed(() => {
    const query = this.search().trim().toLocaleLowerCase();
    return this.state.texts().filter(text => {
      const matchesQuery = !query || [text.title, text.reference, text.theme, text.authority].some(value => value.toLocaleLowerCase().includes(query));
      const matchesStatus = !this.statusFilter() || text.conformity === this.statusFilter();
      return matchesQuery && matchesStatus;
    });
  });
  readonly openActions = computed(() => this.state.actions().filter(action => action.status !== 'Réalisée'));
  readonly adminUsers = [
    { initials: 'AB', name: 'Ayoub Ben Salem', email: 'ayoub@tunis-industrie.tn', role: 'Responsable conformité', department: 'HSE', status: 'Actif' },
    { initials: 'SB', name: 'Sarra Ben Salah', email: 'sarra@tunis-industrie.tn', role: 'Contributrice', department: 'Production', status: 'Actif' },
    { initials: 'MT', name: 'Mehdi Trabelsi', email: 'mehdi@tunis-industrie.tn', role: 'Contributeur', department: 'Environnement', status: 'Actif' },
    { initials: 'NG', name: 'Nour Gharbi', email: 'nour@tunis-industrie.tn', role: 'Lectrice', department: 'Qualité', status: 'Invitation envoyée' },
  ];
  readonly departments = [
    { name: 'Hygiène, sécurité & environnement', code: 'HSE', users: 8, texts: 18, score: 82 },
    { name: 'Production', code: 'PROD', users: 24, texts: 12, score: 68 },
    { name: 'Maintenance', code: 'MAINT', users: 11, texts: 9, score: 74 },
    { name: 'Ressources humaines', code: 'RH', users: 6, texts: 7, score: 91 },
  ];

  t(key: string): string {
    return this.i18n.t(key);
  }

  get title(): string {
    return this.t(`page.${this.page}`);
  }

  get description(): string {
    return this.t(`page.${this.page}Desc`);
  }

  statusLabel(status: Conformity): string {
    const labels: Record<Conformity, string> = {
      Conforme: 'common.conforming',
      'Candidat conforme': 'common.candidate',
      'Partiellement conforme': 'common.partial',
      'Non conforme': 'common.nonconforming',
      'Non analysé': 'common.unanalyzed',
    };
    return this.t(labels[status]);
  }

  statusClass(status: string): string {
    return status.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replaceAll(' ', '-');
  }

  updateConformity(id: number, event: Event): void {
    const conformity = (event.target as HTMLSelectElement).value as Conformity;
    this.state.texts.update(texts => texts.map(text => text.id === id ? { ...text, conformity } : text));
    this.state.notify(this.state.locale() === 'en' ? 'Compliance status updated' : 'Statut de conformité mis à jour');
  }

  toggleSelected(id: number): void {
    this.selectedIds.update(ids => ids.includes(id) ? ids.filter(item => item !== id) : [...ids, id]);
  }

  toggleAll(event: Event): void {
    this.selectedIds.set((event.target as HTMLInputElement).checked ? this.visibleTexts().map(text => text.id) : []);
  }

  addSelected(): void {
    this.selectedIds().forEach(id => this.state.addToMyTexts(id));
    this.selectedIds.set([]);
  }

  changeActionStatus(id: number, status: ActionStatus): void {
    this.state.actions.update(items => items.map(action => action.id === id ? { ...action, status } : action));
    this.state.notify(this.state.locale() === 'en' ? 'Action status updated' : 'Statut de l’action mis à jour');
  }

  actionsFor(status: ActionStatus): CorrectiveAction[] {
    return this.state.actions().filter(action => action.status === status);
  }

  createAction(event: Event): void {
    event.preventDefault();
    if (!this.newActionTitle().trim() || !this.newActionOwner().trim() || !this.newActionDue()) return;
    const action: CorrectiveAction = {
      id: Date.now(),
      title: this.newActionTitle().trim(),
      text: this.state.texts()[0]?.reference ?? '',
      owner: this.newActionOwner().trim(),
      due: this.newActionDue(),
      priority: 'Moyenne',
      status: 'À faire',
    };
    this.state.actions.update(items => [action, ...items]);
    this.dialogOpen.set(false);
    this.newActionTitle.set('');
    this.newActionOwner.set('');
    this.newActionDue.set('');
    this.state.notify(this.state.locale() === 'en' ? 'Action added to your plan' : 'Action ajoutée à votre plan');
  }

  handleFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    const allowed = new Set(['application/pdf', 'image/jpeg', 'image/png', 'image/webp', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet']);
    const invalid = files.find(file => !allowed.has(file.type) || file.size > 10 * 1024 * 1024);
    if (invalid) {
      this.uploadError.set(invalid.size > 10 * 1024 * 1024
        ? 'Chaque fichier doit faire 10 Mo maximum.'
        : 'Format non autorisé. Choisissez un PDF, une image ou un document Office.');
      input.value = '';
      return;
    }
    this.uploadError.set('');
    const date = new Intl.DateTimeFormat(this.state.locale(), { day: '2-digit', month: 'short', year: 'numeric' }).format(new Date());
    this.evidenceFiles.update(current => [...files.map(file => ({ name: file.name, size: file.size, type: file.type, added: date })), ...current]);
    if (files.length) this.state.notify(this.state.locale() === 'en' ? `${files.length} document(s) added to evidence` : `${files.length} document(s) ajouté(s) aux preuves`);
    input.value = '';
  }

  removeEvidence(name: string): void {
    this.evidenceFiles.update(files => files.filter(file => file.name !== name));
    this.state.notify(this.state.locale() === 'en' ? 'Evidence removed from this list' : 'Preuve retirée de cette liste');
  }

  formatSize(bytes: number): string {
    return bytes >= 1_000_000 ? `${(bytes / 1_000_000).toFixed(1)} Mo` : `${Math.max(1, Math.round(bytes / 1000))} Ko`;
  }

  exportCsv(): void {
    const rows = [
      ['Référence', 'Intitulé', 'Type', 'Thème', 'Publication', 'Conformité'],
      ...this.visibleTexts().map(text => [text.reference, text.title, text.category, text.theme, text.date, text.conformity]),
    ];
    const csv = '\uFEFF' + rows.map(row => row.map(cell => `"${cell.replaceAll('"', '""')}"`).join(';')).join('\r\n');
    const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'vigilex-registre-conformite.csv';
    link.click();
    URL.revokeObjectURL(url);
    this.state.notify(this.state.locale() === 'en' ? 'Compliance register exported as CSV' : 'Registre exporté au format CSV');
  }

  printReport(): void {
    window.print();
  }

  setLocale(locale: Locale): void {
    this.state.setLocale(locale);
  }

  saveProfile(): void {
    this.state.notify(this.state.locale() === 'en' ? 'Preferences saved on this device' : 'Préférences enregistrées sur cet appareil');
  }
}
