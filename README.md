# Vigilex

© 2026 Ayoub — Tous droits réservés

Vigilex est une plateforme SaaS de veille réglementaire et de pilotage HSE/QSE.

## Démarrage rapide

1. Ouvrir PowerShell à la racine.
2. Exécuter `.\scripts\setup.ps1`.
3. Copier `infra\.env.example` vers `infra\.env` et remplacer les secrets.
4. Exécuter `.\scripts\dev.ps1`.
5. Ouvrir `http://localhost` puis `http://localhost:8025` pour Mailpit.

## Développement local

```powershell
cd frontend
npm install
npm start

cd ..\backend
.\mvnw.cmd spring-boot:run
```

## Tests

```powershell
cd frontend
npm test -- --watch=false
npm run build

cd ..\backend
.\mvnw.cmd test
```

## Vérifications Docker

```powershell
& "C:\Program Files\Docker\Docker\Docker Desktop.exe"
docker compose --project-directory infra -f infra\docker-compose.yml up -d --build
```
