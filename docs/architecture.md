## Слой представления (Thymeleaf)# Архитектура

> Описание слоёв, конфигурации и внешних зависимостей проекта «ETK».
> Последнее обновление: <20 октября 2026>.



### Что где лежит

| Слой | Пакет | Назначение |
|---|---|---|
| **controller/mvc** | `controller.mvc`, `.mvc.order`, `.mvc.handbook`, `.mvc.journal` | HTML через Thymeleaf. Layout — `welcomeToHome/layout`, контент — фрагменты. |
| **controller/rest** | `controller.rest` | JSON (пока только `EmployeeController`). |
| **service** | `service`, `.handbook` | Бизнес-логика. Ядро — `OrderService`. |
| **service/mapper** | `service.mapper` | `OrderMapper` (entity → DTO). |
| **repository** | `repository.order`, `.handbook`, `.journal` | Spring Data JPA. `@EntityGraph` для борьбы с N+1. |
| **domain/entity** | `domain.entity.order`, `.handbook`, `.journal` | JPA-сущности (доменная модель). |
| **dto** | `dto`, `dto.kafka` | Объекты для передачи между слоями. |
| **util** | `util` | Калькуляторы: `OrderCostCalculator`, `PipeOrderCostCalculator`, `WorkpieceCalculator`. |
| **web/interceptor** | `web.interceptor` | `GlobalModelInterceptor` — добавляет `currentUri` в модель. |
| **configuration** | `configuration` | Spring-конфиги. |

## Ключевые технические решения

### 1. `@EntityGraph` в репозиториях

`OrderRepository` использует `@EntityGraph` для загрузки связей одним JOIN-запросом.
**Зачем:** без этого Hibernate делает **N+1 запрос** (12 заказов → 12 запросов на `company`,
12 на `coupling`...). С `@EntityGraph` — один запрос. На больших объёмах это критично.
Разные методы грузят разные наборы связей — где-то `additionalWorks` нужен, где-то нет.

### 2. `open-in-view=false`

`spring.jpa.open-in-view=false` в `application.properties`. **Зачем:** сессия Hibernate
закрывается сразу после выхода из транзакции. Это правильно, но означает, что **все обращения
к lazy-связям должны быть внутри `@Transactional`**. Если вне — `LazyInitializationException`.
См. известные проблемы в `domain-model.md`.

### 3. Liquibase, `ddl-auto=none`

Схема БД меняется **только** через миграции (`db/changelog/`). Hibernate не трогает схему.
**Правило:** никогда не менять `ddl-auto` на `update` в продакшене.

### 4. Snapshot надбавки (`appliedMarkupPercent`)

См. `domain-model.md`. Кратко: цена фиксируется на момент заказа, изменение справочника
не влияет на старые заказы.

## Конфигурация (`configuration/`)

| Класс | Что делает | Замечания |
|---|---|---|
| `SecurityConfig` | Роли: ADMIN, MANAGER, MASTER, WORKER. Form-login. | **⚠️ Техдолг:** in-memory пользователи + пароли в коде. TODO: БД + BCrypt-хеши или Keycloak. CSRF отключён (TODO перед продом). |
| `WebMvcConfig` | Регистрирует `GlobalModelInterceptor` + раздаёт `/uploads/**`. | |
| `WebConfig` | **Дублирует** раздачу `/uploads/**`. | **⚠️ Техдолг:** объединить с `WebMvcConfig`. |
| `KafkaConfig` | `ProducerFactory` + `KafkaTemplate` для `OrderStartedEvent`. | **⚠️ Техдолг:** `localhost:9092` захардкожен. Перенести в `application.properties`. Kafka выключена через `kafka.enabled=false`. |
| `GlobalControllerAdvice` | `@ModelAttribute` currentUri. | **⚠️ Имя обманывает:** обработчика ошибок нет. `currentUri` дублируется в `OrderController` (несколько методов), `GlobalModelInterceptor`. |

## База данных

- PostgreSQL 15 (docker-compose).
- Миграции — Liquibase (`src/main/resources/db/changelog/`).
- `spring.jpa.hibernate.ddl-auto=none`.
- `spring.jpa.show-sql=true` — для отладки, **выключить перед продом**.
- `spring.jpa.open-in-view=false`.

## Внешние сервисы

### Kafka (сейчас заглушена)

- `kafka.enabled=false` в `application.properties`.
- `KafkaProducerService` — producer событий `OrderStartedEvent` (при старте производства).
- **Пока не используется.** Когда включим — надо:
    - вынести `localhost:9092` в properties;
    - проверить `JacksonJsonSerializer.ADD_TYPE_INFO_HEADERS = false` (правильно для внешних
      потребителей, которые не знают про наш пакет).



## Тесты

| Тип | Где | Статус |
|---|---|---|
| Юнит | `src/test/java/.../service` | **⚠️ Не работают.** В бэклоге: починить + дописать character-тесты. |
| e2e | `e2e/` (Playwright) | Есть спеки (`auth.spec.ts`, `journal.spec.ts`, `orders-read.spec.ts`). |
| Нагрузочные | `loadtest/` (k6) | Есть скрипт + seed-данные. |

**Правило проекта:** без зелёных тестов **не трогаем бизнес-логику**. Механические
переименования/перемещения — можно (компиляция ловит). Логику — только с тестами.

## Известные архитектурные техдолги (сводка)

Из `domain-model.md` + этого файла:

1. `@Data` на JPA-entity (критично).
2. `@Builder.Default` для `additionalWorks`.
3. `createdAt` не автоматический.
4. `getOrderEntityById` / `getOrdersByIdsWithDetails` без `@Transactional` (риск lazy).
5. `OrderService` (31 KB) — god object. Кандидат на разбиение: `OrderCreationService`,
   `OrderPricingService`, `OrderQueryService`.
6. `OrderController` (22 KB) — god object + REST-методы внутри MVC-контроллера.
7. Шаблоны `completed.html` / `production.html` — 90% дублирование.
8. `WebConfig` + `WebMvcConfig` — дублирование `/uploads/**`.
9. `GlobalControllerAdvice` — имя обманывает, `currentUri` дублируется.
10. `String productType` вместо `ProductType` enum в DTO/entity.
11. Kafka `localhost:9092` захардкожен.
12. `docker-compose.yml` — пароль БД в открытом виде.
13. `SecurityConfig` — пароли in-memory в коде.
14. В тестах пакет `utill` (опечатка), `OrderCostCalculator.java` → `...Test.java`,
    `MachinesService.java` → `MachinesServiceTest.java`.

**Все пункты — в бэклоге. Ничего не трогаем без тестов.**
## Слои

### Структура

- **Layout** — `welcomeToHome/layout.html`. Единая обёртка для всех страниц.
  Внутри — `<turbo-frame id="app">` с динамическим контентом.
- **Фрагменты** — `fragments/`. Переиспользуемые блоки:
    - `navbar.html` — навигация.
    - `scene.html` — анимированный фон (фабрика, медведь, листья).
    - `orders/waiting.html`, `orders/production.html`, `orders/completed.html`,
      `orders/view.html`, `orders/create.html`, `orders/edit.html` — страницы заказов.
    - `handbook/*` — формы/списки справочников (муфты, переводники, резьбы, ...).
    - `production-records/*` — журнал производства.
    - `reports/*` — отчёты.
    - `offer/commercial-offer.html` — КП.

### Технологии

- **Thymeleaf** — серверный рендеринг.
- **Hotwire Turbo** (`@hotwired/turbo`) — SPA-подобная навигация без перезагрузки.
  Ссылки помечены `data-turbo-frame="app"` — контент подменяется в `<turbo-frame id="app">`.
- **Bootstrap 5** + Bootstrap Icons — базовая вёрстка.
- **Кастомные CSS** — `styles.css`, `handbook.css`, `orders.css`.
- **JS** — `app.js`, `orders-form.js`, `journal-form.js`, `handbook.js`, `tooltip.js`,
  `drawing-viewer.js`, `bear-scene.js`, `nav-beam.js`.

### Известные проблемы (в бэклоге)

1. **Дублирование карточек заказа.** `waiting.html`, `production.html`, `completed.html`
   содержат ~80% одинакового кода (`order-product`, `order-meta`, `product-tooltip`).
   **План:** вынести в `fragments/orders/card.html`, использовать `th:replace`.
2. **Две модели данных для одной карточки.** `waiting.html` использует `Map<String, Object>`
   (`item['product'].name`), а `completed.html`/`production.html` — `OrderDto`
   (`order.productName`). **План:** везде `OrderDto` (он уже содержит все поля).
3. **Логика продукта в Thymeleaf.** В шаблонах — длинные `th:switch`/тернарники для
   отображения типа продукта, названия, цвета иконки. **План:** вынести в `OrderDto`
   (уже частично есть) и/или `ProductType` enum с `displayName`.
4. **`getProductName()` / `getUnitManufacturingCost()` в entity** — используются
   в шаблонах (fallback). Внутри транзакции безопасно, но лучше вынести в маппер.
   **Требует проверки** в `OrderMapper`.

**Все проблемы — в бэклоге. Шаблоны не трогаем, пока не покроем бэкенд тестами.**