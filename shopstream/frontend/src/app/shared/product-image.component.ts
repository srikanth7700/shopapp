import { Component, computed, input } from '@angular/core';

const CATEGORY_COLORS: Record<string, [string, string]> = {
  Electronics: ['#4f46e5', '#7c3aed'],
  'Home & Kitchen': ['#ea580c', '#f59e0b'],
  Books: ['#0f766e', '#14b8a6'],
  Sports: ['#be123c', '#f43f5e'],
  Fashion: ['#1d4ed8', '#0ea5e9'],
};

/**
 * A generated placeholder "photo": the product's initials on a colour that
 * depends on its category. Keeps the demo free of external image URLs.
 */
@Component({
  selector: 'app-product-image',
  template: `
    @if (imageUrl()) {
      <img class="product-image" [src]="imageUrl()" [alt]="name()" />
    } @else {
      <div class="product-image" [style.background]="background()" [attr.aria-label]="name()">
        <span>{{ initials() }}</span>
      </div>
    }
  `,
})
export class ProductImageComponent {
  readonly name = input.required<string>();
  readonly category = input<string>('');
  readonly imageUrl = input<string | null>(null);

  protected readonly initials = computed(() =>
    this.name()
      .split(/\s+/)
      .filter((word) => /^[A-Za-z]/.test(word))
      .slice(0, 2)
      .map((word) => word[0].toUpperCase())
      .join(''),
  );

  protected readonly background = computed(() => {
    const [from, to] = CATEGORY_COLORS[this.category()] ?? ['#475569', '#94a3b8'];
    return `linear-gradient(135deg, ${from}, ${to})`;
  });
}
