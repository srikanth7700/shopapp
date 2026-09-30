import { Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { catchError, filter, of, switchMap, timer } from 'rxjs';

import { NotificationService } from './core/api/notification.service';
import { AuthService } from './core/auth/auth.service';
import { CartService } from './core/cart/cart.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
})
export class AppComponent {
  protected readonly auth = inject(AuthService);
  protected readonly cart = inject(CartService);
  protected readonly notifications = inject(NotificationService);

  constructor() {
    // Refresh the unread-notifications badge every 10 seconds while logged in.
    timer(0, 10_000)
      .pipe(
        filter(() => this.auth.isLoggedIn()),
        switchMap(() => this.notifications.refreshUnread().pipe(catchError(() => of(null)))),
        takeUntilDestroyed(),
      )
      .subscribe();
  }
}
