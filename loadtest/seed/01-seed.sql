-- =====================================================================
-- SEED: наполняет БД тестовыми данными для нагрузочного теста
-- =====================================================================
BEGIN;

-- ---------------------------------------------------------------------
-- 1. Очистка старых тестовых данных
-- ---------------------------------------------------------------------
-- TRUNCATE удаляет все строки и сбрасывает счётчики id.
-- CASCADE нужен, потому что таблицы связаны FK-ами.
-- ВАЖНО: не трогаем app_user/roles/user_roles — там логины Spring Security.
TRUNCATE order_additional_work, production_records, orders,
    couplings_data, pipe_adapters_data, list_employees, list_machines,
    companies, list_additional_work
    RESTART IDENTITY CASCADE;

-- ---------------------------------------------------------------------
-- 2. Справочники
-- ---------------------------------------------------------------------

-- 20 сотрудников: "Сотрудник #1", "Сотрудник #2", ...
INSERT INTO list_employees (name)
SELECT 'Сотрудник #' || g
FROM generate_series(1, 20) g;

-- 10 станков
INSERT INTO list_machines (name)
SELECT 'Станок #' || g
FROM generate_series(1, 10) g;

-- 6 видов дополнительных работ
INSERT INTO list_additional_work (name, additional_cost, description)
VALUES ('Покраска', 500, 'Антикоррозийное покрытие'),
       ('Гравировка', 800, 'Нанесение маркировки'),
       ('Нарезка резьбы', 1200, 'Доп. резьбовой участок'),
       ('Термообработка', 2500, 'Закалка / отпуск'),
       ('Шлифовка', 700, 'Финишная шлифовка'),
       ('Полировка', 600, 'Декоративная полировка');

-- ---------------------------------------------------------------------
-- 3. Компании: 200 штук
-- ---------------------------------------------------------------------
INSERT INTO companies (name)
SELECT 'ООО «Компания-' || g || '»'
FROM generate_series(1, 200) g;

-- ---------------------------------------------------------------------
-- 4. Муфты: 4 типа × 7 диаметров = 28 записей
-- ---------------------------------------------------------------------
-- CROSS JOIN склеивает каждый тип с каждым диаметром.
INSERT INTO couplings_data
(type, conditional_diameter, length_mm, weight_kg,
 outer_diameter_mm, inner_diameter_mm,
 manufacturing_cost, price_for_employee, standart)
SELECT t.type,
       d.d::varchar,
       100 + d.d,                             -- длина растёт с диаметром
       ROUND((1.5 + d.d * 0.05)::numeric, 2), -- вес ~5-9 кг
       d.d + 20,                              -- наружный > условного
       d.d - 5,                               -- внутренний < условного
       5000 + d.d * 100,                      -- цена изготовления
       ROUND((4000 + d.d * 30)::numeric, 2),  -- цена для работника
       'ГОСТ 633-80'
FROM (VALUES ('НКТ'), ('НКТМ'), ('ОТТМ'), ('ОТТГ')) t(type)
         CROSS JOIN (VALUES (60), (73), (89), (102), (114), (127), (146)) d(d);

-- ---------------------------------------------------------------------
-- 5. Переводники: 2×3 × 2×3 = 36 записей
-- ---------------------------------------------------------------------
INSERT INTO pipe_adapters_data
(first_side_type, first_side_diameter,
 second_side_type, second_side_diameter,
 length_mm, weight_kg, outer_diameter_mm, inner_diameter_mm,
 manufacturing_cost, price_for_employee, standart)
SELECT
    a1.t, a2.d::varchar,
    b1.t, b2.d::varchar,
       150,
       2.5,
       80,
       60,
       6000,
       5000,
       'ГОСТ 633-80'
FROM (VALUES ('НКТ'), ('НКТМ')) a1(t)
         CROSS JOIN (VALUES (60), (73), (89)) a2(d)
         CROSS JOIN (VALUES ('ОТТМ'), ('ОТТГ')) b1(t)
         CROSS JOIN (VALUES (60), (73), (89)) b2(d);

-- ---------------------------------------------------------------------
-- 6. Заказы: 10 600 штук
--    300 WAITING + 300 IN_PRODUCTION + 10 000 COMPLETED
-- ---------------------------------------------------------------------
-- Логика:
--   - company_id — случайная компания из 200
--   - coupling_id XOR adapter_id (из-за check_product_xor)
--     Если случайное число < 0.6 → муфта, иначе → переводник
--   - status определяется по номеру строки: 1..300 WAITING, 301..600 PROD, остальные COMPLETED
--   - workpiece_* заполняем только для тех, кто был в производстве (n > 300)
WITH company_ids AS (SELECT array_agg(id) AS arr FROM companies),
     coupling_ids AS (SELECT array_agg(id) AS arr FROM couplings_data),
     adapter_ids AS (SELECT array_agg(id) AS arr FROM pipe_adapters_data),
     seq AS (SELECT g        AS n,
                    random() AS r_comp,     -- для выбора компании
                    random() AS r_type,     -- для выбора муфта/переводник
                    random() AS r_prod,     -- для выбора конкретного изделия
                    random() AS r_deadline, -- для дедлайна
                    random() AS r_qty,      -- для количества
                    random() AS r_cost      -- для цены
             FROM generate_series(1, 10600) g)
INSERT
INTO orders
(company_id, coupling_id, adapter_id, quantity, total_cost,
 deadline, created_at, status,
 workpiece_outer_diameter, workpiece_wall_thickness,
 workpiece_length_meters, workpiece_weight_kg)
SELECT company_ids.arr[1 + floor(seq.r_comp * array_length(company_ids.arr, 1))::int],

       -- XOR: ровно одно из двух заполнено, второе NULL
       CASE
           WHEN seq.r_type < 0.6
               THEN coupling_ids.arr[1 + floor(seq.r_prod * array_length(coupling_ids.arr, 1))::int]
           END,
       CASE
           WHEN seq.r_type >= 0.6
               THEN adapter_ids.arr[1 + floor(seq.r_prod * array_length(adapter_ids.arr, 1))::int]
           END,

       1 + floor(seq.r_qty * 50)::int,                  -- 1..50 шт
       ROUND((5000 + seq.r_cost * 200000)::numeric, 2), -- 5k..205k руб

       -- у 85% заказов есть дедлайн, у 15% — NULL
       CASE
           WHEN seq.r_deadline < 0.85
               THEN CURRENT_DATE + (floor(seq.r_deadline * 90)::int - 30) -- ±30 дней вокруг сегодня
           END,

       NOW() - (random() * INTERVAL '180 days'),        -- создан в пределах 180 дней

       CASE
           WHEN seq.n <= 300 THEN 'WAITING'
           WHEN seq.n <= 600 THEN 'IN_PRODUCTION'
           ELSE 'COMPLETED'
           END,

       -- заготовка — только у тех, кто был в производстве
       CASE WHEN seq.n > 300 THEN ROUND((60 + random() * 60)::numeric, 1) END,
       CASE WHEN seq.n > 300 THEN ROUND((5 + random() * 10)::numeric, 1) END,
       CASE WHEN seq.n > 300 THEN ROUND((0.5 + random() * 3)::numeric, 2) END,
       CASE WHEN seq.n > 300 THEN ROUND((5 + random() * 50)::numeric, 2) END
FROM seq,
     company_ids,
     coupling_ids,
     adapter_ids;

-- ---------------------------------------------------------------------
-- 7. Доп.
--    Первый проход: каждый 3-й заказ получает 1 доп.
--    Второй проход: каждый 7-й заказ получает ещё 1 (чтобы у некоторых было >1)
-- ---------------------------------------------------------------------
INSERT INTO order_additional_work (order_id, additional_work_id)
SELECT o.id, (SELECT id FROM list_additional_work ORDER BY random() LIMIT 1)
FROM orders o
WHERE o.id % 3 = 0;

INSERT INTO order_additional_work (order_id, additional_work_id)
SELECT o.id, (SELECT id FROM list_additional_work ORDER BY random() LIMIT 1)
FROM orders o
WHERE o.id % 7 = 0;

-- ---------------------------------------------------------------------
-- 8. Журнал производства: 10 000 записей
-- ---------------------------------------------------------------------
WITH emp_ids AS (SELECT array_agg(id) AS arr FROM list_employees),
     mach_ids AS (SELECT array_agg(id) AS arr FROM list_machines),
     cp_ids AS (SELECT array_agg(id) AS arr FROM couplings_data),
     ad_ids AS (SELECT array_agg(id) AS arr FROM pipe_adapters_data),
     seq AS (SELECT random() AS r_type,    -- муфта/переводник
                    random() AS r_emp,     -- сотрудник
                    random() AS r_mach,    -- станок
                    random() AS r_prod,    -- конкретное изделие
                    random() AS r_qty,     -- количество
                    random() AS r_date,    -- дата производства
                    random() AS r_created, -- дата создания записи
                    random() AS r_comment  -- будет ли комментарий
             FROM generate_series(1, 10000))
INSERT
INTO production_records
(production_date, coupling_id, adapter_id,
 quantity, employee_id, machine_id, comment, created_at)
SELECT CURRENT_DATE - floor(seq.r_date * 365)::int, -- за последний год
       CASE
           WHEN seq.r_type < 0.6
               THEN cp_ids.arr[1 + floor(seq.r_prod * array_length(cp_ids.arr, 1))::int]
           END,
       CASE
           WHEN seq.r_type >= 0.6
               THEN ad_ids.arr[1 + floor(seq.r_prod * array_length(ad_ids.arr, 1))::int]
           END,
       1 + floor(seq.r_qty * 50)::int,
       emp_ids.arr[1 + floor(seq.r_emp * array_length(emp_ids.arr, 1))::int],
       mach_ids.arr[1 + floor(seq.r_mach * array_length(mach_ids.arr, 1))::int],
       CASE
           WHEN seq.r_comment < 0.3
               THEN 'Комментарий #' || floor(seq.r_comment * 10000)::int
           END,
       NOW() - (seq.r_created * INTERVAL '365 days')
FROM seq,
     emp_ids,
     mach_ids,
     cp_ids,
     ad_ids;

COMMIT;

-- ---------------------------------------------------------------------
-- 9. Отчёт: что получилось
-- ---------------------------------------------------------------------
SELECT 'orders total' AS what, COUNT(*)::text AS cnt
FROM orders
UNION ALL
SELECT 'orders WAITING', COUNT(*)::text
FROM orders
WHERE status = 'WAITING'
UNION ALL
SELECT 'orders IN_PRODUCTION', COUNT(*)::text
FROM orders
WHERE status = 'IN_PRODUCTION'
UNION ALL
SELECT 'orders COMPLETED', COUNT(*)::text
FROM orders
WHERE status = 'COMPLETED'
UNION ALL
SELECT 'order_additional_work', COUNT(*)::text
FROM order_additional_work
UNION ALL
SELECT 'production_records', COUNT(*)::text
FROM production_records
UNION ALL
SELECT 'couplings_data', COUNT(*)::text
FROM couplings_data
UNION ALL
SELECT 'pipe_adapters_data', COUNT(*)::text
FROM pipe_adapters_data
UNION ALL
SELECT 'companies', COUNT(*)::text
FROM companies
UNION ALL
SELECT 'list_employees', COUNT(*)::text
FROM list_employees
UNION ALL
SELECT 'list_machines', COUNT(*)::text
FROM list_machines;