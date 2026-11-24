# Cahier des charges — VIGILEX

**Plateforme SaaS de veille réglementaire et de pilotage de la conformité HSE / QSE**

| | |
|---|---|
| Version | 1.0 |
| Date | 24/11/2026 |
| Propriétaire | Ayoub (tous droits réservés) |
| Stack | Angular · Spring Boot · PostgreSQL · Docker |

---

## 1. Contexte et objectifs

Les organismes soumis à des exigences légales (HSE, environnement, qualité, énergie, sécurité alimentaire) doivent **identifier** les textes qui les concernent, **évaluer** leur conformité, **planifier** les actions correctives et **prouver** leur conformité lors des audits (ISO 14001, 45001, 9001, 50001, 22000).

**Vigilex** centralise une base commune de textes juridiques (Tunisie en priorité), laisse chaque client constituer sa propre base applicable, mesure sa conformité et pilote ses plans d'action.

**Objectifs mesurables**
- Un client opérationnel en moins de 15 minutes (création du compte, sélection des textes, première analyse).
- Un registre de conformité exportable en PDF/Excel, prêt pour audit, en un clic.
- Un tableau de bord lisible en moins de 10 secondes par un directeur.

## 2. Positionnement et différenciation

Vigilex couvre le périmètre fonctionnel habituel d'un outil de veille réglementaire (base commune, base client, analyse, plan d'actions, tableau de bord) et se différencie par :

1. **Un score de conformité global** (jauge) et par département, avec tendance dans le temps.
2. **Un calendrier réglementaire** (échéances d'actions, revues périodiques, nouveaux textes).
3. **Un registre de preuves** : pièces jointes liées aux textes et aux actions.
4. **Un journal d'audit** complet (qui a changé quoi, quand).
5. **Une vue lecture + analyse** côte à côte sur chaque texte.
6. **Une palette de commandes** (Ctrl+K) et une recherche plein texte multilingue (FR/AR).
7. **Un design moderne** : mode sombre, RTL arabe, accessibilité WCAG AA.
8. **Des rapports d'audit** générés automatiquement (PDF/Excel).

> Vigilex est une création originale : aucun code, texte, logo, icône ou visuel d'un produit existant n'est repris.

## 3. Périmètre fonctionnel

### M1 — Authentification et multi-tenant
- Connexion e-mail + mot de passe, JWT court (15 min) + refresh token en cookie `httpOnly`.
- Mot de passe oublié (lien à durée limitée), changement de mot de passe, verrouillage après 5 échecs.
- Multi-tenant : un schéma partagé, colonne `tenant_id` sur toutes les tables client, filtrage automatique et vérifié par des tests d'isolation.
- Profil utilisateur : langue (FR/EN/AR), thème (clair/sombre/système).

### M2 — Base générale des textes (« Bibliothèque »)
- Fiche texte : référence (ID loi), type (loi, décret, décret-loi, arrêté, circulaire, communiqué, norme), titre FR/AR, autorité émettrice, date de publication, référence JORT, statut (en vigueur / abrogé / modifié), thèmes, normes ISO liées, résumé par article, texte intégral, lien source, date d'ajout.
- Badge « Nouveau » pendant 14 jours après l'ajout.
- Recherche plein texte (français et arabe) avec surlignage des résultats.
- Filtres à facettes : type, thème, année, autorité, statut, norme ISO, « déjà dans ma base ».
- Tri, pagination serveur, vues enregistrées par utilisateur.
- Sélection multiple et action **« Ajouter à ma base »**.

### M3 — Ma base (« Mes textes »)
- Un texte ajouté devient un `tenant_text` propre au client.
- Applicabilité : **Applicable**, **À vérifier**, **Pour info**, avec justification.
- Association aux **départements** et **parties prenantes**.
- Responsable du texte, date de prochaine revue.
- Onglets et compteurs par applicabilité et par statut de conformité.

### M4 — Analyse de conformité
- Statuts : **Non analysé → Non conforme → Partiellement conforme → Candidat conforme → Conforme**.
- Champs : date de vérification, situation actuelle, commentaire, exigences clés extraites (tableau article / exigence / statut).
- **Règles de gestion**
  - « Candidat conforme » : toutes les actions liées sont réalisées.
  - « Conforme » : candidat conforme + toutes les actions jugées efficaces + validation par un Responsable conformité.
  - Toute nouvelle action non réalisée sur un texte « Conforme » le repasse en « Partiellement conforme » (avec notification).
- Bouton **Aide** : panneau latéral expliquant comment analyser un texte (guide statique et modifiable par l'admin).
- Historique des changements de statut (horodaté, avec auteur).

### M5 — Plan d'actions
- Action : titre, description, texte lié, responsable (utilisateur), téléphone, priorité, échéance, statut (à faire / en cours / réalisée), date de réalisation, **efficacité** (non définie / efficace / non efficace) + commentaire.
- Vues : tableau filtrable, **Kanban**, **calendrier**.
- Indicateurs : en cours, réalisées, non réalisées (en retard), réalisées et efficaces, réalisées et non efficaces, efficacité non définie.
- « Bilan des actions » : synthèse par département, par responsable, par texte.
- Rappels e-mail et in-app (J-7, J-1, en retard).

### M6 — Preuves
- Pièces jointes (PDF, images, Office) sur un texte ou une action, 10 Mo max, liste blanche de types MIME, nom assaini, stockage derrière une interface `StorageService` (disque, S3/MinIO plus tard).
- Aperçu et téléchargement sécurisés (contrôle d'accès par tenant).

### M7 — Tableau de bord
- Jauge **Score de conformité** (pondération : Conforme = 100 %, Candidat = 75 %, Partiel = 50 %, Non conforme = 0 %, Non analysé exclu mais signalé).
- Cartes KPI avec tendance : textes en base, non analysés, non conformes, partiels, conformes ; actions en retard.
- Graphiques : donut par statut, barres par département, évolution mensuelle du score, comparaison « Ma base vs Base générale ».
- Listes : échéances à venir, derniers textes ajoutés, actions en retard.
- Filtre par période et par département.

### M8 — Rapports et exports
- Registre de conformité (PDF et Excel) : texte, applicabilité, statut, date de vérification, actions, preuves.
- Rapport d'audit par norme ISO et par département.
- Export CSV de toute liste filtrée.

### M9 — Administration (SUPER_ADMIN)
- Gestion des textes : création, édition, import CSV/JSON, import par lot, historique des versions, abrogation.
- Gestion des thèmes, normes, clients (tenants), utilisateurs et abonnements (statut actif/suspendu).
- **Ingestion planifiée** (`@Scheduled`) via une interface `SourceAdapter` : un adaptateur « import manuel/CSV » livré, un squelette d'adaptateur « source web » à brancher après vérification des conditions d'utilisation de la source.
- Alertes : lorsqu'un nouveau texte correspond aux thèmes d'un client, notification aux administrateurs du client.

### M10 — Administration client (TENANT_ADMIN)
- Utilisateurs, rôles, départements, parties prenantes, logo et nom de l'organisme, langue par défaut.

### Transverses
- **Notifications** : cloche in-app, e-mails (Spring Mail), préférences par utilisateur.
- **Journal d'audit** consultable par TENANT_ADMIN.
- **Palette de commandes** (Ctrl+K) : navigation et recherche globale.
- **i18n** : FR (défaut), EN, AR avec bascule RTL.

## 4. Rôles et droits

| Fonction | SUPER_ADMIN | TENANT_ADMIN | RESP_CONFORMITE | CONTRIBUTEUR | LECTEUR |
|---|:-:|:-:|:-:|:-:|:-:|
| Gérer la base générale | ✔ | | | | |
| Gérer clients et abonnements | ✔ | | | | |
| Gérer utilisateurs du client | | ✔ | | | |
| Ajouter des textes à Ma base | | ✔ | ✔ | | |
| Analyser / changer le statut | | ✔ | ✔ | | |
| Valider « Conforme » | | ✔ | ✔ | | |
| Créer / assigner des actions | | ✔ | ✔ | | |
| Mettre à jour ses actions | | ✔ | ✔ | ✔ (siennes) | |
| Joindre des preuves | | ✔ | ✔ | ✔ | |
| Consulter, exporter | | ✔ | ✔ | ✔ | ✔ |
| Journal d'audit | | ✔ | | | |

## 5. Modèle de données (PostgreSQL)

| Table | Colonnes principales |
|---|---|
| `tenant` | id, name, slug, logo_path, sector, default_locale, status, created_at |
| `app_user` | id, tenant_id (null pour SUPER_ADMIN), email (unique), password_hash, full_name, phone, role, locale, theme, active, failed_attempts, locked_until, last_login_at |
| `refresh_token` | id, user_id, token_hash, expires_at, revoked_at |
| `department` | id, tenant_id, code, name |
| `stakeholder` | id, tenant_id, name, type |
| `theme` | id, code, label_fr, label_en, label_ar |
| `standard` | id, code (ISO 14001…), label |
| `law_text` | id, reference, type, title_fr, title_ar, authority, published_on, official_gazette_ref, status, source_url, summary, full_text, search_vector (tsvector), added_at, created_by |
| `law_text_theme` / `law_text_standard` | law_text_id, theme_id / standard_id |
| `law_text_version` | id, law_text_id, version_no, snapshot (jsonb), changed_at, changed_by |
| `tenant_text` | id, tenant_id, law_text_id, applicability, applicability_reason, conformity, verified_on, current_situation, comment, owner_id, next_review_on, updated_at — **unique(tenant_id, law_text_id)** |
| `tenant_text_department` / `tenant_text_stakeholder` | tenant_text_id, department_id / stakeholder_id |
| `requirement` | id, tenant_text_id, article_ref, description, status |
| `action` | id, tenant_id, tenant_text_id, title, description, responsible_id, responsible_phone, priority, due_on, status, done_on, effectiveness, effectiveness_comment |
| `evidence` | id, tenant_id, tenant_text_id, action_id, filename, storage_key, mime, size, uploaded_by, uploaded_at |
| `conformity_history` | id, tenant_id, tenant_text_id, from_status, to_status, changed_by, changed_at, note |
| `compliance_snapshot` | id, tenant_id, taken_on, score, counts (jsonb) — alimenté chaque nuit |
| `notification` | id, user_id, type, payload (jsonb), created_at, read_at |
| `audit_log` | id, tenant_id, user_id, entity, entity_id, operation, diff (jsonb), at |
| `saved_view` | id, user_id, scope, name, filters (jsonb) |

Index : GIN sur `search_vector` (configuration `french` + `simple` pour l'arabe, extension `unaccent`, `pg_trgm`), index composites `(tenant_id, conformity)`, `(tenant_id, due_on)`, `(tenant_id, applicability)`.

## 6. Architecture technique

```
vigilex/
├─ backend/               Spring Boot 3 (Java 21), Maven
│  └─ src/main/java/tn/vigilex/
│     ├─ config/  security/  tenant/  common/
│     ├─ auth/  user/  library/  basetext/  analysis/
│     ├─ action/  evidence/  dashboard/  report/
│     ├─ ingestion/  notification/  audit/  admin/
│     └─ (controller · service · repository · dto · mapper par module)
├─ frontend/              Angular (standalone components, signals), SCSS
│  └─ src/app/{core,shared,layout,features/*}
├─ infra/                 docker-compose.yml, nginx.conf, .env.example
├─ scripts/               setup.ps1, setup.sh, dev.ps1, seed
└─ docs/                  CDC, ARCHITECTURE.md, API.md, DECISIONS.md, THIRD_PARTY_LICENSES.md
```

**Backend** : Spring Web, Spring Security (JWT), Spring Data JPA/Hibernate (filtre `tenantFilter`), Validation, Flyway, MapStruct, Springdoc OpenAPI, Spring Mail, Apache POI (Excel), Apache PDFBox (PDF), Jsoup, Bucket4j (limitation de débit), Testcontainers + JUnit 5 + Mockito.
**Frontend** : Angular + Angular Material (thème personnalisé), Chart.js via ng2-charts, `@ngx-translate` ou i18n natif, Angular CDK (drag & drop Kanban, overlay), `@fontsource` (polices auto-hébergées), icônes Lucide ou Material Symbols auto-hébergées. Tests : Jest ou Karma + Playwright (parcours critiques).
**Données** : PostgreSQL 16. **Déploiement** : Docker Compose (Nginx, Spring, Postgres, Mailpit en dev).

## 7. API REST (extrait, préfixe `/api/v1`)

| Domaine | Endpoints |
|---|---|
| Auth | `POST /auth/login` · `/auth/refresh` · `/auth/logout` · `/auth/forgot-password` · `/auth/reset-password` |
| Bibliothèque | `GET /library/texts` (q, filtres, page, tri) · `GET /library/texts/{id}` |
| Ma base | `POST /my-texts` (ajout par lot) · `GET /my-texts` · `GET/PATCH /my-texts/{id}` · `PATCH /my-texts/{id}/conformity` · `GET /my-texts/{id}/history` |
| Actions | `GET/POST /actions` · `GET/PATCH/DELETE /actions/{id}` · `GET /actions/summary` |
| Preuves | `POST /evidence` · `GET /evidence/{id}/download` · `DELETE /evidence/{id}` |
| Tableau de bord | `GET /dashboard/kpis` · `/dashboard/score-trend` · `/dashboard/by-department` |
| Rapports | `GET /reports/register.pdf` · `/reports/register.xlsx` · `/reports/audit.pdf` |
| Admin | `/admin/texts` (CRUD, import) · `/admin/tenants` · `/admin/users` · `/admin/themes` |
| Client | `/org/users` · `/org/departments` · `/org/stakeholders` · `/org/settings` |
| Divers | `GET /notifications` · `PATCH /notifications/{id}/read` · `GET /audit-log` |

Format d'erreur unique (RFC 7807 `application/problem+json`), pagination `{content, page, size, total}`.

## 8. Exigences non fonctionnelles

- **Sécurité** : OWASP Top 10, hachage Argon2 ou BCrypt (coût ≥ 12), CORS restreint, en-têtes de sécurité, limitation de débit sur l'authentification, validation de toutes les entrées, requêtes paramétrées, isolation tenant testée automatiquement, secrets via variables d'environnement, aucune donnée sensible dans les logs.
- **Données personnelles** : minimisation, export et suppression sur demande, conformité à la loi organique n° 2004-63 (Tunisie) ; politique de rétention des journaux.
- **Performance** : liste de 5 000 textes paginée en moins de 400 ms (p95), recherche en moins de 500 ms, tableau de bord en moins de 1 s.
- **Accessibilité** : WCAG 2.1 AA (contrastes, focus visible, navigation clavier, ARIA, `prefers-reduced-motion`).
- **Responsive** : bureau, tablette, mobile (les écrans de consultation et de mise à jour d'actions doivent être utilisables sur mobile).
- **Qualité** : couverture ≥ 70 % sur les services backend, tests d'isolation tenant, tests E2E des parcours critiques, CI locale reproductible.
- **Observabilité** : logs structurés, endpoint de santé, métriques Actuator.
- **Sauvegardes** : script `pg_dump` planifiable et procédure de restauration documentée.

## 9. Design system et expérience utilisateur

**Principes** : clair, calme, dense sans être chargé, hiérarchie visuelle forte, un seul accent de couleur, états de conformité toujours identifiables.

**Identité**
- Nom : Vigilex. Signature : *« Voyez ce qui change. Prouvez que vous êtes conforme. »*
- Logo SVG original : bouclier stylisé formé de deux arcs avec une coche intégrée ; version complète, icône seule, version monochrome.

**Jetons de design**

| Jeton | Clair | Sombre |
|---|---|---|
| Fond application | `#F6F8FB` | `#0B1220` |
| Surface (cartes) | `#FFFFFF` | `#111A2C` |
| Bordure | `#E4E9F1` | `#223049` |
| Texte principal | `#0F1B2D` | `#E8EEF8` |
| Texte secondaire | `#5B6B82` | `#9AA9C0` |
| Primaire (marine) | `#0B2A4A` | `#7FB4FF` |
| Accent (turquoise) | `#0FA3A3` | `#2DD4C4` |
| Conforme | `#16A34A` | `#4ADE80` |
| Candidat conforme | `#2563EB` | `#60A5FA` |
| Partiellement conforme | `#D97706` | `#FBBF24` |
| Non conforme | `#DC2626` | `#F87171` |
| Non analysé | `#64748B` | `#94A3B8` |

- **Typographie** : Inter (interface), Plus Jakarta Sans (titres), Cairo (arabe), chiffres tabulaires pour les KPI. Polices auto-hébergées.
- **Grille et formes** : base 8 px, rayon 12 px (cartes) et 8 px (champs), ombres douces à 3 niveaux, icônes 20 px au trait de 1,75.
- **Mouvement** : transitions de 150 à 200 ms, squelettes de chargement, compteurs animés sur les KPI, respect de `prefers-reduced-motion`.

**Structure de l'application**
- Barre latérale repliable (icônes + libellés, compteurs sous forme de pastilles discrètes), en-tête avec recherche globale (Ctrl+K), cloche, sélecteur de langue, thème, menu utilisateur.
- **Connexion** : écran partagé, panneau de marque à gauche, formulaire à droite.
- **Tableau de bord** : bandeau d'accueil, jauge de score, cartes KPI avec tendance, donut par statut, barres par département, courbe du score, échéances à venir, derniers textes.
- **Bibliothèque** : tableau dense, barre de filtres à puces, vues enregistrées, sélection multiple avec barre d'action flottante, panneau latéral d'aperçu.
- **Détail d'un texte** : deux colonnes ; à gauche lecture (navigation par article, résumé, texte intégral) ; à droite panneau d'analyse collant (stepper de conformité, exigences, actions, preuves, historique).
- **Actions** : bascule tableau / Kanban / calendrier, pastilles de priorité et d'échéance (rouge si en retard).
- **États vides** illustrés (SVG originaux), messages d'erreur utiles, confirmations par toast avec « Annuler ».
- **Impression et PDF** : mise en page soignée avec logo du client.

## 10. Propriété intellectuelle et licences

1. **Titularité** : l'intégralité des livrables (code source, base de données, scripts, documentation, identité visuelle, logo, textes, illustrations) est la **propriété exclusive d'Ayoub**, tous droits réservés. Chaque fichier source porte l'en-tête `© 2026 Ayoub — Tous droits réservés`, et un fichier `LICENSE` propriétaire est fourni à la racine.
2. **Originalité** : interdiction de copier ou de reproduire du code, des textes, des visuels, des logos, des noms de marque ou des bases de données d'un produit tiers, y compris HSE Conform. Les captures d'écran de référence servent uniquement à comprendre les fonctionnalités.
3. **Dépendances** : uniquement des licences permissives (MIT, Apache-2.0, BSD, ISC, licence PostgreSQL, OFL pour les polices). **Interdits** : GPL, AGPL, SSPL, licences « non commerciales » ou à clause de partage à l'identique. Un fichier `THIRD_PARTY_LICENSES.md` liste chaque dépendance, sa version et sa licence, et un contrôle automatique échoue si une licence interdite apparaît.
4. **Aucun service imposé** : aucune télémétrie, aucune clé d'API tierce obligatoire, aucun compte externe requis pour faire fonctionner l'application.
5. **Données juridiques** : les textes de loi sont des documents publics, mais les conditions de réutilisation de chaque source (ex. publication officielle) doivent être vérifiées avant toute ingestion automatique ; la source de chaque texte est conservée.
6. **Cession** : la cession des droits entre Ayoub et tout tiers (client, prestataire) se fait par contrat écrit, indépendamment de ce cahier des charges.

## 11. Environnement et installation

Poste cible : **Windows 11** (PowerShell), compatible Linux/macOS via `setup.sh`.

| Prérequis | Version | Installation |
|---|---|---|
| JDK | 21 (Temurin) | `winget install EclipseAdoptium.Temurin.21.JDK` |
| Node.js | 20 LTS ou plus | `winget install OpenJS.NodeJS.LTS` |
| Git | récent | `winget install Git.Git` |
| Docker Desktop **ou** PostgreSQL | 16 | `winget install Docker.DockerDesktop` / `winget install PostgreSQL.PostgreSQL.16` |
| Maven | via Maven Wrapper (`mvnw`) | aucun téléchargement séparé |
| Angular CLI | local au projet | `npx` (aucune installation globale) |

L'agent vérifie chaque prérequis, installe ceux qui manquent, rafraîchit le `PATH`, et consigne le résultat dans `docs/SETUP_REPORT.md`. Les scripts `scripts/setup.ps1` et `scripts/setup.sh` sont **idempotents**.

## 12. Plan de livraison et critères d'acceptation

| Phase | Contenu | Critères d'acceptation |
|---|---|---|
| 0 | Environnement, squelette, Docker, CI locale, design tokens | `setup.ps1` puis `dev.ps1` lancent tout ; build vert |
| 1 | M1 Auth + multi-tenant + rôles | Tests d'isolation tenant verts ; connexion / refresh / verrouillage OK |
| 2 | M2 Bibliothèque + recherche + import admin | Recherche FR/AR, filtres, 1 500 textes de démonstration |
| 3 | M3 Ma base + applicabilité + départements | Ajout par lot, onglets et compteurs corrects |
| 4 | M4 Analyse + règles de statut + historique | Règles « Candidat / Conforme » testées |
| 5 | M5 Actions (tableau, Kanban, calendrier) + rappels | KPI d'actions exacts, rappels envoyés (Mailpit) |
| 6 | M6 Preuves + M8 Rapports | PDF et Excel générés et ouverts correctement |
| 7 | M7 Tableau de bord + snapshots | Score cohérent avec les données, graphiques fluides |
| 8 | M9 Ingestion + alertes + audit + notifications | Import CSV, alerte reçue, journal complet |
| 9 | Finition design, i18n/RTL, accessibilité, performance | Audit Lighthouse ≥ 90 (accessibilité, bonnes pratiques) |
| 10 | Tests E2E, documentation, audit de licences et de sécurité | Rapports générés, README complet |

## 13. Livrables

- Code source complet (backend, frontend, infra, scripts), dépôt Git avec historique propre.
- Jeu de données de démonstration (2 clients, 1 500 textes, actions, preuves fictives).
- Documentation : README, ARCHITECTURE, API (OpenAPI), DECISIONS, SETUP_REPORT, THIRD_PARTY_LICENSES, guide utilisateur, guide d'administration.
- Maquettes et système de design documentés (`docs/DESIGN.md`).
- Scripts de sauvegarde et de restauration.
- Jeu de captures d'écran de présentation (clair / sombre / arabe RTL).

## 14. Hors périmètre (évolutions)

- Résumé automatique des textes par IA (module optionnel, désactivé par défaut, derrière une interface).
- Application mobile native, SSO (SAML / OIDC), paiement en ligne, intégration directe de sources officielles après validation juridique.
