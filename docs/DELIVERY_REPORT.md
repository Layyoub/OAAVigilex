# Vigilex — rapport de livraison

## Livrables présents

- Monorepo backend Spring Boot, frontend Angular et infrastructure Docker Compose.
- Backend multi-tenant avec authentification, bibliothèque, ma base, analyse, actions, preuves, dashboard, rapports, notifications, audit et administration.
- Frontend premium clair/sombre, i18n FR/EN/AR, RTL, shell repliable et écrans fonctionnels.
- Logo SVG original, favicon, tokens de design, scripts Windows/Linux et licences propriétaires.

## Validation

- Angular production build: réussi.
- Tests Angular: 3/3 réussis.
- Backend Maven test: réussi, aucun échec.
- Testcontainers PostgreSQL: ignoré lorsque Docker Engine n'est pas démarré.
- Docker Desktop installé; le moteur doit être démarré avant `scripts/dev.ps1`.

## Points connus

- Les fonctionnalités avancées de rapports, import en lot et ingestion doivent être connectées aux sources réelles avant mise en production.
- Les données juridiques fournies sont fictives et délibérément non présentées comme officielles.
- Lighthouse, Playwright complet et capture visuelle finale nécessitent le moteur Docker démarré.
