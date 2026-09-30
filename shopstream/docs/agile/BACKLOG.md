# Product backlog

How ShopStream would have been planned by a Scrum team. Stories use the format
*As a <role>, I want <goal>, so that <benefit>*, with acceptance criteria in
Given / When / Then form. Points use the Fibonacci scale (1, 2, 3, 5, 8, 13) and
measure relative effort, not hours.

Each story links to the code that implements it, so you can trace a
requirement to its implementation.

## Epic 1: Accounts and security

**US-101: Register an account** (3 points)
As a shopper, I want to create an account, so that I can place orders.
- Given a new email and a password of at least 8 characters, when I sign up, then my account is created and I am logged in.
- Given an email that is already registered, when I sign up, then I see "An account with this email already exists".
- Passwords are stored as BCrypt hashes, never in plain text.
- Code: `user-service/.../auth/AuthService.register`, `frontend/.../auth/register.component.ts`

**US-102: Log in** (3 points)
As a shopper, I want to log in, so that I can see my orders on any device.
- Given valid credentials, when I log in, then I receive a token valid for 2 hours.
- Given a wrong password, when I log in, then I see a generic "Invalid email or password".
- Code: `AuthService.login`, `JwtService`, `frontend/.../auth.service.ts`

**US-103: Protect private endpoints** (5 points)
As the business, I want orders, payments and notifications to require login, so that customers only see their own data.
- Given no token or an invalid token, when I call `/api/orders`, then the gateway answers 401.
- Given a valid token, when I request another user's order, then I get 404.
- Code: `api-gateway/.../JwtAuthenticationFilter`, `OrderService.findOne`

## Epic 2: Catalog

**US-201: Browse and search products** (5 points)
As a shopper, I want to search and filter products by category, so that I find what I need quickly.
- Results are paged (8 per page in the UI) and sorted by name.
- The search matches name or description, case-insensitive.
- Code: `ProductService.search`, `ProductSpecifications`, `product-list.component.ts`

**US-202: See stock on the product page** (2 points)
As a shopper, I want to see whether a product is in stock, so that I am not surprised at checkout.
- "Only N left" appears when 5 or fewer are available.
- Code: `InventoryController`, `product-detail.component.ts`

**US-203: Admin adds a product** (5 points)
As an admin, I want to add products with an initial stock level, so that I can grow the catalog.
- Only users with the ADMIN role can create products (403 otherwise).
- The new product gets a stock record automatically (ProductCreatedEvent).
- Code: `ProductController.create`, `ProductEventPublisher`, `InventoryService.createStockItem`, `admin.component.ts`

**US-204: Admin restocks a product** (2 points)
- Code: `InventoryService.restock`, `admin.component.ts`

## Epic 3: Ordering

**US-301: Cart** (3 points)
As a shopper, I want a cart that survives a page refresh, so that I can shop across visits.
- Code: `frontend/.../cart.service.ts`

**US-302: Place an order** (8 points)
As a shopper, I want to place an order from my cart, so that I can buy products.
- Prices are always taken from the catalog, never from the browser.
- The order is created as PENDING and the response comes back immediately.
- Code: `OrderService.placeOrder`, `ProductClient`, `cart.component.ts`

**US-303: Reserve stock for orders** (8 points)
As the business, I want stock reserved when an order is placed, so that we never sell more than we have.
- All items are reserved, or none are.
- Two concurrent orders for the last unit: exactly one succeeds.
- Code: `InventoryService.reserve`, `InventoryItemRepository.findAllForUpdate`

**US-304: Take payment** (5 points)
As the business, I want reserved orders charged, so that we get paid.
- An order is never charged twice, even if the event is delivered twice.
- Payments above $5,000 are declined (demo rule).
- Code: `PaymentService.processPayment`, `FakePaymentGateway`

**US-305: Roll back failed orders** (8 points)
As a shopper, I want failed orders cancelled cleanly, so that I am not charged and stock is not lost.
- Out of stock: the order is cancelled with the reason.
- Payment declined: the order is cancelled and the reserved stock is released.
- Code: `OrderSagaListener`, `Order.cancel`, `InventoryService.release`

**US-306: Follow my order live** (5 points)
As a shopper, I want to see my order's progress, so that I know it went through.
- The order page updates without a refresh until the order is final.
- Code: `order-detail.component.ts`, `OrderStatusHistory`

## Epic 4: Notifications

**US-401: Order notifications** (5 points)
As a shopper, I want a notification when my order is received, confirmed or cancelled.
- The same event never creates two notifications.
- An unread count shows in the header.
- Code: `notification-service`, `notification-list.component.ts`

## Epic 5: Platform (technical stories)

These have no end-user value on their own, but the team needs them. They are still written as stories and estimated.

- **TS-501:** Transactional outbox so no order event is lost (8). Code: `order-service/.../outbox`
- **TS-502:** Dead-letter topics for failing messages (3). Code: `KafkaErrorHandlingAutoConfiguration`
- **TS-503:** Docker Compose stack with health checks (5). Code: `docker-compose.yml`
- **TS-504:** CI pipeline: build, test, images (5). Code: `.github/workflows/ci.yml`, `Jenkinsfile`
- **TS-505:** Kubernetes manifests and cloud infrastructure (13). Code: `infra/`

## Not started (ideas for your own sprints)

- Order shipping status and tracking number
- Product reviews and ratings
- Coupons and discounts
- Password reset by email
- Real-time order updates with Server-Sent Events
- Distributed tracing across services

The [learning guide](../LEARNING-GUIDE.md) has hints for several of these.
