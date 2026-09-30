import { CurrencyPipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { InventoryService } from '../../core/api/inventory.service';
import { ProductService } from '../../core/api/product.service';
import { CartService, MAX_QUANTITY } from '../../core/cart/cart.service';
import { errorMessage } from '../../core/errors';
import { Product, Stock } from '../../core/models';
import { ProductImageComponent } from '../../shared/product-image.component';

/**
 * This page combines data from two microservices: product-service (name,
 * price) and inventory-service (stock). The browser makes both calls through
 * the gateway. (A "Backend for Frontend" service could combine them instead.)
 */
@Component({
  selector: 'app-product-detail',
  imports: [CurrencyPipe, RouterLink, ProductImageComponent],
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  /** Filled from the :id route parameter thanks to withComponentInputBinding(). */
  readonly id = input.required<string>();

  private readonly productService = inject(ProductService);
  private readonly inventoryService = inject(InventoryService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);

  protected readonly product = signal<Product | null>(null);
  protected readonly stock = signal<Stock | null>(null);
  protected readonly quantity = signal(1);
  protected readonly error = signal<string | null>(null);
  protected readonly maxQuantity = MAX_QUANTITY;

  ngOnInit(): void {
    const productId = Number(this.id());
    this.productService.get(productId).subscribe({
      next: (product) => this.product.set(product),
      error: (err) => this.error.set(errorMessage(err)),
    });
    this.inventoryService.get(productId).subscribe({
      next: (stock) => this.stock.set(stock),
      error: () => this.stock.set(null),
    });
  }

  protected changeQuantity(delta: number): void {
    this.quantity.update((q) => Math.min(this.maxQuantity, Math.max(1, q + delta)));
  }

  protected addToCart(goToCart: boolean): void {
    const product = this.product();
    if (!product) {
      return;
    }
    this.cart.add(product, this.quantity());
    if (goToCart) {
      this.router.navigateByUrl('/cart');
    }
  }
}
