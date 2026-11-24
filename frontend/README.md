# Vigilex frontend

Application Angular 22 standalone stricte, Angular Material, interface FR/EN/AR et navigation responsive.

## Développement

```powershell
npm ci
npm start
```

Ouvrir `http://localhost:4200`. L'interface de démonstration accepte une adresse e-mail valide et un mot de passe de huit caractères minimum. Elle utilise des données exemples locales; connecter l'authentification et les opérations persistantes aux API backend avant tout déploiement.

## Vérification

```powershell
npm test -- --watch=false
npm run build
npx playwright install chromium
npm run test:e2e
```

Le parcours Playwright couvre la connexion, la recherche et la sélection de textes, la création d'une action en Kanban et le changement de langue.
