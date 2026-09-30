# ShopStream frontend

Angular 20 app for ShopStream. See the main [README](../README.md) and the Angular section of the [learning guide](../docs/LEARNING-GUIDE.md#8-angular).

```bash
npm install
npm start          # dev server on http://localhost:4200, proxies /api to the gateway on :8080
npm test           # unit tests in Chrome, watch mode
npm run test:ci    # unit tests once, headless (used by CI)
npm run build      # production build into dist/frontend/browser
```

```
src/app/
├── core/          services, models, auth (interceptor, guards), cart
├── shared/        small reusable components (product image, status badge)
└── features/      one folder per page (products, cart, auth, orders, notifications, admin)
```
