import { Injectable, computed, effect, signal } from '@angular/core';

import { Product } from '../models';

export interface CartItem {
  productId: number;
  name: string;
  category: string;
  price: number;
  quantity: number;
}

const STORAGE_KEY = 'shopstream.cart';
export const MAX_QUANTITY = 99;

/**
 * The cart lives only in the browser (signals + localStorage). The server
 * never sees it until checkout, and at checkout it recalculates every price
 * itself, so the total shown here is for display only.
 */
@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly _items = signal<CartItem[]>(loadCart());

  /** Read-only view for components. Only this service can change the cart. */
  readonly items = this._items.asReadonly();
  readonly count = computed(() => this._items().reduce((sum, item) => sum + item.quantity, 0));
  readonly total = computed(() =>
    this._items().reduce((sum, item) => sum + item.price * item.quantity, 0),
  );

  constructor() {
    // effect() re-runs whenever a signal it reads changes: here, save on every cart change.
    effect(() => {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(this._items()));
      } catch {
        // Storage can be unavailable (private mode). The cart still works in memory.
      }
    });
  }

  add(product: Product, quantity = 1): void {
    this._items.update((items) => {
      const existing = items.find((item) => item.productId === product.id);
      if (existing) {
        return items.map((item) =>
          item.productId === product.id
            ? { ...item, quantity: Math.min(MAX_QUANTITY, item.quantity + quantity) }
            : item,
        );
      }
      return [
        ...items,
        {
          productId: product.id,
          name: product.name,
          category: product.category,
          price: product.price,
          quantity: Math.min(MAX_QUANTITY, quantity),
        },
      ];
    });
  }

  setQuantity(productId: number, quantity: number): void {
    if (quantity <= 0) {
      this.remove(productId);
      return;
    }
    this._items.update((items) =>
      items.map((item) =>
        item.productId === productId ? { ...item, quantity: Math.min(MAX_QUANTITY, quantity) } : item,
      ),
    );
  }

  remove(productId: number): void {
    this._items.update((items) => items.filter((item) => item.productId !== productId));
  }

  clear(): void {
    this._items.set([]);
  }
}

function loadCart(): CartItem[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as CartItem[]) : [];
  } catch {
    return [];
  }
}
