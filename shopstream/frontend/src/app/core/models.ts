// TypeScript interfaces that mirror the JSON returned by the Spring Boot services.
// BigDecimal values arrive as JSON numbers.

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: 'CUSTOMER' | 'ADMIN';
}

export interface AuthResponse {
  token: string;
  expiresInSeconds: number;
  user: User;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  category: string;
  price: number;
  imageUrl: string | null;
  active: boolean;
}

export interface ProductRequest {
  sku: string;
  name: string;
  description: string;
  category: string;
  price: number;
  imageUrl?: string | null;
  initialStock?: number;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type OrderStatus = 'PENDING' | 'INVENTORY_RESERVED' | 'CONFIRMED' | 'CANCELLED';

export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface StatusChange {
  status: OrderStatus;
  note: string | null;
  changedAt: string;
}

export interface Order {
  id: number;
  status: OrderStatus;
  statusReason: string | null;
  totalAmount: number;
  items: OrderItem[];
  history: StatusChange[];
  createdAt: string;
  updatedAt: string;
}

export interface Stock {
  productId: number;
  sku: string;
  availableQuantity: number;
  reservedQuantity: number;
  updatedAt: string;
}

export interface AppNotification {
  id: number;
  orderId: number;
  type: 'ORDER_PLACED' | 'ORDER_CONFIRMED' | 'ORDER_CANCELLED';
  title: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export function isFinalStatus(status: OrderStatus): boolean {
  return status === 'CONFIRMED' || status === 'CANCELLED';
}
