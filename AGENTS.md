# Vigilex — règles d'agent

## Source de vérité

Lire [docs/CDC.md](docs/CDC.md) avant toute implémentation. Le cahier des charges complet doit être respecté sans dévier de la stack, du modèle de données ou des règles métier.

## Mode de travail

- Avancer phase par phase et commencer une nouvelle phase uniquement sur demande explicite.
- Sans choix imposé, prendre le défaut le plus raisonnable et l'archiver dans `docs/DECISIONS.md`.
- Tout livrable doit compiler, démarrer et être testé.
- Aucun TODO, placeholder, fonctionnalité simulée ou code commenté dans une fonctionnalité livrée.
- Commits Conventional Commits après les étapes significatives.

## Stack

- Backend : Java 21, Spring Boot 3, Maven Wrapper, PostgreSQL 16.
- Frontend : Angular stable en mode strict, composants standalone, Angular Material.
- Infrastructure : Docker Compose avec PostgreSQL, backend, nginx et Mailpit.

## Qualité

- Clean architecture par module : controller, service, repository, DTO et mapper.
- Aucun métier dans les controllers.
- Validation serveur, erreurs RFC 7807, sécurité OWASP Top 10.
- JWT d'accès 15 minutes et refresh en cookie `httpOnly`.
- Isolation tenant obligatoire, testée sur les repositories et endpoints.
- FR par défaut, EN et AR avec RTL.
- Accessibilité WCAG 2.1 AA et responsive 360 px à bureau.
- Tests backend, Testcontainers PostgreSQL et Playwright pour les parcours critiques.

## Propriété et licences

- Tous les livrables appartiennent exclusivement à Ayoub.
- En-tête obligatoire : `© 2026 Ayoub — Tous droits réservés`.
- Aucun contenu tiers copié ou imité.
- Licences autorisées uniquement : MIT, Apache-2.0, BSD, ISC, licence PostgreSQL et OFL.
