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

    // ================= Общий вход =================
    function init() {
        initProductTypeToggle();
        initCouplingSelects();
        initAdapterSelects();
        initWorksPicker();
        initStartProduction();
    }

    return { init };
})();