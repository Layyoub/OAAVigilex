/* © 2026 Ayoub — Tous droits réservés. */
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AppStateService, Locale } from '../../core/app-state.service';
import { I18nService } from '../../core/i18n.service';

@Component({
  imports: [FormsModule],
  selector: 'app-login',
  styleUrl: './login.component.scss',
  templateUrl: './login.component.html',
})
export class LoginComponent {
  readonly state = inject(AppStateService);
  readonly i18n = inject(I18nService);
  readonly email = signal('');
  readonly password = signal('');
  readonly error = signal('');
  readonly forgotMessage = signal('');
  readonly showPassword = signal(false);
  private readonly router = inject(Router);

  constructor() {
    document.documentElement.dataset['theme'] = this.state.theme();
    this.state.setLocale(this.state.locale());
    if (this.state.authenticated()) void this.router.navigateByUrl('/dashboard');
  }

  t(key: string): string {
    return this.i18n.t(key);
  }

  signIn(event: Event): void {
    event.preventDefault();
    if (!this.state.login(this.email(), this.password())) {
      this.error.set(this.state.locale() === 'en'
        ? 'Enter a valid email address and a password of at least 8 characters.'
        : this.state.locale() === 'ar'
          ? 'أدخل بريدًا إلكترونيًا صحيحًا وكلمة مرور لا تقل عن 8 أحرف.'
          : 'Saisissez une adresse e-mail valide et un mot de passe de 8 caractères minimum.');
      return;
    }
    this.error.set('');
    void this.router.navigateByUrl('/dashboard');
  }

  requestReset(): void {
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email())) {
      this.error.set(this.state.locale() === 'en' ? 'Enter your email address first.' : 'Saisissez d’abord votre adresse e-mail.');
      return;
    }
    this.error.set('');
    this.forgotMessage.set(this.state.locale() === 'en'
      ? 'If an account exists for this address, reset instructions will be sent.'
      : 'Si un compte est associé à cette adresse, les instructions seront envoyées.');
  }

  changeLocale(locale: Locale): void {
    this.state.setLocale(locale);
  }
}
