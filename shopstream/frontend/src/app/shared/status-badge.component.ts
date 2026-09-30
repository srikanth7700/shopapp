import { Component, computed, input } from '@angular/core';

import { OrderStatus } from '../core/models';

const LABELS: Record<OrderStatus, string> = {
  PENDING: 'Pending',
  INVENTORY_RESERVED: 'Stock reserved',
  CONFIRMED: 'Confirmed',
  CANCELLED: 'Cancelled',
};

@Component({
  selector: 'app-status-badge',
  template: `<span [class]="'badge badge--' + status().toLowerCase()">{{ label() }}</span>`,
})
export class StatusBadgeComponent {
  readonly status = input.required<OrderStatus>();
  protected readonly label = computed(() => LABELS[this.status()] ?? this.status());
}
