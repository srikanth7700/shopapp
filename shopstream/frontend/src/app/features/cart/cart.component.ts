import { CurrencyPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { NotificationService } from '../../core/api/notification.service';
import { OrderService } from '../../core/api/order.service';
import { AuthService } from '../../core/auth/auth.service';
import { CartService } from '../../core/cart/cart.service';
import { errorMessage } from '../../core/errors';
import { ProductImageComponent } from '../../shared/product-image.component';

@Component({
  selector: 'app-cart',
  imports: [CurrencyPipe, RouterLink, ProductImageComponent],
  templateUrl: './cart.component.html',
})
export class CartComponent {
  protected readonly cart = inject(CartService);
  protected readonly auth = inject(AuthService);
  private readonly orders = inject(OrderService);
  private readonly notifications = inject(NotificationService);
  private readonly router = inject(Router);

  protected readonly placing = signal(false);
  protected readonly error = signal<string | null>(null);

  protected checkout(): void {
    if (!this.auth.isLoggedIn()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: '/cart' } });
      return;
    }
    this.placing.set(true);
    this.error.set(null);
    const items = this.cart.items().map((item) => ({ productId: item.productId, quantity: item.quantity }));
    this.orders.place(items).subscribe({
      next: (order) => {
        this.cart.clear();
        this.notifications.refreshUnread().subscribe({ error: () => undefined });
        // The order starts as PENDING; the detail page shows the saga progressing live.
        this.router.navigate(['/orders', order.id]);
      },
      error: (err) => {
        this.error.set(errorMessage(err));
        this.placing.set(false);
      },
    });
  }
}
