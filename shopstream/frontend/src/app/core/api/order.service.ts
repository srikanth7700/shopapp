import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api-config';
import { Order } from '../models';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/orders`;

  place(items: { productId: number; quantity: number }[]): Observable<Order> {
    return this.http.post<Order>(this.url, { items });
  }

  list(): Observable<Order[]> {
    return this.http.get<Order[]>(this.url);
  }

  get(id: number): Observable<Order> {
    return this.http.get<Order>(`${this.url}/${id}`);
  }
}
