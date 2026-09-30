# Learning guide

A suggested path through the code. Each section says what to read, what to try, and what to build. Take it in order: the later sections build on the earlier ones.

Before you start, get the stack running (see the [README](../README.md)) and place a few orders so you have data to look at.

---

## 1. Watch the system work

Before reading code, watch data move through the system.

1. Place a normal order in the UI and keep the order page open.
2. Open **Kafka UI** (http://localhost:8090) → Topics → `order-events` → Messages. Find your `OrderCreatedEvent`. Look at the **key** (the order id) and the `__TypeId__` **header** (the Java class name used to deserialize it).
3. Open `inventory-events` and `payment-events` and follow the same order id.
4. Open **Consumer groups** in Kafka UI. There is one group per service. Each group reads every message independently, and "lag" shows how far behind it is.
5. Open **Adminer** (http://localhost:8091), database `orders_db`, and run:
   ```sql
   SELECT * FROM orders ORDER BY id DESC;
   SELECT * FROM order_status_history ORDER BY id DESC;
   SELECT id, topic, event_type, published_at FROM outbox_events ORDER BY id DESC;
   ```
6. Watch the logs of the whole saga in one terminal:
   ```bash
   docker compose logs -f order-service inventory-service payment-service notification-service
   ```

### Experiments

- **Kill a service mid-saga.** Run `docker compose stop payment-service`, then place an order. It stops at `INVENTORY_RESERVED`. In Kafka UI the `payment-service` consumer group now shows lag. Run `docker compose start payment-service` and the order completes on its own. This is why asynchronous messaging makes systems resilient.
- **Kill the gateway's target.** Stop product-service and load the shop. The gateway's circuit breaker returns a clean `503` instead of hanging.
- **See the compensation.** Note the laptop's `available_quantity` in `inventory_db.inventory_items`, order the laptop, and check again: the stock was reserved, then released. `stock_reservations` shows the reservation with status `RELEASED`.

---

## 2. Core Java

The code targets **Java 25**. Many enterprise codebases still run Java 8 or 11, so here are selected newer features used in this repo, with their Java 8 equivalents:

| Feature (version) | Example in this repo | Java 8 way |
|---|---|---|
| Records (16) | `OrderCreatedEvent`, every `*Request` / `*Response` DTO | A final class with private final fields, constructor, getters, `equals`/`hashCode`/`toString` (or Lombok `@Value`) |
| `Stream.toList()` (16) | `ProductService.getByIds` | `.collect(Collectors.toList())` |
| Text blocks (15) | `OutboxRepository` native query, `LoggingEmailSender` | String concatenation with `+` and `\n` |
| `List.of` / `Map.of` (9) | Tests, `FallbackController` | `Arrays.asList(...)`, `Collections.unmodifiableList(...)` |
| `Optional.ifPresentOrElse` (9) | `OrderService.withOrder` | `if (opt.isPresent()) {...} else {...}` |

Core Java ideas worth finding in the code:

- **Collections and streams:** `InventoryService.reserve` uses `Collectors.toMap` with a merge function and a `TreeMap` supplier, so duplicate lines are summed and product ids end up sorted.
- **`BigDecimal` for money:** `OrderItem` and `Order.addItem`. Try `0.1 + 0.2` with `double` in jshell to see why `double` is wrong for money.
- **Enums with behavior:** `OrderStatus.isFinal()`.
- **Immutability:** records, and `CartService` in the frontend, which always creates new arrays instead of mutating.
- **Encapsulation:** entities expose business methods (`InventoryItem.reserve`, `Order.cancel`) instead of setters, so the rules live in one place.
- **Exceptions:** `ApiException` carries an HTTP status; `GlobalExceptionHandler` turns it into JSON.
- **Concurrency:** `EventPublisher` waits on a `CompletableFuture` with a timeout and restores the interrupt flag on `InterruptedException`.

---

## 3. Spring Boot fundamentals

Read **user-service** first. It is the smallest service.

- **Dependency injection:** every class gets its dependencies through its constructor, with no `@Autowired` on fields. This is why the unit tests can just call `new AuthService(mockRepo, encoder, jwtService)`.
- **Configuration:** `application.yml` uses `${ENV_VAR:default}` everywhere. `JwtProperties` binds `app.jwt.*` into a typed record (`@ConfigurationProperties`).
- **Startup hooks:** `AdminAccountInitializer` (an `ApplicationRunner`) creates the admin user.
- **Auto-configuration:** `shopstream-common/src/main/resources/META-INF/spring/...AutoConfiguration.imports`. Adding the jar to a service is enough to get the Kafka error handler, topic creation and `EventPublisher`. That is exactly how Spring Boot starters work.
- **Actuator:** http://localhost:8081/actuator/health. Docker and Kubernetes use these endpoints to check whether a service is alive.

**Exercise:** add `GET /api/users/me/orders-count`. user-service does not own orders: what are your options? (Call order-service over REST, or keep a count updated from `OrderCreatedEvent`.) Discuss the trade-offs before you build either one.

---

## 4. REST API design

Read `ProductController`, `OrderController` and `GlobalExceptionHandler`.

- **Resources and verbs:** `GET /api/products/{id}`, `POST /api/products` → `201`, `DELETE` → `204`, `PATCH /api/notifications/{id}/read` for a partial update.
- **Validation:** Bean Validation annotations on records (`@NotBlank`, `@Min`) plus `@Valid` in the controller. A failure becomes a `400` with field errors.
- **Errors:** RFC 7807 `ProblemDetail` JSON. Try request 13 in `api-requests.http`.
- **Pagination:** `GET /api/products?page=0&size=12` returns `PageResponse` (content, page, totalPages).
- **Security choices:** `findOne` returns `404` rather than `403` for someone else's order, so users cannot discover which order ids exist.
- **Service-to-service calls:** `ProductClient` uses `RestClient` with connect and read timeouts.

**Exercise:** add `GET /api/products/{id}/related` that returns up to 4 other products from the same category. Write a `@WebMvcTest` for it first, modelled on `ProductControllerTest`.

---

## 5. SQL, JPA and Flyway

- **Migrations:** `src/main/resources/db/migration/V1__*.sql`. Flyway runs each file once and records it in `flyway_schema_history`. Never edit a migration that already ran: add `V3__...` instead.
- **Constraints do real work:** `UNIQUE(order_id)` in payments is what prevents double charging. `CHECK (available_quantity >= 0)` is a last line of defence against overselling.
- **Indexes:** `idx_orders_user_created` matches the "my orders, newest first" query. The partial index `idx_outbox_unpublished` covers only unpublished rows.
- **Relationships:** `Order` ↔ `OrderItem` (`@OneToMany(mappedBy)`, cascade, orphan removal). `StockReservation` uses `@ElementCollection` for value objects.
- **N+1 queries:** `OrderRepository` uses `@EntityGraph(attributePaths = "items")`. Turn on SQL logging to see the difference:
  ```yaml
  # order-service application.yml
  logging.level.org.hibernate.SQL: DEBUG
  ```
  Remove the `@EntityGraph`, list orders, and count the queries.
- **Dynamic queries:** `ProductSpecifications` builds WHERE clauses with the Criteria API.
- **Locking:** `InventoryItemRepository.findAllForUpdate` (pessimistic) and `@Version` on `Order` (optimistic).

**SQL to practise in Adminer** (`orders_db`):
```sql
-- Revenue per day, confirmed orders only
SELECT date_trunc('day', created_at) AS day, count(*) AS orders, sum(total_amount) AS revenue
FROM orders WHERE status = 'CONFIRMED' GROUP BY 1 ORDER BY 1 DESC;

-- Best-selling products
SELECT oi.product_name, sum(oi.quantity) AS units
FROM order_items oi JOIN orders o ON o.id = oi.order_id
WHERE o.status = 'CONFIRMED'
GROUP BY oi.product_name ORDER BY units DESC LIMIT 5;

-- How long each order took from placed to final (a window function)
SELECT order_id, status, changed_at - first_value(changed_at) OVER (PARTITION BY order_id ORDER BY changed_at) AS elapsed
FROM order_status_history ORDER BY order_id DESC, changed_at;
```

**Exercise (other databases):** the job mentions Oracle, SQL Server and MySQL. Switch one service to MySQL: add a `mysql` container to compose, swap the `postgresql` driver for `mysql-connector-j` and `flyway-database-postgresql` for `flyway-mysql`, and change the JDBC URL. Then find what breaks. Hint: `TIMESTAMP WITH TIME ZONE`, `GENERATED BY DEFAULT AS IDENTITY` and `FOR UPDATE SKIP LOCKED` have different spellings or support levels on each database.

---

## 6. Microservices

- **Gateway:** routes in `api-gateway/src/main/resources/application.yml`. The JWT check lives in `JwtAuthenticationFilter`, and the circuit breaker config in `ResilienceConfig`.
- **Service discovery:** there is no Eureka here. Docker Compose and Kubernetes both provide DNS (`http://product-service:8082`), so a separate registry adds nothing. Many older Spring Cloud systems use Eureka + Ribbon/LoadBalancer, and it helps to know what they do.
- **Configuration:** environment variables with defaults, not Spring Cloud Config. In Kubernetes they come from a ConfigMap and a Secret.
- **Data ownership:** find every place a service needs another service's data and note how it gets it: REST call, event, or its own copy.

**Exercise:** add Redis caching to `ProductService.get` with `@Cacheable`, and evict on update with `@CacheEvict`. Add a `redis` container to compose. What happens to the cache when you run two product-service instances, and why is that fine for Redis but not for an in-memory cache?

---

## 7. Event-driven architecture and Kafka

Read in this order:

1. `shopstream-common/.../events`: the event contracts.
2. `order-service/.../outbox`: how events leave order-service.
3. `inventory-service/.../InventoryEventListener` and `InventoryService.reserve`: a consumer that also produces.
4. `order-service/.../OrderSagaListener` and the transitions on `Order`: how the saga ends.
5. `KafkaErrorHandlingAutoConfiguration`: retries and dead-letter topics.
6. The consumer settings in any `application.yml` (`ErrorHandlingDeserializer`, `trusted.packages`, `group-id`).

### Experiments

- **Scale a consumer:** `docker compose up -d --scale notification-service=2` fails because of the fixed container name and port. Remove `container_name` and `ports` for that service, then scale it and watch Kafka UI split the 3 partitions between the two consumers.
- **Poison message:** in Kafka UI, produce a message to `order-events` with the value `not json`. A deserialization error can never succeed on retry, so the error handler skips the retries and sends it straight to `order-events.DLT`, once for each service that consumes that topic. Find it there, and notice that the consumers keep working.
- **Duplicate delivery:** in Kafka UI, copy an `InventoryReservedEvent` and produce it again with the same headers. payment-service logs "already charged" and nobody is charged twice.

### Exercises

1. **Shipping:** add a `SHIPPED` status. An admin endpoint `POST /api/orders/{id}/ship` publishes `OrderShippedEvent`, and notification-service tells the customer.
2. **Replace polling with push:** stream order updates to the browser with Server-Sent Events (`SseEmitter` in order-service, `EventSource` in Angular).
3. **Orchestration:** create an `OrderSagaOrchestrator` in order-service that sends commands (`ReserveStockCommand`, `ChargePaymentCommand`) instead of letting services react to each other's events. Compare the code.
4. **JMS side by side:** add an ActiveMQ Artemis container and `spring-boot-starter-artemis`. Make notification-service also put each email on a JMS queue that a separate `@JmsListener` "sends". Compare `JmsTemplate` with `KafkaTemplate`, and read the Kafka vs JMS table in [ARCHITECTURE.md](ARCHITECTURE.md#kafka-vs-jms--ibm-mq).
5. **Payment timeout:** what if payment-service never answers? Add a scheduled job in order-service that cancels orders stuck in `INVENTORY_RESERVED` for more than 5 minutes. Which compensation does that trigger?

---

## 8. Angular

The frontend uses **Angular 20** with standalone components and signals. If your team's codebase is older (Angular 10-14), it probably uses **NgModules**; the table below maps the ideas.

| Modern (this repo) | Older Angular |
|---|---|
| `bootstrapApplication(AppComponent, appConfig)` in `main.ts` | `platformBrowserDynamic().bootstrapModule(AppModule)` |
| Component `imports: [RouterLink, CurrencyPipe]` | `declarations` + `imports` in an `@NgModule` |
| `provideHttpClient(withInterceptors([authInterceptor]))` | `HttpClientModule` + a class implementing `HttpInterceptor`, registered with `HTTP_INTERCEPTORS` |
| Functional guards (`authGuard: CanActivateFn`) | Class guards implementing `CanActivate` |
| `signal()`, `computed()`, `effect()` | `BehaviorSubject` in services + the `async` pipe |
| `@if`, `@for` control flow | `*ngIf`, `*ngFor` |
| `inject(Service)` | Constructor injection (still works everywhere) |
| `input.required<string>()` | `@Input() id!: string` |
| `loadComponent: () => import(...)` | `loadChildren: () => import(...).then(m => m.OrdersModule)` |

Where to look:

- **State:** `CartService` (signals + `localStorage`), `AuthService`.
- **HTTP:** `core/api/*.service.ts`, `auth.interceptor.ts` (adds the token, logs out on 401).
- **Routing:** `app.routes.ts` (lazy-loaded pages, guards, route params bound to inputs).
- **Forms:** `login.component.ts` and `admin.component.ts` (reactive forms and validators).
- **RxJS:** `product-list.component.ts` (`debounceTime` + `switchMap` for search), `order-detail.component.ts` (`timer` + `switchMap` + `takeWhile` for polling).
- **Tests:** `cart.service.spec.ts`, `auth.interceptor.spec.ts` (`HttpTestingController`).

**Exercises:**
1. Add a product review form on the product page (you will need a new backend endpoint too).
2. Rewrite `CartService` with a `BehaviorSubject` instead of signals, so you know both styles.
3. Add an order-search box on the orders page that filters client-side with a `computed()` signal.

---

## 9. Testing

- **Unit tests with Mockito:** `OrderServiceTest`, `InventoryServiceTest` and `PaymentServiceTest` cover business rules without Spring, a database or Kafka. They run in milliseconds.
- **Web slice test:** `ProductControllerTest` (`@WebMvcTest`) covers JSON, validation, status codes and the admin check.
- **Gateway filter test:** `JwtAuthenticationFilterTest` uses a mock reactive exchange.
- **Angular:** TestBed, `HttpTestingController`.

**Exercise:** add a Testcontainers integration test for order-service that starts real PostgreSQL and Kafka containers, places an order, and asserts that the `OrderCreatedEvent` arrives on `order-events`. Start with `@SpringBootTest`, `@Testcontainers`, `PostgreSQLContainer` and `KafkaContainer`, and use `@ServiceConnection` so Spring Boot wires the URLs for you.

---

## 10. Docker, CI/CD and cloud

- `backend.Dockerfile`: a multi-stage build, a Maven cache mount, a non-root user, and a container-aware JVM (`MaxRAMPercentage`).
- `docker-compose.yml`: health checks, `depends_on` conditions, YAML anchors.
- `.github/workflows/ci.yml`: parallel jobs, caching, a matrix build for images. `deploy-aws.yml` uses OIDC so no AWS keys are stored in GitHub.
- `Jenkinsfile`: the same pipeline in Jenkins, including a manual approval before deploying.
- `infra/k8s`: Deployments, Services, probes, HPA, and Kustomize overlays per cloud.
- `infra/terraform/aws`: infrastructure as code with community modules.

**Exercises:**
1. Push the repo to GitHub and watch the CI workflow run. Break a test on a branch, open a pull request, and see it blocked.
2. Install [kind](https://kind.sigs.k8s.io/) and deploy the Kubernetes manifests locally. Hint: write a `local` overlay that adds PostgreSQL and Kafka as in-cluster Deployments.
3. Add a Dependabot config (`.github/dependabot.yml`) for Maven, npm and GitHub Actions.

---

## 11. Agile and Scrum

Read [docs/agile](agile). The backlog is written the way the project would have been planned: epics, user stories with acceptance criteria, and story points. Each story names the code that implements it, so you can trace a requirement to its implementation.

**Exercise:** pick an exercise from this guide, write it as a user story with acceptance criteria using the issue template, estimate it, create a `feature/...` branch, and open a pull request using the PR template. That is the full loop you will follow at work.
