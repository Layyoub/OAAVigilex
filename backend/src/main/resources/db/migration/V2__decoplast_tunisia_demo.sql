-- © 2026 Ayoub — Tous droits réservés
ALTER TABLE tenant
  ADD COLUMN country_code varchar(2) NOT NULL DEFAULT 'TN',
  ADD COLUMN city varchar(120);

ALTER TABLE law_text DROP CONSTRAINT law_text_status_check;
ALTER TABLE law_text ADD CONSTRAINT law_text_status_check
  CHECK (status IN ('IN_FORCE', 'REPEALED', 'AMENDED', 'TO_VERIFY'));

UPDATE tenant
SET name = 'DECOPLAST',
    slug = 'decoplast',
    sector = 'Plasturgie',
    country_code = 'TN',
    default_locale = 'fr'
WHERE slug = 'demo-nord';

UPDATE tenant
SET name = 'Organisation de démonstration - Sud',
    country_code = 'TN'
WHERE slug = 'demo-sud';

INSERT INTO organization_settings(tenant_id, organization_name, default_locale)
SELECT id, name, default_locale
FROM tenant
ON CONFLICT (tenant_id) DO UPDATE
SET organization_name = EXCLUDED.organization_name,
    default_locale = EXCLUDED.default_locale;

INSERT INTO department(tenant_id, code, name)
SELECT id, department.code, department.name
FROM tenant
CROSS JOIN (VALUES
  ('QHSE', 'Qualité, hygiène, sécurité et environnement'),
  ('PROD', 'Production plastique'),
  ('MAINT', 'Maintenance industrielle'),
  ('ENER', 'Énergie et utilités')
) AS department(code, name)
WHERE tenant.slug = 'decoplast'
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO stakeholder(tenant_id, name, type)
SELECT id, stakeholder.name, stakeholder.type
FROM tenant
CROSS JOIN (VALUES
  ('Agence Nationale de Gestion des Déchets', 'Organisme public'),
  ('Agence Nationale de Protection de l’Environnement', 'Organisme public'),
  ('Société Tunisienne de l’Électricité et du Gaz', 'Fournisseur')
) AS stakeholder(name, type)
WHERE tenant.slug = 'decoplast';

INSERT INTO law_text(id, reference, type, title_fr, title_ar, authority, published_on, status, summary)
VALUES
  ('20000000-0000-0000-0000-000000000001', 'Loi n° 96-41', 'LAW',
   'Déchets : gestion et élimination',
   'النفايات: التصرف والإزالة',
   'République tunisienne', DATE '1996-06-10', 'TO_VERIFY',
   'Référence tunisienne de démonstration pour la gestion des déchets. Vérifier le texte consolidé et sa publication au JORT avant toute décision de conformité.'),
  ('20000000-0000-0000-0000-000000000002', 'Décret n° 2005-1991', 'DECREE',
   'Étude d’impact sur l’environnement',
   'دراسة المؤثرات على المحيط',
   'République tunisienne', DATE '2005-07-11', 'TO_VERIFY',
   'Référence tunisienne de démonstration concernant les études d’impact. Vérifier les catégories et le texte consolidé au JORT avant toute décision de conformité.'),
  ('20000000-0000-0000-0000-000000000003', 'Loi n° 2007-34', 'LAW',
   'Qualité de l’air',
   'جودة الهواء',
   'République tunisienne', DATE '2007-06-04', 'TO_VERIFY',
   'Référence tunisienne de démonstration sur la qualité de l’air. Vérifier le texte consolidé et son applicabilité au site au JORT avant toute décision de conformité.'),
  ('20000000-0000-0000-0000-000000000004', 'Loi n° 2004-72', 'LAW',
   'Maîtrise de l’énergie',
   'التحكم في الطاقة',
   'République tunisienne', DATE '2004-08-02', 'TO_VERIFY',
   'Référence tunisienne de démonstration sur la maîtrise de l’énergie. Vérifier le texte consolidé et les exigences applicables au JORT avant toute décision de conformité.')
ON CONFLICT (reference) DO NOTHING;

INSERT INTO law_text_theme(law_text_id, theme_id)
SELECT law_text.id, theme.id
FROM law_text
JOIN theme ON theme.code = CASE law_text.reference
  WHEN 'Loi n° 96-41' THEN 'ENV'
  WHEN 'Décret n° 2005-1991' THEN 'ENV'
  WHEN 'Loi n° 2007-34' THEN 'ENV'
  WHEN 'Loi n° 2004-72' THEN 'ENERGY'
END
WHERE law_text.reference IN ('Loi n° 96-41', 'Décret n° 2005-1991', 'Loi n° 2007-34', 'Loi n° 2004-72')
ON CONFLICT DO NOTHING;

INSERT INTO tenant_text(tenant_id, law_text_id, applicability, applicability_reason, conformity, next_review_on)
SELECT tenant.id, law_text.id, 'TO_REVIEW',
       'Référence de départ : confirmer la version officielle au JORT et qualifier son applicabilité au site DECOPLAST.',
       'NOT_ANALYZED', DATE '2027-01-15'
FROM tenant
CROSS JOIN law_text
WHERE tenant.slug = 'decoplast'
  AND law_text.reference IN ('Loi n° 96-41', 'Décret n° 2005-1991', 'Loi n° 2007-34', 'Loi n° 2004-72')
ON CONFLICT (tenant_id, law_text_id) DO NOTHING;

INSERT INTO tenant_text_department(tenant_text_id, department_id)
SELECT tenant_text.id, department.id
FROM tenant_text
JOIN tenant ON tenant.id = tenant_text.tenant_id
JOIN law_text ON law_text.id = tenant_text.law_text_id
JOIN department ON department.tenant_id = tenant.id
WHERE tenant.slug = 'decoplast'
  AND (
    (law_text.reference = 'Loi n° 96-41' AND department.code IN ('QHSE', 'PROD'))
    OR (law_text.reference = 'Décret n° 2005-1991' AND department.code = 'QHSE')
    OR (law_text.reference = 'Loi n° 2007-34' AND department.code IN ('QHSE', 'PROD'))
    OR (law_text.reference = 'Loi n° 2004-72' AND department.code IN ('ENER', 'MAINT'))
  )
ON CONFLICT (tenant_text_id, department_id) DO NOTHING;

INSERT INTO action(tenant_id, tenant_text_id, title, description, priority, due_on, status)
SELECT tenant.id, tenant_text.id, action_seed.title, action_seed.description,
       action_seed.priority, action_seed.due_on, 'TO_DO'
FROM tenant
JOIN tenant_text ON tenant_text.tenant_id = tenant.id
JOIN law_text ON law_text.id = tenant_text.law_text_id
JOIN (VALUES
  ('Loi n° 96-41', 'Cartographier les déchets de production', 'Recenser les flux de déchets et vérifier les filières de gestion applicables au site.', 'HIGH', DATE '2026-12-15'),
  ('Décret n° 2005-1991', 'Vérifier le périmètre de l’étude d’impact', 'Confirmer auprès des sources officielles les exigences applicables à l’activité et au site.', 'MEDIUM', DATE '2026-12-22'),
  ('Loi n° 2007-34', 'Recenser les sources d’émissions atmosphériques', 'Identifier les équipements et émissions à examiner lors de la revue QHSE.', 'HIGH', DATE '2027-01-08'),
  ('Loi n° 2004-72', 'Établir le suivi des consommations énergétiques', 'Consolider les consommations du site et vérifier les obligations applicables.', 'MEDIUM', DATE '2027-01-15')
) AS action_seed(reference, title, description, priority, due_on) ON action_seed.reference = law_text.reference
WHERE tenant.slug = 'decoplast'
  AND NOT EXISTS (
    SELECT 1 FROM action existing
    WHERE existing.tenant_id = tenant.id
      AND existing.tenant_text_id = tenant_text.id
      AND existing.title = action_seed.title
  );
