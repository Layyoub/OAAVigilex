CREATE EXTENSION IF NOT EXISTS unaccent;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE tenant (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name varchar(180) NOT NULL,
  slug varchar(90) NOT NULL UNIQUE,
  logo_path varchar(500),
  sector varchar(120),
  default_locale varchar(5) NOT NULL DEFAULT 'fr',
  status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SUSPENDED')),
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id),
  email varchar(254) NOT NULL UNIQUE,
  password_hash varchar(100) NOT NULL,
  full_name varchar(180) NOT NULL,
  phone varchar(40),
  role varchar(30) NOT NULL CHECK (role IN ('SUPER_ADMIN','TENANT_ADMIN','RESP_CONFORMITE','CONTRIBUTEUR','LECTEUR')),
  locale varchar(5) NOT NULL DEFAULT 'fr',
  theme varchar(12) NOT NULL DEFAULT 'system',
  active boolean NOT NULL DEFAULT true,
  failed_attempts integer NOT NULL DEFAULT 0,
  locked_until timestamptz,
  last_login_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX app_user_tenant_idx ON app_user(tenant_id);

CREATE TABLE refresh_token (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token_hash varchar(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE password_reset_token (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  token_hash varchar(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  used_at timestamptz
);
CREATE TABLE department (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  code varchar(40) NOT NULL,
  name varchar(160) NOT NULL,
  UNIQUE(tenant_id, code)
);
CREATE TABLE stakeholder (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  name varchar(160) NOT NULL,
  type varchar(80) NOT NULL
);
CREATE TABLE theme (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(60) NOT NULL UNIQUE,
  label_fr varchar(160) NOT NULL,
  label_en varchar(160) NOT NULL,
  label_ar varchar(160) NOT NULL
);
CREATE TABLE standard (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code varchar(30) NOT NULL UNIQUE,
  label varchar(160) NOT NULL
);
CREATE TABLE law_text (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reference varchar(120) NOT NULL UNIQUE,
  type varchar(30) NOT NULL,
  title_fr varchar(500) NOT NULL,
  title_ar varchar(500),
  authority varchar(180),
  published_on date,
  official_gazette_ref varchar(120),
  status varchar(20) NOT NULL DEFAULT 'IN_FORCE' CHECK (status IN ('IN_FORCE','REPEALED','AMENDED')),
  source_url varchar(1000),
  summary text,
  full_text text,
  search_vector tsvector NOT NULL DEFAULT ''::tsvector,
  added_at timestamptz NOT NULL DEFAULT now(),
  created_by uuid REFERENCES app_user(id)
);
CREATE INDEX law_text_added_idx ON law_text(added_at DESC);
CREATE INDEX law_text_type_idx ON law_text(type);
CREATE INDEX law_text_search_idx ON law_text USING gin(search_vector);
CREATE INDEX law_text_title_trgm_idx ON law_text USING gin(title_fr gin_trgm_ops);
CREATE TABLE law_text_theme (
  law_text_id uuid NOT NULL REFERENCES law_text(id) ON DELETE CASCADE,
  theme_id uuid NOT NULL REFERENCES theme(id) ON DELETE CASCADE,
  PRIMARY KEY(law_text_id, theme_id)
);
CREATE TABLE law_text_standard (
  law_text_id uuid NOT NULL REFERENCES law_text(id) ON DELETE CASCADE,
  standard_id uuid NOT NULL REFERENCES standard(id) ON DELETE CASCADE,
  PRIMARY KEY(law_text_id, standard_id)
);
CREATE TABLE law_text_version (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  law_text_id uuid NOT NULL REFERENCES law_text(id) ON DELETE CASCADE,
  version_no integer NOT NULL,
  snapshot jsonb NOT NULL,
  changed_at timestamptz NOT NULL DEFAULT now(),
  changed_by uuid REFERENCES app_user(id),
  UNIQUE(law_text_id, version_no)
);
CREATE TABLE tenant_text (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  law_text_id uuid NOT NULL REFERENCES law_text(id),
  applicability varchar(20) NOT NULL DEFAULT 'TO_REVIEW' CHECK (applicability IN ('APPLICABLE','TO_REVIEW','INFORMATION')),
  applicability_reason text,
  conformity varchar(30) NOT NULL DEFAULT 'NOT_ANALYZED' CHECK (conformity IN ('NOT_ANALYZED','NON_COMPLIANT','PARTIALLY_COMPLIANT','CANDIDATE_COMPLIANT','COMPLIANT')),
  verified_on date,
  current_situation text,
  comment text,
  owner_id uuid REFERENCES app_user(id),
  next_review_on date,
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(tenant_id, law_text_id)
);
CREATE INDEX tenant_text_tenant_conformity_idx ON tenant_text(tenant_id, conformity);
CREATE INDEX tenant_text_tenant_applicability_idx ON tenant_text(tenant_id, applicability);
CREATE TABLE tenant_text_department (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_text_id uuid NOT NULL REFERENCES tenant_text(id) ON DELETE CASCADE,
  department_id uuid NOT NULL REFERENCES department(id) ON DELETE CASCADE,
  UNIQUE(tenant_text_id, department_id)
);
CREATE TABLE tenant_text_stakeholder (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_text_id uuid NOT NULL REFERENCES tenant_text(id) ON DELETE CASCADE,
  stakeholder_id uuid NOT NULL REFERENCES stakeholder(id) ON DELETE CASCADE,
  UNIQUE(tenant_text_id, stakeholder_id)
);
CREATE TABLE requirement (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_text_id uuid NOT NULL REFERENCES tenant_text(id) ON DELETE CASCADE,
  article_ref varchar(100),
  description text NOT NULL,
  status varchar(30) NOT NULL DEFAULT 'NOT_ANALYZED'
);
CREATE TABLE action (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  tenant_text_id uuid NOT NULL REFERENCES tenant_text(id) ON DELETE CASCADE,
  title varchar(300) NOT NULL,
  description text,
  responsible_id uuid REFERENCES app_user(id),
  responsible_phone varchar(40),
  priority varchar(20) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
  due_on date,
  status varchar(20) NOT NULL DEFAULT 'TO_DO' CHECK (status IN ('TO_DO','IN_PROGRESS','DONE')),
  done_on date,
  effectiveness varchar(20) NOT NULL DEFAULT 'UNDEFINED' CHECK (effectiveness IN ('UNDEFINED','EFFECTIVE','INEFFECTIVE')),
  effectiveness_comment text,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX action_tenant_due_idx ON action(tenant_id, due_on);
CREATE INDEX action_tenant_text_idx ON action(tenant_id, tenant_text_id);
CREATE TABLE evidence (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  tenant_text_id uuid REFERENCES tenant_text(id) ON DELETE CASCADE,
  action_id uuid REFERENCES action(id) ON DELETE CASCADE,
  filename varchar(255) NOT NULL,
  storage_key varchar(500) NOT NULL UNIQUE,
  mime varchar(120) NOT NULL,
  size bigint NOT NULL CHECK (size BETWEEN 1 AND 10485760),
  uploaded_by uuid NOT NULL REFERENCES app_user(id),
  uploaded_at timestamptz NOT NULL DEFAULT now(),
  CHECK (tenant_text_id IS NOT NULL OR action_id IS NOT NULL)
);
CREATE TABLE conformity_history (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  tenant_text_id uuid NOT NULL REFERENCES tenant_text(id) ON DELETE CASCADE,
  from_status varchar(30),
  to_status varchar(30) NOT NULL,
  changed_by uuid NOT NULL REFERENCES app_user(id),
  changed_at timestamptz NOT NULL DEFAULT now(),
  note text
);
CREATE TABLE compliance_snapshot (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid NOT NULL REFERENCES tenant(id) ON DELETE CASCADE,
  taken_on date NOT NULL DEFAULT current_date,
  score numeric(5,2) NOT NULL,
  counts jsonb NOT NULL DEFAULT '{}'::jsonb,
  UNIQUE(tenant_id, taken_on)
);
CREATE TABLE notification (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  type varchar(60) NOT NULL,
  payload jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  read_at timestamptz
);
CREATE TABLE audit_log (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id uuid REFERENCES tenant(id) ON DELETE CASCADE,
  user_id uuid NOT NULL REFERENCES app_user(id),
  entity varchar(80) NOT NULL,
  entity_id uuid,
  operation varchar(30) NOT NULL,
  diff jsonb NOT NULL DEFAULT '{}'::jsonb,
  at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_tenant_at_idx ON audit_log(tenant_id, at DESC);
CREATE TABLE saved_view (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
  scope varchar(60) NOT NULL,
  name varchar(140) NOT NULL,
  filters jsonb NOT NULL DEFAULT '{}'::jsonb
);
CREATE TABLE organization_settings (
  tenant_id uuid PRIMARY KEY REFERENCES tenant(id) ON DELETE CASCADE,
  organization_name varchar(180) NOT NULL,
  default_locale varchar(5) NOT NULL DEFAULT 'fr',
  updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE action_reminder (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  action_id uuid NOT NULL REFERENCES action(id) ON DELETE CASCADE,
  reminder_date date NOT NULL,
  reminder_type varchar(20) NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(action_id, reminder_date, reminder_type)
);

INSERT INTO tenant(id,name,slug,sector) VALUES
('10000000-0000-0000-0000-000000000001','Organisation Démonstration Nord','demo-nord','Industrie'),
('10000000-0000-0000-0000-000000000002','Organisation Démonstration Sud','demo-sud','Services');
INSERT INTO theme(code,label_fr,label_en,label_ar) VALUES
('HSE','Hygiène, sécurité et environnement','Health, safety and environment','الصحة والسلامة والبيئة'),
('ENV','Environnement','Environment','البيئة'),
('QUALITY','Qualité','Quality','الجودة'),
('ENERGY','Énergie','Energy','الطاقة');
INSERT INTO standard(code,label) VALUES
('ISO 14001','Management environnemental'),
('ISO 45001','Santé et sécurité au travail'),
('ISO 9001','Management de la qualité'),
('ISO 50001','Management de l’énergie'),
('ISO 22000','Sécurité des denrées alimentaires');

-- These synthetic records are explicitly labelled demonstrations, not legal advice or purported legislation.
INSERT INTO law_text(reference,type,title_fr,title_ar,authority,published_on,status,summary,full_text)
SELECT 'DEMO-' || lpad(n::text,4,'0'),
       (ARRAY['LAW','DECREE','ORDER','CIRCULAR','STANDARD'])[((n-1)%5)+1],
       'Texte réglementaire fictif de démonstration ' || lpad(n::text,4,'0'),
       'نص تنظيمي تجريبي افتراضي ' || lpad(n::text,4,'0'),
       'Jeu de démonstration Vigilex',
       date '2020-01-01' + ((n-1) % 2200),
       (ARRAY['IN_FORCE','AMENDED','REPEALED'])[((n-1)%3)+1],
       'Enregistrement synthétique destiné à tester les filtres, la recherche et la pagination. Référence fictive, sans valeur juridique.',
       'Donnée fictive de démonstration ' || n || '. Aucun contenu juridique officiel. Vérifier les sources réglementaires applicables avant toute décision de conformité.'
FROM generate_series(1,1500) AS n;
UPDATE law_text SET search_vector =
  setweight(to_tsvector('french', coalesce(title_fr,'') || ' ' || coalesce(summary,'')), 'A') ||
  setweight(to_tsvector('simple', coalesce(title_ar,'')), 'B') ||
  to_tsvector('simple', coalesce(reference,'') || ' ' || coalesce(full_text,''));

CREATE OR REPLACE FUNCTION law_text_search_vector_update() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    setweight(to_tsvector('french', coalesce(NEW.title_fr,'') || ' ' || coalesce(NEW.summary,'')), 'A') ||
    setweight(to_tsvector('simple', coalesce(NEW.title_ar,'')), 'B') ||
    to_tsvector('simple', coalesce(NEW.reference,'') || ' ' || coalesce(NEW.full_text,''));
  RETURN NEW;
END
$$ LANGUAGE plpgsql;
CREATE TRIGGER law_text_search_vector_trigger
  BEFORE INSERT OR UPDATE OF title_fr,title_ar,reference,summary,full_text ON law_text
  FOR EACH ROW EXECUTE FUNCTION law_text_search_vector_update();
