import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, debounceTime, map, of, switchMap } from 'rxjs';

import { ProductService } from '../../core/api/product.service';
import { CartService } from '../../core/cart/cart.service';
import { errorMessage } from '../../core/errors';
import { Page, Product } from '../../core/models';
import { ProductImageComponent } from '../../shared/product-image.component';

const PAGE_SIZE = 8;

/**
 * Signals hold the filter state (search, category, page). Whenever one changes,
 * the `query` computed signal changes, which is turned into an Observable so we
 * can use RxJS operators:
 * - debounceTime: wait until the user stops typing
 * - switchMap: cancel the previous HTTP call if a newer query arrives
 */
@Component({
  selector: 'app-product-list',
  imports: [RouterLink, CurrencyPipe, ProductImageComponent],
  templateUrl: './product-list.component.html',
})
export class ProductListComponent {
  private readonly productService = inject(ProductService);
  private readonly cart = inject(CartService);

  protected readonly search = signal('');
  protected readonly category = signal<string | null>(null);
  protected readonly page = signal(0);
  protected readonly error = signal<string | null>(null);
  protected readonly lastAdded = signal<number | null>(null);

  protected readonly categories = toSignal(
    this.productService.categories().pipe(catchError(() => of([] as string[]))),
    { initialValue: [] as string[] },
  );

  private readonly query = computed(() => ({
    search: this.search().trim(),
    category: this.category(),
    page: this.page(),
    size: PAGE_SIZE,
  }));

  protected readonly result = toSignal(
    toObservable(this.query).pipe(
      debounceTime(250),
      switchMap((query) =>
        this.productService.list(query).pipe(
          map((page): Page<Product> | null => {
            this.error.set(null);
            return page;
          }),
          catchError((err) => {
            this.error.set(errorMessage(err));
            return of(null);
          }),
        ),
      ),
    ),
    { initialValue: null },
  );

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected selectCategory(category: string | null): void {
    this.category.set(category);
    this.page.set(0);
  }

  protected addToCart(product: Product): void {
    this.cart.add(product);
    this.lastAdded.set(product.id);
    setTimeout(() => {
      if (this.lastAdded() === product.id) {
        this.lastAdded.set(null);
      }
    }, 1500);
  }
}
