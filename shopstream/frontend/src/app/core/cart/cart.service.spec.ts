import { TestBed } from '@angular/core/testing';

import { Product } from '../models';
import { CartService } from './cart.service';

describe('CartService', () => {
  const headphones: Product = {
    id: 1,
    sku: 'SS-ELEC-001',
    name: 'Headphones',
    description: null,
    category: 'Electronics',
    price: 199.99,
    imageUrl: null,
    active: true,
  };
  const book: Product = { ...headphones, id: 8, sku: 'SS-BOOK-001', name: 'Book', category: 'Books', price: 42 };

  let cart: CartService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({});
    cart = TestBed.inject(CartService);
  });

  it('adds products and merges quantities of the same product', () => {
    cart.add(headphones);
    cart.add(headphones, 2);
    cart.add(book);

    expect(cart.items().length).toBe(2);
    expect(cart.count()).toBe(4);
  });

  it('computes the total from price x quantity', () => {
    cart.add(headphones, 2);
    cart.add(book, 1);

    expect(cart.total()).toBeCloseTo(441.98, 2);
  });

  it('removes a line when its quantity drops to zero', () => {
    cart.add(book);
    cart.setQuantity(book.id, 0);

    expect(cart.items()).toEqual([]);
  });

  it('caps quantities at 99', () => {
    cart.add(book, 150);

    expect(cart.count()).toBe(99);
  });
});
