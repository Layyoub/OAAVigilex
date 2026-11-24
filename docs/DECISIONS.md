# Décisions techniques

## 2026-11-24 — Emplacement du cahier des charges

- **Décision** : créer `docs/CDC.md` comme source de vérité de travail à partir du document remis.
- **Raison** : le cahier initial se trouvait à la racine du workspace ; l'emplacement demandé par le master prompt doit être disponible dès le démarrage.
- **Impact** : le document reste propriétaire, traçable et référencé par les règles d'agent.

## 2026-11-24 — Démarrage par phases

- **Décision** : initialiser uniquement les règles et décisions tant que `PHASE 0` n'a pas été demandé.
- **Raison** : la consigne explicite du prompt maître impose d'attendre le prompt de phase après cette première action.
- **Impact** : aucun projet applicatif ou dépendance n'est installé avant la phase demandée.

## 2026-11-24 — Application frontend Vigilex

- **Décision** : initialiser `frontend/` avec Angular 22 standalone strict, Angular Material, une navigation protégée et des écrans autonomes par route.
- **Raison** : la demande couvre l'expérience web complète; la version Angular 22.3.0 est la version stable publiée lors de la réalisation.
- **Impact** : les écrans sont utilisables avec un jeu de données de démonstration isolé côté navigateur; l'authentification et les opérations persistantes devront être raccordées aux API backend avant un déploiement.
- **Décision** : privilégier les graphes SVG/CSS natifs, une identité visuelle SVG originale et un export CSV imprimable plutôt qu'une dépendance de visualisation ou un générateur de document supplémentaire.
- **Raison** : conserver une interface légère et éviter d'intégrer des bibliothèques inutiles pour les vues de ce premier frontend.
