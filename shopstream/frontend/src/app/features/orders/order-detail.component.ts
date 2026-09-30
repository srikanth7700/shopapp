import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, DestroyRef, OnInit, computed, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { switchMap, takeWhile, timer } from 'rxjs';

import { OrderService } from '../../core/api/order.service';
import { errorMessage } from '../../core/errors';
import { Order, OrderStatus, isFinalStatus } from '../../core/models';
import { StatusBadgeComponent } from '../../shared/status-badge.component';

type StepState = 'done' | 'current' | 'failed' | 'todo';

interface Step {
  label: string;
  service: string;
  state: StepState;
}

/**
 * Polls the order every second until it reaches a final status, so you can
 * watch the Kafka saga move it through PENDING -> INVENTORY_RESERVED -> CONFIRMED
 * (or CANCELLED).
 */
@Component({
  selector: 'app-order-detail',
  imports: [CurrencyPipe, DatePipe, RouterLink, StatusBadgeComponent],
  templateUrl: './order-detail.component.html',
})
export class OrderDetailComponent implements OnInit {
  readonly id = input.required<string>();

  private readonly orderService = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly order = signal<Order | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly polling = computed(() => {
    const order = this.order();
    return order !== null && !isFinalStatus(order.status);
  });

  protected readonly steps = computed<Step[]>(() => {
    const order = this.order();
    return order ? buildSteps(order) : [];
  });

  ngOnInit(): void {
    const orderId = Number(this.id());
    timer(0, 1000)
      .pipe(
        switchMap(() => this.orderService.get(orderId)),
        // Keep polling until the status is final (inclusive = also emit that last value).
        takeWhile((order) => !isFinalStatus(order.status), true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (order) => this.order.set(order),
        error: (err) => this.error.set(errorMessage(err)),
      });
  }
}

function buildSteps(order: Order): Step[] {
  const reached = (status: OrderStatus) => order.history.some((h) => h.status === status);
  const cancelled = order.status === 'CANCELLED';
  const stockReserved = reached('INVENTORY_RESERVED') || order.status === 'CONFIRMED';

  const stockState: StepState = stockReserved ? 'done' : cancelled ? 'failed' : 'current';
  const paymentState: StepState = order.status === 'CONFIRMED'
    ? 'done'
    : cancelled
      ? (stockReserved ? 'failed' : 'todo')
      : stockReserved ? 'current' : 'todo';
  const confirmState: StepState = order.status === 'CONFIRMED' ? 'done' : cancelled ? 'failed' : 'todo';

  return [
    { label: 'Order placed', service: 'order-service', state: 'done' },
    { label: 'Stock reserved', service: 'inventory-service', state: stockState },
    { label: 'Payment', service: 'payment-service', state: paymentState },
    { label: 'Confirmed', service: 'order-service', state: confirmState },
  ];
}
