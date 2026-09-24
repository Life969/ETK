/**
 * orders-form.js
 * Инициализация страниц заказов: формы create/edit + waiting
 * Работает на turbo:frame-load через layout.html
 */
window.OrdersForm = (function () {

    function qsa(sel, root) { return Array.from((root || document).querySelectorAll(sel)); }

    // ================= Тип продукции: муфта / переводник =================
    function initProductTypeToggle() {
        const radioCoupling = document.getElementById('typeCoupling');
        const radioAdapter = document.getElementById('typeAdapter');
        if (!radioCoupling || !radioAdapter) return;
        if (radioCoupling.dataset.wired === 'true') return;
        radioCoupling.dataset.wired = 'true';

        const couplingBlock = document.getElementById('couplingBlock');
        const adapterBlock = document.getElementById('adapterBlock');
        const couplingIdHidden = document.getElementById('couplingIdHidden');
        const adapterIdHidden = document.getElementById('adapterIdHidden');

        function toggle() {
            if (radioCoupling.checked) {
                couplingBlock.style.display = 'block';
                adapterBlock.style.display = 'none';
                if (adapterIdHidden) adapterIdHidden.value = '';
            } else if (radioAdapter.checked) {
                couplingBlock.style.display = 'none';
                adapterBlock.style.display = 'block';
                if (couplingIdHidden) couplingIdHidden.value = '';
            } else {
                couplingBlock.style.display = 'none';
                adapterBlock.style.display = 'none';
            }
        }

        radioCoupling.addEventListener('change', toggle);
        radioAdapter.addEventListener('change', toggle);
        toggle();
    }

    // ================= Селекты муфт =================
    function initCouplingSelects() {
        const typeSelect = document.getElementById('couplingTypeSelect');
        const diamSelect = document.getElementById('couplingDiameterSelect');
        const idHidden = document.getElementById('couplingIdHidden');
        if (!typeSelect || !diamSelect) return;
        if (typeSelect.dataset.initialized === 'true') return;
        typeSelect.dataset.initialized = 'true';

        const form = typeSelect.closest('form');
        const initialType = form?.dataset.initialCouplingType || '';
        const initialId = form?.dataset.initialCouplingId || '';

        fetch('/orders/api/couplings/types')
            .then(r => r.json())
            .then(types => {
                typeSelect.innerHTML = '<option value="">-- Выберите тип --</option>';
                types.forEach(t => {
                    const o = new Option(t, t);
                    if (t === initialType) o.selected = true;
                    typeSelect.add(o);
                });
                if (initialType) typeSelect.dispatchEvent(new Event('change'));
            });

        typeSelect.addEventListener('change', function () {
            const type = this.value;
            diamSelect.innerHTML = '<option value="">-- Выберите диаметр --</option>';
            if (idHidden) idHidden.value = '';
            if (!type) { diamSelect.disabled = true; return; }
            diamSelect.disabled = false;

            fetch(`/orders/api/couplings/by-type?type=${encodeURIComponent(type)}`)
                .then(r => r.json())
                .then(list => {
                    list.forEach(c => diamSelect.add(new Option(c.diameter, c.id)));
                    if (initialId) {
                        const opt = Array.from(diamSelect.options).find(o => o.value == initialId);
                        if (opt) {
                            opt.selected = true;
                            if (idHidden) idHidden.value = initialId;
                        }
                    }
                });
        });

        diamSelect.addEventListener('change', function () {
            if (idHidden) idHidden.value = this.value;
        });
    }

    // ================= Селекты переводников =================
    function initAdapterSelects() {
        const typeSelect = document.getElementById('adapterTypeSelect');
        const adapterSelect = document.getElementById('adapterSelect');
        const idHidden = document.getElementById('adapterIdHidden');
        if (!typeSelect || !adapterSelect) return;
        if (typeSelect.dataset.initialized === 'true') return;
        typeSelect.dataset.initialized = 'true';

        const form = typeSelect.closest('form');
        const initialType = form?.dataset.initialAdapterFirstSideType || '';
        const initialId = form?.dataset.initialAdapterId || '';

        function loadAdapters(type, selectedId) {
            if (!type) { adapterSelect.disabled = true; return; }
            adapterSelect.disabled = false;
            fetch(`/orders/api/adapters/by-first-side-type?type=${encodeURIComponent(type)}`)
                .then(r => r.json())
                .then(list => {
                    adapterSelect.innerHTML = '<option value="">-- Выберите переводник --</option>';
                    list.forEach(a => {
                        const o = new Option(a.fullName, a.id);
                        if (a.id == selectedId) o.selected = true;
                        adapterSelect.add(o);
                    });
                    if (selectedId && idHidden) idHidden.value = selectedId;
                });
        }

        if (initialType) {
            typeSelect.value = initialType;
            loadAdapters(initialType, initialId);
        }

        typeSelect.addEventListener('change', function () {
            const type = this.value;
            adapterSelect.innerHTML = '<option value="">-- Сначала выберите тип --</option>';
            if (idHidden) idHidden.value = '';
            loadAdapters(type, null);
        });

        adapterSelect.addEventListener('change', function () {
            if (idHidden) idHidden.value = this.value;
        });
    }

    // ================= Пикер доп. работ =================
    function initWorksPicker() {
        const picker = document.getElementById('worksPicker');
        if (!picker) return;
        if (picker.dataset.initialized === 'true') return;
        picker.dataset.initialized = 'true';

        const chips = qsa('.work-chip', picker);
        const selectedList = picker.querySelector('.hb-works-selected-list');
        const countSpan = picker.querySelector('.selected-count');
        const totalSpan = picker.querySelector('.hb-works-selected-total');
        const searchInput = picker.querySelector('.hb-works-search-input');
        const checkboxes = qsa('.work-hidden-checkbox', picker);

        const cbMap = new Map();
        checkboxes.forEach(cb => cbMap.set(String(cb.value), cb));

        const chipMap = new Map();
        chips.forEach(ch => chipMap.set(String(ch.dataset.workId), ch));

        function fmtPrice(p) {
            return Number(p || 0).toLocaleString('ru-RU') + ' ₽';
        }

        function renderSelected() {
            const selected = chips.filter(ch => ch.classList.contains('selected'));
            countSpan.textContent = selected.length;
            const total = selected.reduce((s, ch) => s + Number(ch.dataset.workPrice || 0), 0);
            totalSpan.textContent = fmtPrice(total);

            selectedList.innerHTML = '';
            if (selected.length === 0) {
                const empty = document.createElement('span');
                empty.className = 'hb-works-selected-empty';
                empty.textContent = 'Ничего не выбрано';
                selectedList.appendChild(empty);
                return;
            }

            selected.forEach(ch => {
                const chip = document.createElement('span');
                chip.className = 'selected-work-chip';
                chip.innerHTML = `
                    <span>${ch.dataset.workName}</span>
                    <span class="chip-price">${fmtPrice(ch.dataset.workPrice)}</span>
                    <button type="button" class="remove-btn" title="Убрать">
                        <i class="bi bi-x"></i>
                    </button>
                `;
                chip.querySelector('.remove-btn').addEventListener('click', (ev) => {
                    ev.stopPropagation();
                    toggleChip(ch.dataset.workId);
                });
                selectedList.appendChild(chip);
            });
        }

        function toggleChip(workId) {
            const chip = chipMap.get(String(workId));
            const cb = cbMap.get(String(workId));
            if (!chip || !cb) return;
            const wasSelected = chip.classList.contains('selected');
            if (wasSelected) {
                chip.classList.remove('selected');
                cb.checked = false;
            } else {
                chip.classList.add('selected');
                cb.checked = true;
            }
            renderSelected();
        }

        chips.forEach(ch => {
            const cb = cbMap.get(String(ch.dataset.workId));
            if (cb && cb.checked) ch.classList.add('selected');
        });
        renderSelected();

        chips.forEach(ch => ch.addEventListener('click', () => toggleChip(ch.dataset.workId)));

        if (searchInput) {
            searchInput.addEventListener('input', function () {
                const q = this.value.trim().toLowerCase();
                chips.forEach(ch => {
                    const name = ch.dataset.workName.toLowerCase();
                    ch.style.display = (!q || name.includes(q)) ? '' : 'none';
                });
            });
        }
    }

    // ================= Waiting: старт производства =================
    function initStartProduction() {
        if (document.body.dataset.startProdWired === 'true') return;
        document.body.dataset.startProdWired = 'true';

        // Делегирование — работает после Turbo-навигаций
        document.addEventListener('click', function (e) {
            const btn = e.target.closest('.start-production-btn');
            if (!btn) return;

            const orderId = btn.dataset.orderId;
            const hasDeadline = btn.dataset.hasDeadline === 'true';

            if (hasDeadline) {
                // Дедлайн есть — сразу отправляем форму POST
                submitStatusForm(orderId, 'IN_PRODUCTION', '/orders/waiting');
            } else {
                // Дедлайна нет — открываем модалку
                const modalEl = document.getElementById('deadlineModal');
                const hiddenId = document.getElementById('orderIdForDeadline');
                const dateInput = document.getElementById('deadlineInput');
                if (!modalEl || !hiddenId) return;

                hiddenId.value = orderId;
                // Минимум — завтра
                const tomorrow = new Date();
                tomorrow.setDate(tomorrow.getDate() + 1);
                dateInput.min = tomorrow.toISOString().split('T')[0];
                dateInput.value = dateInput.min;

                const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });

        // Подтверждение из модалки
        document.addEventListener('click', function (e) {
            const btn = e.target.closest('#confirmDeadlineBtn');
            if (!btn) return;

            const hiddenId = document.getElementById('orderIdForDeadline');
            const dateInput = document.getElementById('deadlineInput');
            const orderId = hiddenId?.value;
            const deadline = dateInput?.value;

            if (!orderId || !deadline) {
                alert('Укажите дедлайн');
                return;
            }

            // AJAX POST на /orders/{id}/set-deadline-and-start
            fetch(`/orders/${orderId}/set-deadline-and-start`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: `deadline=${encodeURIComponent(deadline)}`
            })
                .then(r => r.json())
                .then(res => {
                    if (res.success) {
                        const modalEl = document.getElementById('deadlineModal');
                        bootstrap.Modal.getInstance(modalEl)?.hide();
                        // Обновляем turbo-frame
                        window.Turbo?.visit('/orders/waiting', { action: 'replace' });
                    } else {
                        alert('Ошибка: ' + (res.error || 'неизвестная'));
                    }
                })
                .catch(err => {
                    console.error(err);
                    alert('Ошибка сети');
                });
        });
    }

    // Хелпер: собрать и отправить POST-форму смены статуса
    function submitStatusForm(orderId, newStatus, returnTo) {
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = `/orders/${orderId}/status`;
        form.style.display = 'none';

        const s1 = document.createElement('input');
        s1.type = 'hidden';
        s1.name = 'newStatus';
        s1.value = newStatus;
        form.appendChild(s1);

        const s2 = document.createElement('input');
        s2.type = 'hidden';
        s2.name = 'returnTo';
        s2.value = returnTo;
        form.appendChild(s2);

        // CSRF если нужно
        const csrf = document.querySelector('meta[name="_csrf"]');
        if (csrf) {
            const c = document.createElement('input');
            c.type = 'hidden';
            c.name = '_csrf';
            c.value = csrf.content;
            form.appendChild(c);
        }

        document.body.appendChild(form);
        form.submit();
    }

    // ================= КП: добавление/удаление/очистка =================
    function initCommercialOffer() {
        if (document.body.dataset.offerWired === 'true') return;
        document.body.dataset.offerWired = 'true';

        // --- Хелпер: CSRF ---
        function csrfHeaders(extra) {
            const headers = Object.assign({ 'Accept': 'application/json' }, extra || {});
            const tokenMeta = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            if (tokenMeta && tokenMeta.content && headerMeta && headerMeta.content) {
                headers[headerMeta.content] = tokenMeta.content;
            }
            return headers;
        }

        // --- Хелпер: обновить панель "В КП: N" ---
        function refreshOfferPanel(count) {
            const panel = document.getElementById('offerActions');
            const counter = document.getElementById('offerCount');
            if (!panel) return;
            if (count > 0) {
                panel.style.display = '';
                if (counter) counter.textContent = count;
            } else {
                panel.style.display = 'none';
            }
        }

        // --- Делегированный клик: всё, что связано с КП ---
        document.addEventListener('click', function (e) {

            // === Кнопка "+" на карточке заказа ===
            const addBtn = e.target.closest('.add-to-offer-btn');
            if (addBtn) {
                e.preventDefault();
                e.stopPropagation();
                if (addBtn.disabled) return;

                const orderId = addBtn.dataset.orderId;
                if (!orderId) return;

                addBtn.disabled = true;

                fetch('/commercial-offer/add/' + orderId, {
                    method: 'POST',
                    headers: csrfHeaders(),
                    credentials: 'same-origin'
                })
                    .then(r => r.json())
                    .then(data => {
                        addBtn.disabled = false;

                        if (data.error) {
                            alert(data.error);
                            return;
                        }

                        // Меняем визуал кнопки
                        if (data.added) {
                            addBtn.classList.add('active');
                            addBtn.innerHTML = '<i class="bi bi-check-lg"></i>';
                            addBtn.title = 'Убрать из КП';
                        } else {
                            addBtn.classList.remove('active');
                            addBtn.innerHTML = '<i class="bi bi-plus-lg"></i>';
                            addBtn.title = 'Добавить в КП';
                        }

                        refreshOfferPanel(data.count || 0);
                    })
                    .catch(err => {
                        addBtn.disabled = false;
                        console.error('Ошибка добавления в КП:', err);
                        alert('Ошибка сети при добавлении в КП');
                    });

                return;
            }

            // === Кнопка "Очистить" в панели КП ===
            const clearBtn = e.target.closest('#clearOfferBtn');
            if (clearBtn) {
                e.preventDefault();
                if (clearBtn.disabled) return;
                clearBtn.disabled = true;

                fetch('/commercial-offer/clear', {
                    method: 'POST',
                    headers: csrfHeaders(),
                    credentials: 'same-origin'
                })
                    .then(() => {
                        clearBtn.disabled = false;

                        // Сбрасываем визуал всех кнопок "+"
                        document.querySelectorAll('.add-to-offer-btn.active').forEach(b => {
                            b.classList.remove('active');
                            b.innerHTML = '<i class="bi bi-plus-lg"></i>';
                            b.title = 'Добавить в КП';
                        });

                        refreshOfferPanel(0);
                    })
                    .catch(err => {
                        clearBtn.disabled = false;
                        console.error('Ошибка очистки КП:', err);
                    });

                return;
            }
        });

        // --- Восстанавливаем состояние панели при init ---
        const actives = document.querySelectorAll('.add-to-offer-btn.active').length;
        refreshOfferPanel(actives);
    }

    // ================= ЖУРНАЛ: форма =================
    function initProductionRecordForm() {
        const form = document.getElementById('recordForm');
        if (!form || form.dataset.initialized === 'true') return;
        form.dataset.initialized = 'true';

        const radioCoupling = document.getElementById('typeCoupling');
        const radioAdapter  = document.getElementById('typeAdapter');
        const couplingBlock = document.getElementById('couplingBlock');
        const adapterBlock  = document.getElementById('adapterBlock');
        const hiddenType    = document.getElementById('productTypeHidden');
        if (!radioCoupling || !radioAdapter) return;

        const typeSelect        = document.getElementById('typeSelect');
        const diameterSelect    = document.getElementById('diameterSelect');
        const couplingIdHidden  = document.getElementById('couplingIdHidden');

        const firstSideTypeSelect = document.getElementById('adapterFirstSideTypeSelect');
        const adapterSelect       = document.getElementById('adapterSelect');
        const adapterIdHidden     = document.getElementById('adapterIdHidden');

        const initialCouplingId     = couplingIdHidden?.value || '';
        const initialAdapterId      = adapterIdHidden?.value || '';
        const initialCouplingType   = form.dataset.currentCouplingType || '';
        const initialAdapterFST     = form.dataset.currentAdapterFirstSideType || '';

        function toggleBlocks() {
            const isCoupling = radioCoupling.checked;
            if (couplingBlock) couplingBlock.style.display = isCoupling ? '' : 'none';
            if (adapterBlock)  adapterBlock.style.display  = isCoupling ? 'none' : '';
            if (hiddenType)    hiddenType.value = isCoupling ? 'COUPLING' : 'ADAPTER';
            if (isCoupling && typeSelect && typeSelect.options.length <= 1) loadCouplingTypes();
            if (!isCoupling && firstSideTypeSelect && firstSideTypeSelect.options.length <= 1) loadAdapterTypes();
        }

        function loadCouplingTypes() {
            fetch('/production-records/api/couplings/types')
                .then(r => r.json())
                .then(types => {
                    typeSelect.innerHTML = '<option value="">-- Выберите тип --</option>';
                    types.forEach(t => {
                        const o = new Option(t, t);
                        if (t === initialCouplingType) o.selected = true;
                        typeSelect.add(o);
                    });
                    if (initialCouplingType) typeSelect.dispatchEvent(new Event('change'));
                });
        }

        function loadCouplingDiameters(type) {
            diameterSelect.innerHTML = '<option value="">-- Выберите диаметр --</option>';
            if (couplingIdHidden) couplingIdHidden.value = '';
            if (!type) { diameterSelect.disabled = true; return; }
            diameterSelect.disabled = false;
            fetch(`/production-records/api/couplings/by-type?type=${encodeURIComponent(type)}`)
                .then(r => r.json())
                .then(list => {
                    list.forEach(c => diameterSelect.add(new Option(c.diameter, c.id)));
                    if (initialCouplingId) {
                        [...diameterSelect.options].forEach(o => {
                            if (String(o.value) === String(initialCouplingId)) {
                                o.selected = true;
                                if (couplingIdHidden) couplingIdHidden.value = initialCouplingId;
                            }
                        });
                    }
                });
        }

        if (typeSelect)     typeSelect.addEventListener('change', function () { loadCouplingDiameters(this.value); });
        if (diameterSelect) diameterSelect.addEventListener('change', function () {
            if (couplingIdHidden) couplingIdHidden.value = this.value;
        });

        function loadAdapterTypes() {
            fetch('/production-records/api/adapters/first-side-types')
                .then(r => r.json())
                .then(types => {
                    firstSideTypeSelect.innerHTML = '<option value="">-- Выберите тип --</option>';
                    types.forEach(t => {
                        const o = new Option(t, t);
                        if (t === initialAdapterFST) o.selected = true;
                        firstSideTypeSelect.add(o);
                    });
                    if (initialAdapterFST) firstSideTypeSelect.dispatchEvent(new Event('change'));
                });
        }

        function loadAdapters(fst) {
            adapterSelect.innerHTML = '<option value="">-- Выберите переводник --</option>';
            if (adapterIdHidden) adapterIdHidden.value = '';
            if (!fst) { adapterSelect.disabled = true; return; }
            adapterSelect.disabled = false;
            fetch(`/production-records/api/adapters/by-first-side-type?type=${encodeURIComponent(fst)}`)
                .then(r => r.json())
                .then(list => {
                    list.forEach(a => adapterSelect.add(new Option(a.fullName, a.id)));
                    if (initialAdapterId) {
                        [...adapterSelect.options].forEach(o => {
                            if (String(o.value) === String(initialAdapterId)) {
                                o.selected = true;
                                if (adapterIdHidden) adapterIdHidden.value = initialAdapterId;
                            }
                        });
                    }
                });
        }

        if (firstSideTypeSelect) firstSideTypeSelect.addEventListener('change', function () { loadAdapters(this.value); });
        if (adapterSelect)       adapterSelect.addEventListener('change', function () {
            if (adapterIdHidden) adapterIdHidden.value = this.value;
        });

        radioCoupling.addEventListener('change', toggleBlocks);
        radioAdapter.addEventListener('change', toggleBlocks);

        // Если ни одна не выбрана — по умолчанию муфта
        if (!radioCoupling.checked && !radioAdapter.checked) radioCoupling.checked = true;
        toggleBlocks();
    }

    // ================= ЖУРНАЛ: фильтры =================
    function initProductionRecordFilters() {
        const container = document.getElementById('journalFilters');
        if (!container) return;

        const toggle = document.getElementById('filtersToggle');
        if (toggle && toggle.dataset.wired !== 'true') {
            toggle.dataset.wired = 'true';
            toggle.addEventListener('click', () => container.classList.toggle('open'));
        }

        const productTypeSelect = document.getElementById('filterProductType');
        if (!productTypeSelect || productTypeSelect.dataset.wired === 'true') return;
        productTypeSelect.dataset.wired = 'true';

        const couplingBlock   = document.getElementById('filterCouplingBlock');
        const diameterBlock   = document.getElementById('filterDiameterBlock');
        const adapterBlock    = document.getElementById('filterAdapterBlock');
        const adapterSelect   = document.getElementById('filterAdapterSelect');

        const typeSelect      = document.getElementById('filterTypeSelect');
        const diameterSelect  = document.getElementById('filterDiameterSelect');
        const couplingIdHidden = document.getElementById('filterCouplingIdHidden');

        const initialCouplingId = couplingIdHidden?.value || '';
        const initialAdapterId  = adapterSelect?.value || '';

        function toggleFilterBlocks() {
            const isAdapter = productTypeSelect.value === 'ADAPTER';
            if (couplingBlock) couplingBlock.style.display = isAdapter ? 'none' : '';
            if (diameterBlock) diameterBlock.style.display = isAdapter ? 'none' : '';
            if (adapterBlock)  adapterBlock.style.display  = isAdapter ? '' : 'none';
            if (isAdapter && adapterSelect && adapterSelect.options.length <= 1) loadAllAdapters();
            if (!isAdapter && typeSelect && typeSelect.options.length <= 1) loadCouplingTypes();
        }

        function loadCouplingTypes() {
            fetch('/production-records/api/couplings/types')
                .then(r => r.json())
                .then(types => {
                    typeSelect.innerHTML = '<option value="">-- Все типы --</option>';
                    types.forEach(t => typeSelect.add(new Option(t, t)));
                    if (initialCouplingId) loadCouplingById(initialCouplingId);
                });
        }

        function loadCouplingById(id) {
            fetch(`/production-records/api/couplings/${id}`)
                .then(r => r.json())
                .then(data => {
                    if (typeSelect) {
                        typeSelect.value = data.type;
                        typeSelect.dispatchEvent(new Event('change'));
                    }
                    let attempts = 0;
                    const iv = setInterval(() => {
                        attempts++;
                        if ((diameterSelect.options.length > 1) || attempts > 20) {
                            clearInterval(iv);
                            [...diameterSelect.options].forEach(o => {
                                if (String(o.value) === String(id)) {
                                    o.selected = true;
                                    if (couplingIdHidden) couplingIdHidden.value = id;
                                }
                            });
                        }
                    }, 60);
                });
        }

        if (typeSelect) typeSelect.addEventListener('change', function () {
            const type = this.value;
            diameterSelect.innerHTML = '<option value="">-- Все диаметры --</option>';
            if (couplingIdHidden) couplingIdHidden.value = '';
            if (!type) { diameterSelect.disabled = true; return; }
            diameterSelect.disabled = false;
            fetch(`/production-records/api/couplings/by-type?type=${encodeURIComponent(type)}`)
                .then(r => r.json())
                .then(list => {
                    list.forEach(c => diameterSelect.add(new Option(c.diameter, c.id)));
                    if (initialCouplingId) {
                        [...diameterSelect.options].forEach(o => {
                            if (String(o.value) === String(initialCouplingId)) {
                                o.selected = true;
                                if (couplingIdHidden) couplingIdHidden.value = initialCouplingId;
                            }
                        });
                    }
                });
        });

        if (diameterSelect) diameterSelect.addEventListener('change', function () {
            if (couplingIdHidden) couplingIdHidden.value = this.value;
        });

        function loadAllAdapters() {
            fetch('/production-records/api/adapters')
                .then(r => r.json())
                .then(list => {
                    adapterSelect.innerHTML = '<option value="">-- Все переводники --</option>';
                    list.forEach(a => {
                        const o = new Option(a.fullName, a.id);
                        if (String(a.id) === String(initialAdapterId)) o.selected = true;
                        adapterSelect.add(o);
                    });
                });
        }

        productTypeSelect.addEventListener('change', toggleFilterBlocks);
        toggleFilterBlocks();
    }

    // ================= Общий вход =================
    function init() {
        initProductTypeToggle();
        initCouplingSelects();
        initAdapterSelects();
        initWorksPicker();
        initStartProduction();
        initCommercialOffer();
        initProductionRecordForm();
        initProductionRecordFilters();
    }

    return { init };
})();