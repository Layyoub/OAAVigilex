/* © 2026 Ayoub — Tous droits réservés. */
import { describe, expect, it } from 'vitest';
import { calculateComplianceScore, RegulatoryText } from './app-state.service';

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
