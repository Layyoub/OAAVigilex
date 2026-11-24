/* © 2026 Ayoub — Tous droits réservés. */
import { expect, test } from '@playwright/test';

test('sign in, review the library, create an action, and switch locale', async ({ page }) => {
  await page.goto('/login');
  await page.getByLabel('Adresse e-mail').fill('ayoub@decoplast.tn');
  await page.getByRole('textbox', { name: 'Mot de passe' }).fill('Vigilex2026!');
  await page.getByRole('button', { name: 'Se connecter' }).click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByRole('heading', { name: /DECOPLAST/ })).toBeVisible();
  await page.getByRole('link', { name: 'Bibliothèque' }).click();
  await expect(page.getByRole('heading', { name: 'Bibliothèque réglementaire' })).toBeVisible();
  await page.getByRole('searchbox').fill('96-41');
  await page.getByLabel('Sélectionner Loi n° 96-41').check();
  await page.getByRole('button', { name: 'Ajouter à Mes textes' }).first().click();
  await expect(page.getByRole('status')).toContainText('Texte ajouté à Mes textes');

  await page.getByRole('link', { name: 'Plan d’actions' }).click();
  await page.getByRole('button', { name: 'Nouvelle action' }).click();
  await page.getByLabel('Titre de l’action').fill('Contrôler le plan de prévention');
  await page.getByLabel('Responsable').fill('Sarra Ben Salah');
  await page.getByLabel('Date d’échéance').fill('2026-12-15');
  await page.getByRole('button', { name: 'Créer l’action' }).click();
  await page.getByRole('button', { name: 'Kanban' }).click();
  await expect(page.getByText('Contrôler le plan de prévention')).toBeVisible();

  await page.getByRole('button', { name: 'English' }).click();
  await expect(page.locator('html')).toHaveAttribute('lang', 'en');
  await expect(page.getByRole('heading', { name: 'Action plan' })).toBeVisible();
});
