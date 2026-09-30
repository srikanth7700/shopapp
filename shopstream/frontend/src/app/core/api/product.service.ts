import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api-config';
import { Page, Product, ProductRequest } from '../models';

export interface ProductQuery {
  search?: string;
  category?: string | null;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ProductService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/products`;

  list(query: ProductQuery): Observable<Page<Product>> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 12);
    if (query.search) {
      params = params.set('search', query.search);
    }
    if (query.category) {
      params = params.set('category', query.category);
    }
    return this.http.get<Page<Product>>(this.url, { params });
  }

  get(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.url}/${id}`);
  }

  categories(): Observable<string[]> {
    return this.http.get<string[]>(`${this.url}/categories`);
  }

  create(request: ProductRequest): Observable<Product> {
    return this.http.post<Product>(this.url, request);
  }
}
