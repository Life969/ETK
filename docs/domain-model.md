# Доменная модель

> Документ описывает бизнес-сущности проекта «ETK» и связи между ними.
> Обновляется по мере разбора домена.
> Последнее обновление: <20.Октября.2026>. 

## Обзор

Проект — учёт заказов на изготовление металлоизделий (муфты, переводники, трубы, патрубки).
Три ключевых домена:

- **order** — заказы и компании-заказчики
- **journal** — журнал производства (записи о выполненной работе)
- **handbook** — справочники (муфты, переводники, резьбы, марки стали, доп. работы, сотрудники, станки)

## Заказ (`OrderEntity`)

**Что это.** Один заказ описывает **один тип продукции** — муфту, переводник, трубу или патрубок.
Тип определяется тем, какая из связей заполнена. Это **single-table inheritance**:
одна таблица `orders`, половина колонок `null` в зависимости от типа. Целостность
обеспечивается **только в коде** (`OrderService.applyProductAndRecalc`), на уровне БД
constraint'ов нет.

### Типы продукции и их поля

| Тип | Заполнено | Не заполнено |
|---|---|---|
| **COUPLING** (муфта) | `coupling`, `quantity`, `workpiece*` | `adapter`, `externalThread`, `pipeProductKind`, `wallThicknessMm`, `averagePipeLengthM`, `orderTotalLengthMeters`, `orderTotalWeightKg`, `pricePerTon`, `pieceLengthMm` |
| **ADAPTER** (переводник) | `adapter`, `quantity`, `workpiece*` | `coupling`, `externalThread`, `pipeProductKind`, ... (те же, что у COUPLING) |
| **PIPE** (труба) | `externalThread`, `pipeProductKind=PIPE`, `wallThicknessMm`, `averagePipeLengthM`, `orderTotalLengthMeters`, `orderTotalWeightKg`, `pricePerTon`, `workpiece*` | `coupling`, `adapter`, `pieceLengthMm` |
| **NIPPLE** (патрубок) | `externalThread`, `pipeProductKind=NIPPLE`, `pieceLengthMm`, `wallThicknessMm`, `orderTotalWeightKg`, `pricePerTon`, `quantity`, `workpiece*` | `coupling`, `adapter`, `averagePipeLengthM` |

### Ключевые поля

- `status` — `OrderStatus` (`WAITING`, `IN_PRODUCTION`, `COMPLETED`). Enum хранится как STRING.
  **Отмена заказа пока не поддерживается** (по ТЗ).
- `createdAt` — дата формирования. **Заполняется вручную** в `OrderService.createOrder`
  (`OrderEntity.builder().createdAt(LocalDateTime.now())`). При редактировании не меняется.
  **⚠️ Техдолг:** перевести на `@PrePersist`, чтобы не забыть в новом коде создания.
- `appliedMarkupPercent` — **снимок** процента надбавки за группу прочности на момент
  создания/редактирования заказа. **Зачем:** если завтра в справочнике `SteelStrengthGroup`
  поменяется процент — старые заказы **не должны** пересчитываться. Классический
  snapshot-паттерн для исторических данных.
- `totalCost` — итоговая стоимость. **Хранится** (не вычисляется на лету) — для отчётов и скорости.

### Связи

- `company` (ManyToOne) — компания-заказчик
- `coupling` (ManyToOne, nullable) — муфта (если заказ на муфту)
- `adapter` (ManyToOne, nullable) — переводник (если заказ на переводник)
- `externalThread` (ManyToOne, nullable) — резьба (если труба/патрубок)
- `strengthGroup` (ManyToOne, nullable) — группа прочности стали
- `steelGrade` (ManyToOne, nullable) — марка стали (информационно, на цену не влияет)
- `additionalWorks` (ManyToMany, join-table `order_additional_work`) — доп. работы

### Бизнес-правила (реализованы в `OrderService`)

#### 1. Пересчёт при создании/редактировании

`applyProductAndRecalc` **полностью пересобирает** продуктовую часть:
- очищает все продуктовые поля (чтобы при смене типа не осталось мусора);
- заполняет нужные поля в зависимости от типа;
- пересчитывает `workpiece_*` и (для PIPE) `order_total_*` / `quantity`.

#### 2. Муфта и переводник (COUPLING / ADAPTER)

- `quantity` приходит из DTO, должен быть > 0.
- Заготовка считается по `D_нар` / `D_внутр` из справочника:
    - `wall = (D_нар − D_внутр) / 2`
    - `totalLengthM = (lengthMm × quantity) / 1000`
    - `weight = calculateWeight(D_нар, wall, totalLengthM)` (см. `WorkpieceCalculator`)

#### 3. Труба (PIPE)

- Менеджер вводит **ИЛИ** метраж (`orderTotalLengthMeters`), **ИЛИ** тоннаж (`orderTotalWeightKg`).
- Второе значение вычисляется:
    - метраж → вес: `calculateWeight(D, S, L)`
    - вес → метраж: `lengthFromWeight(D, S, weight)` — обратная формула из `calculateWeight`
- `quantity` **не берётся из DTO** — вычисляется: `ceil(totalLengthM / avgLength)`, где
  `avgLength = averagePipeLengthM` (или дефолт **10 м**, если не указано). Минимум 1.
- `D` (наружный диаметр) парсится из `ExternalThreadEntity.conditionalDiameter` (строка "73" → 73.0).
  Если строка не парсится — `IllegalArgumentException` (не молчим).

#### 4. Патрубок (NIPPLE)

- `quantity` приходит из DTO, должен быть > 0.
- `pieceLengthMm` (длина одной штуки) приходит из DTO, должен быть > 0.
- `totalLengthM = (pieceLengthMm × quantity) / 1000`
- `weight = calculateWeight(D, S, totalLengthM)`

#### 5. Группа прочности и марка стали

`applyStrengthAndGrade` обрабатывает **4 случая**:

| Случай | Что задано | Что происходит |
|---|---|---|
| 1 | Ничего | Группа = null, марка = null, `appliedMarkupPercent = 0` |
| 2 | Только группа | Марка = null, `appliedMarkupPercent = группа.markupPercent` |
| 3 | Только марка | Группа = `марка.strengthGroup` (автоподстановка), `appliedMarkupPercent = группа.markupPercent` |
| 4 | Обе | Марка **обязана** принадлежать группе, иначе `IllegalArgumentException`. `appliedMarkupPercent = группа.markupPercent` |

#### 6. Расчёт стоимости

Развилка по типу продукции (единственное место вызова калькуляторов — `calculateTotalCost`):

- **COUPLING / ADAPTER** → `OrderCostCalculator.calculate(unitCost, works, quantity, markupPercent, workpieceWeightKg)`
- **PIPE / NIPPLE** → `PipeOrderCostCalculator.calculate(threadManufacturingCost, pricePerTon, quantity, orderTotalWeightKg, works, markupPercent)`

Никаких формул «по месту» — всё в `util`-классах.

### Известные проблемы (в бэклоге)

1. **`@Data` на entity.** `equals`/`hashCode`/`toString` сгенерированы по всем полям, включая связи
   и коллекции → риск `LazyInitializationException` / `StackOverflowError` / «потери» в `HashSet`.
   **Критично.** План: `@Getter`/`@Setter` + `@EqualsAndHashCode(of = "id")` + `@ToString(exclude = {...})`.
2. **`@Builder` игнорирует `additionalWorks = new ArrayList<>()`.** В текущем коде не стреляет,
   потому что `createOrder` всегда передаёт список явно. Но если кто-то забудет — NPE.
   План: `@Builder.Default`.
3. **`createdAt` не автоматический.** Риск забыть в новом коде создания.
   План: `@PrePersist`.
4. **`getOrderEntityById` и `getOrdersByIdsWithDetails` — без `@Transactional`.**
   При `open-in-view=false` обращение к связям вне транзакции → `LazyInitializationException`.
   Пример: `CommercialOfferController.toggleOrder` вызывает `getOrderEntityById(orderId)`
   и затем `order.getCompany().getId()` — риск. **Требует проверки** при разборе контроллеров.
5. **`getProductName()` / `getUnitManufacturingCost()` в entity ходят в связи.**
   Внутри `@Transactional` — безопасно. Вне — риск. План: вынести в маппер/сервис.

## Компания (`CompanyEntity`)

**Что это.** Компания-заказчик. Минимальная сущность: `id`, `name` (unique, `@NotBlank`).

**Связи:** один-ко-многим с `OrderEntity`.

**Особенность:** в `OrderService.resolveCompany` компания **создаётся на лету**, если не найдена
по имени (`findByNameIgnoreCase` → если пусто → `save(new CompanyEntity(name))`). То есть
менеджер вводит название в форму, и если такой компании ещё нет — она появится в справочнике
автоматически. **Это бизнес-решение**, а не баг.

## Статусы и перечисления

- **`OrderStatus`** — `WAITING` (ожидание подписания), `IN_PRODUCTION` (в производстве),
  `COMPLETED` (выполнен). Хранится как STRING. Отмена пока не поддерживается.
- **`PipeProductKind`** — `PIPE` (труба, заказ метражом/тоннажом), `NIPPLE` (патрубок, заказ штуками).
  Хранится как STRING.

## Что ещё нужно разобрать

- [ ] `ProductionRecordEntity` (журнал производства) + его связь с `OrderEntity`
- [ ] Справочники: `CouplingEntity`, `PipeAdapterEntity`, `ExternalThreadEntity`,
  `SteelGradeEntity`, `SteelStrengthGroupEntity`, `AdditionalWorkEntity`,
  `EmployeeEntity`, `MachineEntity`
- [ ] `WorkpieceCalculator` — точные формулы (используются в `OrderService`)
- [ ] `OrderCostCalculator` и `PipeOrderCostCalculator` — точные формулы
- [ ] `OrderMapper` — как entity → DTO (и где могут быть lazy load)
- [ ] Диаграмма связей (mermaid) — после разбора всех entity