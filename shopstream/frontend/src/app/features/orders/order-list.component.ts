import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { OrderService } from '../../core/api/order.service';
import { errorMessage } from '../../core/errors';
import { Order } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

@Component({
  selector: 'app-order-list',
  imports: [CurrencyPipe, DatePipe, RouterLink, StatusBadgeComponent],
  templateUrl: './order-list.component.html',
})
export class OrderListComponent implements OnInit {
  private readonly orderService = inject(OrderService);

  protected readonly orders = signal<Order[] | null>(null);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.orderService.list().subscribe({
      next: (orders) => this.orders.set(orders),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }
}
