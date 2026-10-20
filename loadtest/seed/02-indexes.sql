-- =====================================================================
-- Индексы под нагрузочные фильтры
-- =====================================================================

-- заказы: фильтруем и сортируем по status + created_at
CREATE INDEX IF NOT EXISTS idx_orders_status_created   ON orders(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_company          ON orders(company_id);
CREATE INDEX IF NOT EXISTS idx_orders_deadline         ON orders(deadline);

-- журнал: сортируем по created_at, фильтруем по employee/machine/coupling/adapter
CREATE INDEX IF NOT EXISTS idx_records_created         ON production_records(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_records_production_date ON production_records(production_date DESC);
CREATE INDEX IF NOT EXISTS idx_records_employee        ON production_records(employee_id);
CREATE INDEX IF NOT EXISTS idx_records_machine         ON production_records(machine_id);
CREATE INDEX IF NOT EXISTS idx_records_coupling        ON production_records(coupling_id);
CREATE INDEX IF NOT EXISTS idx_records_adapter         ON production_records(adapter_id);

-- справочники: поиск по типу
CREATE INDEX IF NOT EXISTS idx_couplings_type          ON couplings_data(type);
CREATE INDEX IF NOT EXISTS idx_adapters_first_side     ON pipe_adapters_data(first_side_type);

-- обновляем статистику, чтобы Postgres сразу знал про новые данные
ANALYZE orders;
ANALYZE production_records;
ANALYZE couplings_data;
ANALYZE pipe_adapters_data;