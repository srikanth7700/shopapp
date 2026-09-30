import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { NotificationService } from '../../core/api/notification.service';
import { errorMessage } from '../../core/errors';
import { AppNotification } from '../../core/models';

@Component({
  selector: 'app-notification-list',
  imports: [DatePipe, RouterLink],
  templateUrl: './notification-list.component.html',
})
export class NotificationListComponent implements OnInit {
  private readonly notificationService = inject(NotificationService);

  protected readonly notifications = signal<AppNotification[] | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.notificationService.list().subscribe({
      next: (list) => this.notifications.set(list),
      error: (err) => this.error.set(errorMessage(err)),
    });
    this.notificationService.refreshUnread().subscribe({ error: () => undefined });
  }

  protected markRead(notification: AppNotification): void {
    if (notification.read) {
      return;
    }
    this.notificationService.markRead(notification.id).subscribe((updated) =>
      this.notifications.update((list) => list?.map((n) => (n.id === updated.id ? updated : n)) ?? null),
    );
  }

  protected markAllRead(): void {
    this.notificationService.markAllRead().subscribe(() =>
      this.notifications.update((list) => list?.map((n) => ({ ...n, read: true })) ?? null),
    );
  }
}
