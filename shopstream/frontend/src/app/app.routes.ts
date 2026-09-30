import { Routes } from '@angular/router';

import { adminGuard, authGuard } from './core/auth/auth.guards';
import { ProductListComponent } from './features/products/product-list.component';

/**
 * loadComponent = lazy loading: the code for a page is downloaded only when
 * the user first visits it, which keeps the initial bundle small.
 */
export const routes: Routes = [
  { path: '', component: ProductListComponent, title: 'ShopStream' },
  {
    path: 'products/:id',
    loadComponent: () =>
      import('./features/products/product-detail.component').then((m) => m.ProductDetailComponent),
    title: 'Product - ShopStream',
  },
  {
    path: 'cart',
    loadComponent: () => import('./features/cart/cart.component').then((m) => m.CartComponent),
    title: 'Cart - ShopStream',
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
    title: 'Log in - ShopStream',
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register.component').then((m) => m.RegisterComponent),
    title: 'Sign up - ShopStream',
  },
  {
    path: 'orders',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-list.component').then((m) => m.OrderListComponent),
    title: 'My orders - ShopStream',
  },
  {
    path: 'orders/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/orders/order-detail.component').then((m) => m.OrderDetailComponent),
    title: 'Order - ShopStream',
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/notifications/notification-list.component').then(
        (m) => m.NotificationListComponent,
      ),
    title: 'Notifications - ShopStream',
  },
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./features/admin/admin.component').then((m) => m.AdminComponent),
    title: 'Admin - ShopStream',
  },
  { path: '**', redirectTo: '' },
];
