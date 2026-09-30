import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';

import { InventoryService } from '../../core/api/inventory.service';
import { ProductService } from '../../core/api/product.service';
import { errorMessage } from '../../core/errors';
import { Product, Stock } from '../../core/models';

interface StockRow {
  stock: Stock;
  product: Product | undefined;
}

/**
 * Creating a product here shows an event flowing between services:
 * product-service saves it and publishes ProductCreatedEvent, and
 * inventory-service consumes it and creates the stock record. Refresh the
 * inventory table a second later to see the new row appear.
 */
@Component({
  selector: 'app-admin',
  imports: [ReactiveFormsModule, CurrencyPipe],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly form = this.fb.group({
    sku: ['', [Validators.required, Validators.pattern(/^[A-Z0-9-]+$/), Validators.maxLength(40)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    category: ['Electronics', Validators.required],
    price: [9.99, [Validators.required, Validators.min(0.01)]],
    initialStock: [10, [Validators.required, Validators.min(0)]],
    description: [''],
  });

  protected readonly categories = ['Electronics', 'Home & Kitchen', 'Books', 'Sports', 'Fashion'];
  protected readonly saving = signal(false);
  protected readonly message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  private readonly products = signal<Product[]>([]);
  private readonly stock = signal<Stock[]>([]);
  protected readonly restockAmounts: Record<number, number> = {};

  protected readonly rows = computed<StockRow[]>(() => {
    const byId = new Map(this.products().map((p) => [p.id, p]));
    return this.stock().map((stock) => ({ stock, product: byId.get(stock.productId) }));
  });

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    forkJoin({
      products: this.productService.list({ page: 0, size: 100 }),
      stock: this.inventoryService.list(),
    }).subscribe({
      next: ({ products, stock }) => {
        this.products.set(products.content);
        this.stock.set(stock);
      },
      error: (err) => this.message.set({ type: 'error', text: errorMessage(err) }),
    });
  }

  protected createProduct(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.message.set(null);
    this.productService.create(this.form.getRawValue()).subscribe({
      next: (product) => {
        this.saving.set(false);
        this.message.set({
          type: 'success',
          text: `Created "${product.name}". inventory-service will pick up the ProductCreatedEvent in a moment.`,
        });
        this.form.reset();
        // Give the event a moment to travel through Kafka, then reload.
        setTimeout(() => this.load(), 1500);
      },
      error: (err) => {
        this.saving.set(false);
        this.message.set({ type: 'error', text: errorMessage(err) });
      },
    });
  }

  protected restock(productId: number): void {
    const quantity = Number(this.restockAmounts[productId] ?? 0);
    if (!quantity || quantity < 1) {
      return;
    }
    this.inventoryService.restock(productId, quantity).subscribe({
      next: (updated) => {
        this.stock.update((list) => list.map((s) => (s.productId === productId ? updated : s)));
        this.restockAmounts[productId] = 0;
      },
      error: (err) => this.message.set({ type: 'error', text: errorMessage(err) }),
    });
  }

  protected setRestockAmount(productId: number, value: string): void {
    this.restockAmounts[productId] = Number(value);
  }
}
