# Vigilex — rapport de livraison

## Livrables présents

- Monorepo backend Spring Boot, frontend Angular et infrastructure Docker Compose.
- Backend multi-tenant avec authentification, bibliothèque, ma base, analyse, actions, preuves, dashboard, rapports, notifications, audit et administration. Tenant de démonstration DECOPLAST (plasturgie, Tunisie) plus un second tenant pour les tests d’isolation.
- Frontend premium clair/sombre, i18n FR/EN/AR, RTL, shell repliable et écrans fonctionnels, avec références tunisiennes d’exemple et typographie agrandie.
- Logo SVG original, favicon, tokens de design, scripts Windows/Linux et licences propriétaires.

## Validation

- Angular production build: réussi.
- Tests Angular: 4/4 réussis.
- Backend Maven test: réussi, aucun échec.
- Testcontainers PostgreSQL: ignoré lorsque Docker Engine n'est pas démarré.
- Docker Desktop installé et WSL activé; redémarrage Windows requis avant que le moteur Docker puisse démarrer, puis lancer `scripts/dev.ps1`.

## Points connus

- Les fonctionnalités avancées de rapports, import en lot et ingestion doivent être connectées aux sources réelles avant mise en production.
- Les données juridiques de démonstration incluent quatre références tunisiennes, à vérifier au JORT et à qualifier pour le site DECOPLAST avant toute décision de conformité. Les 1 500 autres enregistrements génériques restent explicitement fictifs.
- Le frontend reste un mode démonstration local : il n’est pas encore raccordé à l’authentification et aux données persistées du backend.
- Lighthouse, Playwright complet et capture visuelle finale nécessitent le moteur Docker démarré.
