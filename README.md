Job — система управления производством соединительных деталей труб

Внутренний веб-сервис нефтяного завода для управления заказами на изготовление
соединительных деталей труб (муфт, переводников, труб и патрубков), учёта
производства и формирования коммерческих предложений.

Содержание
----------
- Что это
- Домен
- Стек
- Архитектура
- Структура проекта
- Запуск
- Ключевая бизнес-логика
- Фронтенд
- Тестирование
- Соглашения
- Технический долг
- Для AI-ассистента

---

Что это
-------
Сервис закрывает цикл «заказ → производство → отгрузка → отчётность»:

- Заказы — компании заказывают муфты, переводники, трубы и патрубки с
  указанием количества (или метража/тоннажа — для труб), дедлайна и группы
  прочности стали.
- Коммерческое предложение (КП) — подборка заказов одной компании для
  отправки клиенту.
- Журнал производства — фактические записи о том, кто, на каком станке,
  сколько изделий изготовил.
- Справочники — управление номенклатурой: муфты, переводники, наружные
  резьбы, сотрудники, станки, доп. работы, группы прочности стали и марки
  стали.
- Отчёты — сводка по сотруднику (сколько заработал) и по станку
  (сколько продукции выпущено).

---

Домен
-----

Продукция

Завод производит четыре типа изделий.

Муфта — соединительная деталь труб с внутренней резьбой.
Характеризуется типом (НКТ, ОТТМ), условным диаметром (73, 89, …),
длиной, весом, наружным/внутренним диаметром. Справочник — CouplingEntity.

Переводник — переходник между трубами с разными типами резьбы. Имеет
две стороны (тип + диаметр каждой), длину, вес, диаметры. Справочник —
PipeAdapterEntity.

Труба (PIPE) — заказывается метражом или тоннажом. Характеризуется
наружной резьбой (ExternalThreadEntity), толщиной стенки, средней длиной
одной трубы. Количество штук вычисляется оценочно.

Патрубок (NIPPLE) — заказывается штуками, у каждой задаётся длина.
Использует тот же справочник наружных резьб, что и труба. Различие между
трубой и патрубком — не в справочнике, а в поле заказа pipeProductKind.

У всех четырёх типов есть:
- manufacturingCost — цена изготовления за 1 единицу;
- priceForEmployee — цена для работника за 1 единицу.

Семантика цен:

- Для муфт/переводников manufacturingCost — за 1 изделие.
- Для труб/патрубков manufacturingCost — за 1 нарезку; у готового
  изделия нарезок две, в калькуляторе × 2.
- priceForEmployee — всегда за 1 готовое изделие (у труб/патрубков —
  уже с обеими резьбами).

Наружные резьбы

Отдельный справочник ExternalThreadEntity — только для труб и патрубков.
Муфты (внутренняя резьба) остаются в своём CouplingEntity.

Поля: type (НКТ/ОТТМ/ОТТГ), conditionalDiameter (73, 89, …),
manufacturingCost, priceForEmployee, standart, description,
imagePath.

Название на UI — «НКТ 73».

Жизненный цикл заказа

WAITING (ожидание) ──► IN_PRODUCTION (в производстве) ──► COMPLETED (выполнен)
▲                          │                              │
└──────────────────────────┴──────────────────────────────┘
(возможен возврат на предыдущий статус)

При переходе WAITING → IN_PRODUCTION обязателен дедлайн (если не указан —
запрос открывает модалку для ввода).

Группы прочности стали

По ГОСТ 633-80 / API 5CT существует 6 групп прочности: Д, К, Е, Л, М, Р.
Каждая группа:

- имеет процент надбавки к стоимости изготовления (например, Е = +10%);
- содержит справочник марок стали (например, 30ХГСА, 40ХН2МА).

На заказ влияет только группа (через процент надбавки). Марка стали —
информационное поле, на цену не влияет.

Группа и марка на заказе опциональны. Для труб/патрубков надбавка
начисляется на всю базу (резьбы + металл).

Snapshot надбавки: при создании/редактировании заказа в БД сохраняется
applied_markup_percent — на случай, если админ потом изменит процент в
справочнике, старые заказы не пересчитываются.

Дополнительные работы

Работы типа «покраска», «нарезка резьбы» и т.п. Их цена указана за тонну
продукции. При расчёте:

- вес берётся из workpieceWeightKg (для труб/патрубков — из
  orderTotalWeightKg);
- минимум 100 кг — заказ меньше 100 кг считается как 100 кг;
- надбавка за группу прочности на доп. работы не начисляется.

Компании

Заказ всегда привязан к компании. При создании заказа можно ввести имя
существующей компании (автокомплит через <datalist>) или создать новую.
В КП нельзя смешивать заказы разных компаний.

---

Стек
----

| Слой               | Технология                                      |
|--------------------|-------------------------------------------------|
| Язык               | Java 17                                         |
| Framework          | Spring Boot 4.0.2                               |
| Web                | Spring MVC + Thymeleaf                          |
| Безопасность       | Spring Security + Keycloak (OAuth2 client)      |
| БД                 | PostgreSQL 15                                   |
| ORM                | Spring Data JPA + Hibernate 7                   |
| Миграции           | Liquibase                                       |
| Очереди            | Kafka (интеграция в разработке)                 |
| Фронт              | Thymeleaf + Bootstrap 5.3.3 + Hotwired Turbo 8  |
| Сборка             | Maven                                           |
| Тесты              | JUnit 5, Mockito 5.20, Testcontainers, Spring Boot Test |
| Контейнеризация    | Docker Compose                                  |

---

Архитектура
-----------

Классическая слоистая:

HTTP ──► Controller ──► Service ──► Repository ──► PostgreSQL
│            │
│            └─► Mapper ─► DTO (наружу)
│
└─► Thymeleaf-шаблон (фрагмент) ──► Layout

Правила

- Controller — только HTTP. Не содержит бизнес-логики. Принимает DTO,
  возвращает имя шаблона (или JSON для @ResponseBody-эндпоинтов).
- Service — вся бизнес-логика. Ходит только в репозитории, не в другие
  сервисы того же уровня (чтобы не плодить циклы).
- Repository — Spring Data JPA. Для read-методов, отдающих сущности с
  ленивыми связями, используется @EntityGraph.
- DTO — на границах. Сущности наружу не отдаём (кроме случаев, где так
  исторически сложилось).
- Mapper — рукописный (OrderMapper), без MapStruct.

Сессионное состояние

Корзина КП хранится в HttpSession под ключом commercialOffer как
Set<Long> — набор id заказов.

XOR-constraint

На двух таблицах (orders, production_records) есть check-constraint
check_product_xor: ровно один из (coupling_id, adapter_id,
external_thread_id) непустой. При добавлении нового типа продукции обновлять
обе таблицы.

---

Структура проекта
-----------------

src/main/java/ProjectForJob/example/Job/
├── DataTransferObject/        # DTO + DTO для Kafka
├── MVCcontrollers/            # HTTP-контроллеры
│   ├── ForOrders/             # Заказы, КП
│   ├── Handbook/              # Справочники
│   └── *.java                 # Home, Login, ProductionRecord, Reports
├── entityJob/                 # JPA-сущности
│   ├── ForOrders/             # Order, Company, ProductionRecord, PipeProductKind
│   └── Handbook/              # Coupling, Adapter, ExternalThread, Employee, ...
├── repositories/              # Spring Data JPA
├── services/                  # Бизнес-логика
│   ├── Handbook/              # Сервисы справочников
│   ├── mapping/               # Ручные мапперы
│   └── OrderService.java      # Основной сервис
└── util/                      # Утилиты без состояния
├── OrderCostCalculator.java       # Формула цены для муфт/переводников
├── PipeOrderCostCalculator.java   # Формула цены для труб/патрубков
└── WorkpieceCalculator.java       # Формула веса заготовки

src/main/resources/
├── db/changelog/              # Liquibase changelog'и
│   ├── db.changelog-master.yaml
│   └── 2026/04/*.xml
├── static/
│   ├── css/styles.css         # Всё, что кастомное
│   ├── js/                    # Tooltip, forms, animations
│   └── images/
├── templates/
│   ├── welcomeToHome/
│   │   ├── layout.html        # Единая обёртка (см. Фронтенд)
│   │   └── welcome.html
│   └── fragments/             # Фрагменты для Turbo
│       ├── orders/            # Заказы
│       ├── couplings/         # Справочник муфт
│       ├── adapters/          # Справочник переводников
│       ├── external-threads/  # Справочник наружных резьб
│       ├── steel-groups/      # Справочник групп прочности
│       ├── production-records/# Журнал производства
│       └── ...

---

Запуск
------

Требования

- Docker + Docker Compose
- Java 17 (для локальной сборки без Docker)
- Maven (или ./mvnw, если добавите wrapper)

Через Docker (рекомендуется)

docker compose up -d --build

После старта:

- Приложение: http://localhost:8080
- PostgreSQL: localhost:5432 (см. docker-compose.yml для креденшелов)

Проверить статус:

docker ps
docker logs spring_app --tail 100

Приложение выведет Started JobApplication in X seconds. Liquibase
автоматически применит все миграции.

Что делать после изменения Java-кода

Обязательно пересобрать образ:

docker compose up -d --build spring_app

Если изменения только в templates/ или static/ — пересборка тоже нужна,
потому что они пакуются в JAR.

Подключение к БД

docker exec -i postgres psql -U jobuser -d jobdb -c '\dt'

---

Ключевая бизнес-логика
----------------------

Формула веса заготовки

WorkpieceCalculator.calculateWeight:

M = π × ρ × S × (D - S) × L / 1_000_000

где:

- ρ = 7850 кг/м³ (плотность стали),
- S — толщина стенки, мм = (D_наруж − D_внутр) / 2 для муфт/переводников;
  для труб/патрубков S — задаётся в заказе напрямую (wallThicknessMm),
- D — наружный диаметр, мм (для труб/патрубков берётся из
  externalThread.conditionalDiameter, "73" → 73.0),
- L — общая длина, м.

Результат округляется до 2 знаков.

Обратная формула (OrderService.lengthFromWeight) — для пересчёта
тоннаж → метраж:

L = M × 1_000_000 / (π × ρ × S × (D − S))

Формула цены заказа для муфт/переводников

OrderCostCalculator.calculate:

baseCost     = P_unit × Q
markupCost   = baseCost × (M_pct / 100)
productTotal = baseCost + markupCost

W_billing    = max(W_total, 100)       # минимум 100 кг
worksCost    = Σ_i ( W_i × W_billing / 1000 )

totalCost    = productTotal + worksCost
→ округление до scale=2, HALF_UP

где:

- P_unit — цена изготовления 1 шт (manufacturingCost),
- Q — количество,
- M_pct — snapshot надбавки за группу прочности,
- W_total — теоретический вес заготовки, кг,
- W_i — цена i-й доп. работы за тонну.

Формула цены заказа для труб/патрубков

PipeOrderCostCalculator.calculate:

threadsCost  = Q × 2 × manufacturingCost
metalCost    = pricePerTon × weight_kg / 1000
baseCost     = threadsCost + metalCost
markupCost   = baseCost × markup_pct / 100
productTotal = baseCost + markupCost

billingW     = max(weight_kg, 100)
worksCost    = Σ W_i × billingW / 1000

totalCost    = productTotal + worksCost
→ округление до scale=2, HALF_UP

где:

- Q — количество штук (для PIPE — оценочное, см. ниже),
- manufacturingCost — цена одной нарезки из справочника резьб,
- pricePerTon — цена металла за тонну, задаёт менеджер.

Надбавка за группу прочности — на весь baseCost (резьбы + металл).
Доп.работы — за тонну, минимум 100 кг, надбавка не начисляется.

Никогда не меняйте формулы вне этих двух классов. При изменении —
обновляйте OrderCostCalculatorTest / PipeOrderCostCalculatorTest.

Логика PIPE/NIPPLE в OrderService.applyPipeOrNipple

Для NIPPLE (патрубок): quantity вводит менеджер,
totalLength_m = pieceLengthMm × quantity / 1000, вес считается через
WorkpieceCalculator.

Для PIPE (труба): quantity не вводится. Менеджер задаёт либо метраж,
либо тоннаж (второе считается на лету в JS и сохраняется в БД на всякий
случай). В сервисе:

если задан orderTotalLengthMeters → weight = WorkpieceCalculator.calculate(...);
если задан orderTotalWeightKg → totalLength = lengthFromWeight(...);
quantity = ceil(totalLength / averagePipeLengthM), где
averagePipeLengthM — дефолт 10.0, менеджер может задать своё.

Это оценка — реальная длина плети у каждой трубы разная. Оценка нужна
исключительно для отображения и расчёта threadsCost = Q × 2 × cost.

Snapshot надбавки

При создании/редактировании заказа OrderService.applyStrengthAndGrade
фиксирует appliedMarkupPercent в заказе. Даже если админ потом изменит
процент у группы в справочнике — старые заказы не пересчитываются.

---

Фронтенд
--------

Один layout + фрагменты

Приложение не использует отдельные страницы. Есть один layout.html,
внутри которого <turbo-frame id="app">. Все страницы — фрагменты
Thymeleaf, вставляемые в этот frame.

┌───────────────────────────────────────────┐
│ layout.html                                │
│ ┌───────────────────────────────────────┐ │
│ │ navbar (фрагмент)                     │ │
│ ├───────────────────────────────────────┤ │
│ │ <turbo-frame id="app">                │ │
│ │   {{contentTemplate}}  ← фрагмент     │ │
│ │ </turbo-frame>                        │ │
│ └───────────────────────────────────────┘ │
└───────────────────────────────────────────┘

Каждый контроллер возвращает welcomeToHome/layout и добавляет в модель
contentTemplate — строку вида fragments/orders/waiting :: content.

Hotwired Turbo

Навигация — через Turbo. Клики по внутренним ссылкам с
data-turbo-frame="app" подгружают только фрагмент. События, на которые
реагирует JS:

DOMContentLoaded — первая загрузка;
turbo:load — полная навигация;
turbo:frame-load с target.id === 'app' — подгрузка фрагмента.

Все инициализации DOM — на этих трёх событиях. Функция initPageScripts
в app.js — единая точка входа.

Соглашения по JS

Делегирование — вешаем слушатели на document.
Флаг dataset.wired = 'true' — чтобы не навесить слушатель дважды.
Никаких inline-обработчиков — всё через addEventListener.
Модалки — кастомные (.hb-modal-overlay). Bootstrap modal конфликтует
со stacking context от .app-panel.

CSS

Весь кастомный CSS — в одном styles.css. Bootstrap 5 — только сетка и
утилиты. Компоненты собственные: .btn-glass, .btn-mini, .hb-field,
.hb-radio, .order-card, .product-icon-tile--*, .journal-entry.

Классы иконок по редкости:

.product-icon-tile--coupling — фиолетовый (epic)
.product-icon-tile--adapter — оранжевый (legendary)
.product-icon-tile--pipe — зелёный (uncommon)

Версионируйте CSS/JS в layout.html через ?v=N.

---

Тестирование
------------

mvn test

Что уже покрыто

OrderCostCalculatorTest — единственный полноценный unit-тест.
Покрывает формулу цены для муфт/переводников.

Что НЕ покрыто (и это надо править)

Backend, критично:

PipeOrderCostCalculator — не покрыт. Новая формула уйдёт в прод без
тестов. Приоритет — как у OrderCostCalculatorTest.

OrderService.applyPipeOrNipple — не покрыт. Ветки PIPE vs NIPPLE,
пересчёт метраж↔тоннаж, quantity = ceil(...), edge-кейсы (оба поля null,
оба заданы, кривой conditionalDiameter).

OrderService.createOrder / updateOrder — нет тестов. Там валидация,
snapshot надбавки, расчёт заготовки, развилка на 4 типа продукции.

OrderService.applyStrengthAndGrade — 4 кейса (группа без марки / марка
без группы / марка не из группы / обе заданы), все не покрыты.

WorkpieceCalculator — нет тестов. Формула веса не проверяется.

ProductionRecordMvcController — фильтры по датам/сотруднику/станку/
типу продукции не покрыты.

OrderMapper — маппинг новых полей (externalThreadName,
pipeProductKind, wallThicknessMm, orderTotalWeightKg и др.) не
проверяется.

OrderRepository / ProductionRecordRepository — @EntityGraph не
проверяется интеграционным тестом.

DTO-валидация — isProductValid (теперь XOR на 3 поля),
isQuantityValid, isPipeKindValid — не покрыты.

Frontend:

JS-тестов нет. Для orders-form.js — не критично, для tooltip.js —
стоит написать smoke через Playwright.

Интеграционные (Testcontainers):

Накат Liquibase на чистую БД.
OrderRepository + @EntityGraph — проверка что связи не lazy.
OrderService.createOrder — сквозной путь для 4 типов продукции.

Правило обновления тестов

При любой правке, которая меняет поведение — сначала переписать тесты,
потом код. Если тест красный и ты не уверен в причине — разбирайся,
не «поправляй по-быстрому».

Перед merge любой фичи

mvn test — зелёные.
Smoke-тест через UI (создать заказ каждого типа → отредактировать →
статусы → КП → удалить).
Проверка в БД после изменений.
Если менял миграции — прогон на чистой БД (docker compose down -v +
up --build).

---

Соглашения
----------

Java

Пакеты — по слоям, не по фичам.
Сущности: *Entity, кроме OrderEntity / CompanyEntity.
Сервисы справочников — в services/Handbook/.
Ходить между сервисами одного уровня запрещено, только через
репозитории.
Формулы и расчёты — в util/-классах без состояния.
Всё, что меняет состояние в БД, — в @Transactional.

Liquibase

Все миграции — в db/changelog/YYYY/MM/DD-NN-changelog.xml.
Регистрируются в db.changelog-master.yaml через include.
Не редактировать накатанные changelog'и — только добавлять новые.
При изменении XOR-constraint'ов использовать DROP CONSTRAINT IF EXISTS +
ADD CONSTRAINT.

Git

Осмысленные коммиты, одна фича = один PR.
Не пушить сломанные миграции — сначала локально прогнать
docker compose up --build.

---

Технический долг
----------------

Список того, что известно и осознанно отложено. Разделы отсортированы
по критичности.

Backend, высокий риск

PipeOrderCostCalculator не покрыт тестами. Формула новая, уйдёт в
прод без unit-теста. Приоритет — как у OrderCostCalculatorTest.

OrderService.applyPipeOrNipple не покрыт тестами. Ветки PIPE vs
NIPPLE, пересчёт метраж↔тоннаж, quantity = ceil(...), обработка
orderTotalLengthMeters vs orderTotalWeightKg. Edge-кейсы не проверены.

lengthFromWeight — обратная формула живёт в OrderService и
дублирует логику WorkpieceCalculator. Если поменять прямую формулу —
обратная молча разъедется. Как починить: вынести обе формулы в
WorkpieceCalculator рядом (calculateWeight / calculateLength).

fillWorkpieceData (для муфт/переводников) принимает double. Если
у муфты outerDiameterMm == null — NPE при unboxing. Как починить:
перейти на Double с явной валидацией.

OrderService.applyStrengthAndGrade не покрыт тестами. См. раздел
Тестирование.

OrderService.resolveCompany — без нормализации имён. Дубли при гонке.
Плюс "ООО Ромашка" и "ООО  Ромашка" — разные компании. Как
починить: UNIQUE по LOWER(name) + retry на
DataIntegrityViolationException.

N+1 в справочнике групп. findAllByOrderByCodeAsc через @EntityGraph
подтягивает все марки всех групп. Форме заказа grades не нужны — ей нужны
только группы.

Backend, средний риск

HHH90003004 warning. Пагинация + @ManyToMany (additionalWorks) +
@EntityGraph — Hibernate применяет LIMIT в памяти. Проявляется на заказах
и журнале. Как починить: two-step fetching или @BatchSize.

Дубли API-эндпоинтов резьб. /api/external-threads/by-type,
/api/couplings/*, /api/adapters/* — есть и в OrderController, и в
ProductionRecordMvcController. Как починить: общий
CatalogApiController с /api/catalog/....

ProductionRecordMvcController.addReferenceData теперь грузит
externalThreadTypes. Проверить, не осталось ли дублирующего
model.addAttribute в listAll.

PipeOrderCostCalculator и OrderCostCalculator — параллельные
калькуляторы. Развилка в OrderService.calculateTotalCost. Со временем —
слить в один с параметром.

Kafka-интеграция закомментирована в OrderService.startProduction.
Ждёт готовности сервиса производства.

OrderMapper.toStartedEvent содержит поля, которые сейчас никто не
читает (Kafka мёртвая). После включения — синхронизировать с DTO события.

CouplingEntity.getName() возвращает type + " " + conditionalDiameter
без .trim(). Если type пустой — вылезет " 73" с ведущим пробелом.

SteelStrengthGroupService.save — инвариант дефолтной группы. Защита
в коде, но прямой SQL может сломать. Как починить: триггер в БД или
сервисный тест, ловящий регрессию.

SteelStrengthGroupService.deleteById — прямой deleteById. При
наличии марок в группе — DataIntegrityViolationException с некрасивым
сообщением.

Backend, низкий риск

fillFormRefs в OrderController грузит все муфты и переводники
(couplingService.findAll() / pipeAdapterService.findAll()). Форма их не
использует (AJAX-каскады). Мусорная нагрузка.

OrderUpdateDto.isProductValid валидирует XOR
couplingId/adapterId/externalThreadId, но не проверяет
согласованность strengthGroupId и steelGradeId. Проверка живёт в сервисе
и кидает IllegalArgumentException — 500 вместо нормальной ошибки формы.

OrderEntity.getProductType() / ProductionRecordEntity.getProductType()
дублируют логику. Можно вынести в util.

ProductionRecordListDto — при добавлении новых типов продукции
придётся обновлять toListDto в контроллере.

Frontend

Тултип PIPE/NIPPLE кривой. Пользователь подтвердил — переделать.
Сейчас три ветки в tooltip.js (updateContent): coupling / adapter /
pipe. Для pipe показывает «толщина, метраж, вес» — визуально некрасиво.

orders-form.js разросся. initProductTypeToggle (для журнала) и
initPipeNippleBlocks (для заказа) разделены хаковым условием «если есть
typePipe — я не работаю». Как починить: два файла
(orders-form.js / journal-form.js) или явный data-page.

Два блока PIPE/NIPPLE в форме заказа, один общий в форме журнала.
Разные подходы к одной сущности. Унифицировать.

В форме заказа два скрытых externalThreadId (pipeExternalThreadId
и nippleExternalThreadId), оба disable через setBlockEnabled. Работает,
но некрасиво. Правильно: один hidden + JS пишет в него из активного
каскада.

edit.html — th:field="*{externalThreadId}" на двух hidden, оба
заполняются одним значением. Работает, но хрупко.

Кастомная модалка вместо Bootstrap — правильно (конфликт со stacking
context), но каждая новая модалка — копипаста.

tooltip.js переприкрепляет контейнер к document.body при каждом
init. Хрупкое место.

В шаблонах остались inline-стили.

Дубликат .hb-field с одним и тем же th:field — Spring возьмёт
последнее значение, тихо теряя первое. Проверять формы.

Версионирование CSS/JS через ?v=N — вручную.

Данные и миграции

Цепочка новых миграций (все физически в 2026/04/, id — с датами
2026-10-XX):

05-10 — создание pipe_products_data (откатили, файл оставили)
06-01 — drop pipe_products_data + create external_threads_data
07-01 — 8 полей PIPE/NIPPLE в orders
08-01 — пересоздание check_product_xor в orders
(добавлен external_thread_id)
09-01 — pipe_product_kind + external_thread_id в
production_records, пересоздание XOR там же

Семантика additionalWork.price — «цена за тонну». В БД могли
остаться старые значения «цена за единицу». Пересмотреть вручную.

Данные нагрузочного теста (~1000 заказов) в БД. У них
workpiece_* = NULL. Формула цены работает, UI показывает «не рассчитана».
Тестовые данные.

Что делать с этим списком

Не закапывайся. 15 минут — чини сразу. 2 часа — заводи тикет.

Читай перед рефакторингом. Трогаешь OrderService — прочти
applyStrengthAndGrade и applyPipeOrNipple. Трогаешь CSS — про stacking
context.

Дополняй. Нашёл новое — в файл, не «запомню».

---

Для AI-ассистента
-----------------

Если ты AI-помощник и работаешь с этим проектом впервые — читай в таком
порядке:

1. Этот README — целиком.
2. OrderService.java — центральный сервис. Понимаешь заказы — понимаешь
   проект.
3. OrderCostCalculator.java + PipeOrderCostCalculator.java +
   WorkpieceCalculator.java — все формулы тут.
4. ExternalThreadEntity.java + ExternalThreadService — справочник наружных
   резьб (только для труб/патрубков).
5. OrderController.java — карта эндпоинтов для заказов.
6. ProductionRecordMvcController.java — журнал производства.
7. fragments/orders/*.html, fragments/production-records/*.html,
   fragments/external-threads/*.html — фронтенд.
8. layout.html + app.js — Turbo-навигация и инициализация скриптов.

Ключевые правила для работы

Не менять формулы вне OrderCostCalculator /
PipeOrderCostCalculator / WorkpieceCalculator.

PIPE/NIPPLE используют PipeOrderCostCalculator, муфты/переводники —
OrderCostCalculator. Развилка в OrderService.calculateTotalCost по
pipeProductKind != null.

quantity для PIPE — оценочное,
ceil(totalLength / averagePipeLengthM). Не вводится менеджером напрямую.

check_product_xor есть в двух таблицах (orders,
production_records). При добавлении новых типов продукции обновлять обе.

Не путать CouplingEntity (внутренняя резьба) и ExternalThreadEntity
(наружная). Разные справочники.

Не менять накатанные changelog'и — только добавлять новые.

Все инициализации JS — через initPageScripts в app.js, идемпотентно
(флаг dataset.wired).

Слушатели на document, не на элементах.

Сервисы не ходят в другие сервисы — только в репозитории.

Сущности наружу не отдаём — только DTO.

Snapshot (например, appliedMarkupPercent) не пересчитываем задним
числом.

List.of() в JPA-сущности не кладём — Hibernate merge() вызывает
clear(), будет UnsupportedOperationException.

Что спрашивать у владельца, прежде чем менять

Любая правка формулы цены/веса → попросить явную формулу.
Любая правка схемы БД → согласовать подход к миграции.
Любая правка UI заказов → уточнить, не сломает ли это КП.
Любая правка справочника групп прочности → помнить про snapshot.
Любая правка тултипов PIPE/NIPPLE → согласовать дизайн (пользователь
планирует редизайн).

Что запланировано на следующие сессии

Редизайн тултипа PIPE/NIPPLE (запрос пользователя).
Рефакторинг orders-form.js — разнести заказы и журнал по файлам.
Унификация блоков PIPE/NIPPLE в форме заказа (один блок вместо двух).
Тесты: PipeOrderCostCalculatorTest, OrderService.applyPipeOrNipple,
WorkpieceCalculatorTest.
Проверка КП — PIPE/NIPPLE заказы в коммерческом предложении
(проверить CommercialOfferController на lazy-init).
Отчёты — ReportController для PIPE/NIPPLE (по сотруднику —
priceForEmployee за штуку; по станку —
manufacturingCost × 2 × quantity).