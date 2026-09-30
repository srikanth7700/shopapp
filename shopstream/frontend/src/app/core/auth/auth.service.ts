import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';

import { API_BASE } from '../api-config';
import { AuthResponse, User } from '../models';

interface Session {
  token: string;
  expiresAt: number;
  user: User;
}

const STORAGE_KEY = 'shopstream.session';

/**
 * Holds the logged-in user in a signal. Components read `auth.user()` and
 * Angular re-renders them automatically when it changes.
 *
 * Trade-off: the JWT is kept in localStorage so a page refresh keeps you
 * logged in. JavaScript can read localStorage, so an XSS bug could steal the
 * token. Many production apps use an httpOnly cookie instead.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly session = signal<Session | null>(loadSession());

  readonly user = computed(() => this.session()?.user ?? null);
  readonly isLoggedIn = computed(() => this.session() !== null);
  readonly isAdmin = computed(() => this.user()?.role === 'ADMIN');

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, { email, password })
      .pipe(tap((response) => this.startSession(response)));
  }

  register(fullName: string, email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/register`, { fullName, email, password })
      .pipe(tap((response) => this.startSession(response)));
  }

  /** Returns the JWT, or null if there is none or it has expired. */
  token(): string | null {
    const current = this.session();
    if (!current) {
      return null;
    }
    if (Date.now() >= current.expiresAt) {
      this.clearSession();
      return null;
    }
    return current.token;
  }

  logout(): void {
    this.clearSession();
    this.router.navigateByUrl('/');
  }

  private startSession(response: AuthResponse): void {
    const session: Session = {
      token: response.token,
      expiresAt: Date.now() + response.expiresInSeconds * 1000,
      user: response.user,
    };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
    this.session.set(session);
  }

  private clearSession(): void {
    localStorage.removeItem(STORAGE_KEY);
    this.session.set(null);
  }
}

function loadSession(): Session | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const session = JSON.parse(raw) as Session;
    return session.expiresAt > Date.now() ? session : null;
  } catch {
    return null;
  }
}
