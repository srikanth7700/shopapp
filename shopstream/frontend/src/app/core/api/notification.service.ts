import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE } from '../api-config';
import { AppNotification } from '../models';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/notifications`;

  /** Shown as a badge in the header. */
  readonly unread = signal(0);

  list(): Observable<AppNotification[]> {
    return this.http.get<AppNotification[]>(this.url);
  }

  refreshUnread(): Observable<{ count: number }> {
    return this.http
      .get<{ count: number }>(`${this.url}/unread-count`)
      .pipe(tap((result) => this.unread.set(result.count)));
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http
      .patch<AppNotification>(`${this.url}/${id}/read`, {})
      .pipe(tap(() => this.unread.update((n) => Math.max(0, n - 1))));
  }

  markAllRead(): Observable<{ updated: number }> {
    return this.http
      .patch<{ updated: number }>(`${this.url}/read-all`, {})
      .pipe(tap(() => this.unread.set(0)));
  }
}
