import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE } from '../api-config';
import { Stock } from '../models';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  private readonly http = inject(HttpClient);
  private readonly url = `${API_BASE}/inventory`;

  list(): Observable<Stock[]> {
    return this.http.get<Stock[]>(this.url);
  }

  get(productId: number): Observable<Stock> {
    return this.http.get<Stock>(`${this.url}/${productId}`);
  }

  restock(productId: number, quantity: number): Observable<Stock> {
    return this.http.post<Stock>(`${this.url}/${productId}/restock`, { quantity });
  }
}
