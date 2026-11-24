/* © 2026 Ayoub — Tous droits réservés. */
import { describe, expect, it } from 'vitest';
import { AppStateService, calculateComplianceScore, RegulatoryText } from './app-state.service';

const text = (conformity: RegulatoryText['conformity']): RegulatoryText => ({
  id: 1,
  reference: 'Loi n° 1',
  title: 'Sécurité au travail',
  category: 'Loi',
  authority: 'Autorité',
  date: '2026-01-01',
  theme: 'Sécurité',
  conformity,
});

describe('calculateComplianceScore', () => {
  it('applies the specified compliance weights', () => {
    expect(calculateComplianceScore([
      text('Conforme'),
      text('Candidat conforme'),
      text('Partiellement conforme'),
      text('Non conforme'),
    ])).toBe(56);
  });

  it('excludes unanalyzed texts from the score', () => {
    expect(calculateComplianceScore([text('Conforme'), text('Non analysé')])).toBe(100);
  });

  it('returns zero when no text has been analyzed', () => {
    expect(calculateComplianceScore([text('Non analysé')])).toBe(0);
    expect(calculateComplianceScore([])).toBe(0);
  });
});

describe('DECOPLAST demo workspace', () => {
  it('starts with Tunisian reference texts awaiting a real applicability review', () => {
    const workspace = new AppStateService();

    expect(workspace.organization.name).toBe('DECOPLAST');
    expect(workspace.texts().map(item => item.reference)).toEqual([
      'Loi n° 96-41',
      'Décret n° 2005-1991',
      'Loi n° 2007-34',
      'Loi n° 2004-72',
    ]);
    expect(workspace.texts().every(item => item.conformity === 'Non analysé')).toBe(true);
  });
});
