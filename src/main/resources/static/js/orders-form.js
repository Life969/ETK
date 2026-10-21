/**
 * orders-form.js
 * Инициализация страниц заказов: формы create/edit + waiting/production/completed
 * Работает на turbo:frame-load через layout.html
 */
window.OrdersForm = (function () {

    function qsa(sel, root) {
        return Array.from((root || document).querySelectorAll(sel));
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

    // ================= Селекты стали: группа → марка =================
    function initSteelGroupSelects() {
        const groupSelect = document.getElementById('steelGroupSelect');
        const gradeSelect = document.getElementById('steelGradeSelect');
        const groupIdHidden = document.getElementById('steelGroupIdHidden');
        const gradeIdHidden = document.getElementById('steelGradeIdHidden');
        if (!groupSelect || !gradeSelect) return;
        if (groupSelect.dataset.initialized === 'true') return;
        groupSelect.dataset.initialized = 'true';

        const form = groupSelect.closest('form');
        let initialGroupId = form?.dataset.initialStrengthGroupId || '';
        let initialGradeId = form?.dataset.initialSteelGradeId || '';

        if (!initialGroupId && groupIdHidden?.value) initialGroupId = groupIdHidden.value;
        if (!initialGradeId && gradeIdHidden?.value) initialGradeId = gradeIdHidden.value;

        function loadGrades(groupId, selectedGradeId) {
            gradeSelect.innerHTML = '<option value="">-- Не указана --</option>';
            if (gradeIdHidden) gradeIdHidden.value = '';

            if (!groupId) {
                gradeSelect.disabled = true;
                gradeSelect.innerHTML = '<option value="">-- Сначала выберите группу --</option>';
                return;
            }

            gradeSelect.disabled = false;
            fetch(`/orders/api/steel-groups/${groupId}/grades`)
                .then(r => r.json())
                .then(list => {
                    if (list.length === 0) {
                        gradeSelect.innerHTML = '<option value="">-- У группы нет марок --</option>';
                        return;
                    }
                    gradeSelect.innerHTML = '<option value="">-- Не указана --</option>';
                    list.forEach(g => {
                        const o = new Option(g.name, g.id);
                        gradeSelect.add(o);
                    });
                    if (selectedGradeId) {
                        const opt = Array.from(gradeSelect.options)
                            .find(o => String(o.value) === String(selectedGradeId));
                        if (opt) {
                            opt.selected = true;
                            if (gradeIdHidden) gradeIdHidden.value = selectedGradeId;
                        }
                    }
                })
                .catch(err => {
                    console.error('Ошибка загрузки марок:', err);
                    gradeSelect.innerHTML = '<option value="">-- Ошибка загрузки --</option>';
                });
        }

        if (initialGroupId) {
            groupSelect.value = initialGroupId;
            if (groupIdHidden) groupIdHidden.value = initialGroupId;
            loadGrades(initialGroupId, initialGradeId);
        } else {
            gradeSelect.disabled = true;
        }

        groupSelect.addEventListener('change', function () {
            const gid = this.value;
            if (groupIdHidden) groupIdHidden.value = gid || '';
            if (gradeIdHidden) gradeIdHidden.value = '';
            loadGrades(gid, null);
        });

        gradeSelect.addEventListener('change', function () {
            if (gradeIdHidden) gradeIdHidden.value = this.value || '';
        });
    }

    // ================================================================
    // ПИКЕР КАРТОЧЕК — замена <select> диаметров / переводников / резьб
    // ================================================================

    /** Плотность стали, кг/м³ — синхронно с WorkpieceCalculator. */
    const STEEL_DENSITY_JS = 7850.0;

    /** M = π · ρ · S · (D − S) · L / 1e6 */
    function calcWeightFromLength(D, S, L) {
        if (D <= 0 || S <= 0 || L <= 0) return 0;
        return Math.PI * STEEL_DENSITY_JS * S * (D - S) * L / 1_000_000.0;
    }

    /** L = M · 1e6 / (π · ρ · S · (D − S)) */
    function calcLengthFromWeight(D, S, M) {
        const denom = Math.PI * STEEL_DENSITY_JS * S * (D - S) / 1_000_000.0;
        if (denom <= 0) return 0;
        return M / denom;
    }

    function round2(v) {
        return Math.round(v * 100) / 100;
    }

    /**
     * Создать DOM-карточку для пикера.
     *
     * kind: 'coupling' | 'adapter' | 'thread'
     *   - 'coupling' → WoW-тултип как у муфты (data-type / data-diameter ...)
     *   - 'adapter'  → WoW-тултип как у переводника (data-first-type ...)
     *   - 'thread'   → WoW-тултип справочника резьб (data-pipe-kind="THREAD")
     */
    function createPickerCard(item, kind, onSelect) {
        const card = document.createElement('div');
        card.className = 'order-picker-card product-tooltip';
        card.dataset.pickerId = item.id;
        card.setAttribute('role', 'button');
        card.setAttribute('tabindex', '0');

        // ─── Картинка / иконка ───
        const imgWrap = document.createElement('div');
        imgWrap.className = 'order-picker-card-image';
        if (item.imagePath) {
            const img = document.createElement('img');
            let src = item.imagePath;
            if (!src.startsWith('/') && !src.startsWith('http')) src = '/uploads/' + src;
            img.src = src;
            img.alt = '';
            img.onerror = () => { img.style.display = 'none'; };
            imgWrap.appendChild(img);
        } else {
            const i = document.createElement('i');
            i.className = kind === 'coupling' ? 'bi bi-gear-wide-connected'
                : kind === 'adapter'  ? 'bi bi-arrow-left-right'
                    :                       'bi bi-record-circle';
            imgWrap.appendChild(i);
        }
        card.appendChild(imgWrap);

        // ─── Основная подпись ───
        const label = document.createElement('div');
        label.className = 'order-picker-card-label';
        if (kind === 'adapter') {
            label.textContent = (item.firstSideType || '') + ' ' + (item.firstSideDiameter || '');
        } else {
            label.textContent = item.conditionalDiameter || item.name || '—';
        }
        card.appendChild(label);

        // ─── Подпись (× НКТ 89 / ГОСТ) ───
        if (kind === 'adapter' && item.secondSideType) {
            const sub = document.createElement('div');
            sub.className = 'order-picker-card-sublabel';
            sub.textContent = '× ' + item.secondSideType + ' ' + (item.secondSideDiameter || '');
            card.appendChild(sub);
        } else if (kind === 'thread' && item.standart) {
            const sub = document.createElement('div');
            sub.className = 'order-picker-card-sublabel';
            sub.textContent = item.standart;
            card.appendChild(sub);
        }

        // ─── data-* для WowTooltip ───
        if (kind === 'coupling') {
            card.dataset.id            = item.id;
            card.dataset.name          = item.name || '';
            card.dataset.type          = item.type || '';
            card.dataset.diameter      = item.conditionalDiameter || '';
            card.dataset.length        = item.lengthMm ?? '';
            card.dataset.weight        = item.weightKg ?? '';
            card.dataset.outerDiameter = item.outerDiameterMm ?? '';
            card.dataset.innerDiameter = item.innerDiameterMm ?? '';
            card.dataset.cost          = item.manufacturingCost ?? '';
            card.dataset.price         = item.priceForEmployee ?? '';
            card.dataset.description   = item.description || '';
            card.dataset.image         = item.imagePath || '';
        } else if (kind === 'adapter') {
            card.dataset.id             = item.id;
            card.dataset.name           = item.fullName || '';
            card.dataset.firstType      = item.firstSideType || '';
            card.dataset.firstDiameter  = item.firstSideDiameter || '';
            card.dataset.secondType     = item.secondSideType || '';
            card.dataset.secondDiameter = item.secondSideDiameter || '';
            card.dataset.length         = item.lengthMm ?? '';
            card.dataset.weight         = item.weightKg ?? '';
            card.dataset.outerDiameter  = item.outerDiameterMm ?? '';
            card.dataset.innerDiameter  = item.innerDiameterMm ?? '';
            card.dataset.cost           = item.manufacturingCost ?? '';
            card.dataset.price          = item.priceForEmployee ?? '';
            card.dataset.description    = item.description || '';
            card.dataset.image          = item.imagePath || '';
        } else if (kind === 'thread') {
            card.dataset.pipeKind            = 'THREAD';
            card.dataset.id                  = item.id;
            card.dataset.name                = item.name || '';
            card.dataset.conditionalDiameter = item.conditionalDiameter || '';
            card.dataset.standart            = item.standart || '';
            card.dataset.cost                = item.manufacturingCost ?? '';
            card.dataset.price               = item.priceForEmployee ?? '';
            card.dataset.description         = item.description || '';
            card.dataset.image               = item.imagePath || '';
        }

        const activate = () => onSelect(item);
        card.addEventListener('click', activate);
        card.addEventListener('keydown', (ev) => {
            if (ev.key === 'Enter' || ev.key === ' ') { ev.preventDefault(); activate(); }
        });

        return card;
    }

    /**
     * Универсальный пикер «тип → карточки в скроллбаре».
     *
     * cfg:
     *   typeSelectId  — id <select> с типом
     *   containerId   — id div-контейнера карточек
     *   hiddenId      — id hidden-input, куда пишем выбранный id
     *   kind          — 'coupling' | 'adapter' | 'thread'
     *   emptyHint     — текст «-- Сначала выберите тип --»
     *   noItemsHint   — текст «Нет данных»
     *   buildUrl(type) → url
     *   getInitial()  → { type, id } | null   (для edit-режима)
     */
    function initCardPicker(cfg) {
        const typeSelect = document.getElementById(cfg.typeSelectId);
        const picker     = document.getElementById(cfg.containerId);
        const hidden     = document.getElementById(cfg.hiddenId);
        if (!typeSelect || !picker) return;
        if (typeSelect.dataset.pickerWired === 'true') return;
        typeSelect.dataset.pickerWired = 'true';

        const initial = typeof cfg.getInitial === 'function' ? cfg.getInitial() : null;

        function renderEmpty(text) {
            picker.innerHTML = '';
            const e = document.createElement('div');
            e.className = 'order-picker-empty';
            e.textContent = text;
            picker.appendChild(e);
            picker.dataset.selectedDiameter = '';
        }

        function select(item) {
            if (hidden) hidden.value = item.id;
            picker.dataset.selectedDiameter = item.conditionalDiameter || '';

            picker.dataset.selectedCost    = item.manufacturingCost ?? '';
            picker.dataset.selectedOuterD  = item.outerDiameterMm  ?? '';
            picker.dataset.selectedInnerD  = item.innerDiameterMm  ?? '';
            picker.dataset.selectedLength  = item.lengthMm         ?? '';
            picker.dataset.selectedWeight  = item.weightKg         ?? '';

            picker.querySelectorAll('.order-picker-card').forEach(c => {
                c.classList.toggle('selected', String(c.dataset.pickerId) === String(item.id));
            });
            document.dispatchEvent(new CustomEvent('pipe:recalc'));
        }

        function loadItems(type, selectedId) {
            if (!type) {
                renderEmpty(cfg.emptyHint || '-- Сначала выберите тип --');
                if (hidden) hidden.value = '';
                return;
            }
            renderEmpty('Загрузка...');

            fetch(cfg.buildUrl(type))
                .then(r => r.json())
                .then(items => {
                    if (!Array.isArray(items) || items.length === 0) {
                        renderEmpty(cfg.noItemsHint || 'Нет данных для этого типа');
                        if (hidden) hidden.value = '';
                        return;
                    }
                    picker.innerHTML = '';
                    items.forEach(item => {
                        const card = createPickerCard(item, cfg.kind, (it) => select(it));
                        picker.appendChild(card);
                    });

                    // автоселект (edit-режим)
                    if (selectedId) {
                        const toSelect = items.find(it => String(it.id) === String(selectedId));
                        if (toSelect) {
                            select(toSelect);
                            return;
                        }
                    }
                    // если не выбрали — сбрасываем данные пикера
                    picker.dataset.selectedDiameter = '';
                    document.dispatchEvent(new CustomEvent('pipe:recalc'));
                })
                .catch(err => {
                    console.error('Picker load error:', err);
                    renderEmpty('Ошибка загрузки');
                });
        }

        // Стартовое состояние (edit-mode)
        if (initial && initial.type) {
            typeSelect.value = initial.type;
            loadItems(initial.type, initial.id);
        } else {
            renderEmpty(cfg.emptyHint || '-- Сначала выберите тип --');
        }

        // Смена типа пользователем → перезагружаем карточки
        typeSelect.addEventListener('change', function () {
            if (hidden) hidden.value = '';
            picker.dataset.selectedDiameter = '';
            loadItems(this.value, null);
        });
    }

    // --- три конкретные инициализации ---

    function initCouplingPicker() {
        initCardPicker({
            typeSelectId: 'couplingTypeSelect',
            containerId:  'couplingDiameterPicker',
            hiddenId:     'couplingIdHidden',
            kind:         'coupling',
            emptyHint:    '-- Сначала выберите тип --',
            noItemsHint:  'Нет муфт с таким типом',
            buildUrl: (t) => `/orders/api/couplings/by-type?type=${encodeURIComponent(t)}`,
            getInitial: () => {
                const form = document.getElementById('orderForm');
                if (!form) return null;
                const checked = form.querySelector('input[name="productType"]:checked');
                if (checked?.value !== 'COUPLING') return null;
                const t = form.dataset.initialCouplingType || '';
                const id = form.dataset.initialCouplingId || '';
                return t ? { type: t, id } : null;
            }
        });
    }

    function initAdapterPicker() {
        initCardPicker({
            typeSelectId: 'adapterTypeSelect',
            containerId:  'adapterPicker',
            hiddenId:     'adapterIdHidden',
            kind:         'adapter',
            emptyHint:    '-- Сначала выберите тип --',
            noItemsHint:  'Нет переводников с таким типом',
            buildUrl: (t) => `/orders/api/adapters/by-first-side-type?type=${encodeURIComponent(t)}`,
            getInitial: () => {
                const form = document.getElementById('orderForm');
                if (!form) return null;
                const checked = form.querySelector('input[name="productType"]:checked');
                if (checked?.value !== 'ADAPTER') return null;
                const t = form.dataset.initialAdapterFirstSideType || '';
                const id = form.dataset.initialAdapterId || '';
                return t ? { type: t, id } : null;
            }
        });
    }

    function initThreadPicker(which /* 'pipe' | 'nipple' */) {
        const cap = which === 'pipe' ? 'pipe' : 'nipple';
        initCardPicker({
            typeSelectId: `${cap}ThreadTypeSelect`,
            containerId:  `${cap}ThreadDiameterPicker`,
            hiddenId:     `${cap}ExternalThreadId`,
            kind:         'thread',
            emptyHint:    '-- Сначала выберите тип --',
            noItemsHint:  'Нет резьб с таким типом',
            buildUrl: (t) => `/orders/api/external-threads/by-type?type=${encodeURIComponent(t)}`,
            getInitial: () => {
                const form = document.getElementById('orderForm');
                const kindH = document.getElementById('pipeProductKindHidden');
                const kind = kindH?.value || '';
                if (!form) return null;
                if (which === 'pipe'   && kind !== 'PIPE')   return null;
                if (which === 'nipple' && kind !== 'NIPPLE') return null;
                const t = form.dataset.initialExternalThreadType || '';
                const id = form.dataset.initialExternalThreadId || '';
                return t ? { type: t, id } : null;
            }
        });
    }

    // ================================================================
    // ТРУБА / ПАТРУБОК — живой пересчёт
    // ================================================================

    /** Живой пересчёт для трубы: метраж ↔ тоннаж + оценка количества. */
    function initPipeLiveCalculation() {
        const block = document.getElementById('pipeBlock');
        if (!block || block.dataset.liveWired === 'true') return;
        block.dataset.liveWired = 'true';

        const modeLengthRadio = document.getElementById('pipeModeLength');
        const modeWeightRadio = document.getElementById('pipeModeWeight');
        const picker          = document.getElementById('pipeThreadDiameterPicker');
        const wallInput       = document.getElementById('pipeWallThickness');
        const totalLen        = document.getElementById('pipeTotalLength');
        const totalW          = document.getElementById('pipeTotalWeight');
        const avgLen          = document.getElementById('pipeAvgLength');
        const computedQty     = document.getElementById('pipeComputedQty');

        if (!totalLen || !totalW) return;

        function getDiameter() {
            const d = picker?.dataset?.selectedDiameter;
            if (!d) return null;
            const v = parseFloat(d);
            return isFinite(v) && v > 0 ? v : null;
        }

        function recalcFrom(source) {
            const D = getDiameter();
            const S = parseFloat(wallInput?.value);
            if (!D || !(S > 0)) {
                if (computedQty) computedQty.textContent = '—';
                return;
            }

            if (source === 'length') {
                const L = parseFloat(totalLen.value);
                if (L > 0) {
                    const M = calcWeightFromLength(D, S, L);
                    totalW.value = round2(M);
                }
            } else if (source === 'weight') {
                const M = parseFloat(totalW.value);
                if (M > 0) {
                    const L = calcLengthFromWeight(D, S, M);
                    totalLen.value = round2(L);
                }
            }

            if (computedQty) {
                const L = parseFloat(totalLen.value);
                const avg = parseFloat(avgLen?.value) || 10;
                computedQty.textContent = (L > 0 && avg > 0) ? Math.ceil(L / avg) : '—';
            }
        }

        function applyMode() {
            if (modeWeightRadio?.checked) {
                totalW.readOnly = false;
                totalLen.readOnly = true;
                recalcFrom('weight');
            } else {
                totalLen.readOnly = false;
                totalW.readOnly = true;
                recalcFrom('length');
            }
        }

        if (modeLengthRadio) modeLengthRadio.addEventListener('change', applyMode);
        if (modeWeightRadio) modeWeightRadio.addEventListener('change', applyMode);

        totalLen.addEventListener('input', () => recalcFrom('length'));
        totalW.addEventListener('input', () => recalcFrom('weight'));
        if (wallInput) wallInput.addEventListener('input', applyMode);
        if (avgLen) avgLen.addEventListener('input', applyMode);

        document.addEventListener('pipe:recalc', applyMode);

        // стартовое состояние
        applyMode();
    }

    /** Живой пересчёт для патрубка: количество × длина → метраж → вес. */
    function initNippleLiveCalculation() {
        const block = document.getElementById('nippleBlock');
        if (!block || block.dataset.liveWired === 'true') return;
        block.dataset.liveWired = 'true';

        const picker    = document.getElementById('nippleThreadDiameterPicker');
        const wallInput = document.getElementById('nippleWallThickness');
        const pieceLen  = document.getElementById('nipplePieceLength');
        const qtyInput  = document.getElementById('nippleQuantity');
        const outLength = document.getElementById('nippleComputedLength');
        const outWeight = document.getElementById('nippleComputedWeight');
        if (!pieceLen || !qtyInput) return;

        function getDiameter() {
            const d = picker?.dataset?.selectedDiameter;
            if (!d) return null;
            const v = parseFloat(d);
            return isFinite(v) && v > 0 ? v : null;
        }

        function recalc() {
            const D  = getDiameter();
            const S  = parseFloat(wallInput?.value);
            const Lp = parseFloat(pieceLen.value);
            const Q  = parseInt(qtyInput.value);

            if (!D || !(S > 0) || !(Lp > 0) || !(Q > 0)) {
                if (outLength) outLength.textContent = '—';
                if (outWeight) outWeight.textContent = '—';
                return;
            }

            const totalLenM = (Lp * Q) / 1000.0;
            const weightKg = calcWeightFromLength(D, S, totalLenM);

            if (outLength) outLength.textContent = round2(totalLenM);
            if (outWeight) outWeight.textContent = round2(weightKg);
        }

        pieceLen.addEventListener('input', recalc);
        qtyInput.addEventListener('input', recalc);
        if (wallInput) wallInput.addEventListener('input', recalc);
        document.addEventListener('pipe:recalc', recalc);

        recalc();
    }

    // ================================================================
// LIVE РАСЧЁТ СТОИМОСТИ ЗАКАЗА
// ================================================================

    function initLiveCostCalculation() {
        const panel = document.getElementById('liveCostPanel');
        if (!panel || panel.dataset.wired === 'true') return;
        panel.dataset.wired = 'true';

        // ---- helpers ----
        function fmt(v) {
            return Number(v || 0).toLocaleString('ru-RU', {
                minimumFractionDigits: 2,
                maximumFractionDigits: 2
            });
        }
        function setText(id, val) {
            const el = document.getElementById(id);
            if (el) el.textContent = val;
        }
        function escapeHtml(s) {
            return String(s || '').replace(/[&<>]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;'}[c]));
        }
        function getProductType() {
            const c = document.querySelector('input[name="productType"]:checked');
            return c ? c.value : null;
        }
        function getQuantity() {
            const t = getProductType();
            if (t === 'COUPLING' || t === 'ADAPTER') {
                return parseInt(document.getElementById('globalQuantity')?.value) || 0;
            }
            if (t === 'NIPPLE') {
                return parseInt(document.getElementById('nippleQuantity')?.value) || 0;
            }
            if (t === 'PIPE') {
                const raw = document.getElementById('pipeComputedQty')?.textContent;
                const n = parseInt(raw);
                return isFinite(n) ? n : 0;
            }
            return 0;
        }
        function getMarkup() {
            const sel = document.getElementById('steelGroupSelect');
            if (!sel || !sel.value) return { percent: 0, code: '' };
            const opt = sel.options[sel.selectedIndex];
            const txt = opt.textContent || '';
            const m = txt.match(/\(\+([\d.]+)%\)/);
            return {
                percent: m ? parseFloat(m[1]) : 0,
                code: (txt.split('—')[0] || '').trim()
            };
        }
        function getSelectedWorks() {
            const arr = [];
            document.querySelectorAll('#worksPicker .work-chip.selected').forEach(ch => {
                arr.push({
                    name: ch.dataset.workName || '',
                    price: parseFloat(ch.dataset.workPrice) || 0
                });
            });
            return arr;
        }
        function getPickerSelectedCost(id) {
            const el = document.getElementById(id);
            return parseFloat(el?.dataset.selectedCost) || 0;
        }
        function getPickerSelectedOuter(id) {
            return parseFloat(document.getElementById(id)?.dataset.selectedOuterD) || 0;
        }
        function getPickerSelectedInner(id) {
            return parseFloat(document.getElementById(id)?.dataset.selectedInnerD) || 0;
        }
        function getPickerSelectedLength(id) {
            return parseFloat(document.getElementById(id)?.dataset.selectedLength) || 0;
        }
        function getPickerSelectedWeight(id) {
            return parseFloat(document.getElementById(id)?.dataset.selectedWeight) || 0;
        }

        // ---- toggle раскрытия ----
        const toggle = document.getElementById('liveBreakdownToggle');
        const breakdown = document.getElementById('liveCostBreakdown');
        if (toggle && breakdown) {
            toggle.classList.add('open');
            breakdown.classList.add('open');
            toggle.addEventListener('click', () => {
                const open = breakdown.classList.toggle('open');
                toggle.classList.toggle('open', open);
                const txt = toggle.querySelector('.btn-breakdown-text');
                if (txt) txt.textContent = open ? 'Скрыть' : 'Расшифровка';
            });
        }

        // ---- главный пересчёт ----
        function recalc() {
            const type = getProductType();
            if (!type) {
                panel.style.display = 'none';
                return;
            }
            panel.style.display = 'block';

            const qty = getQuantity();
            const works = getSelectedWorks();
            const markup = getMarkup();

            let baseCost     = 0;
            let weightKg     = 0;
            let unitCost     = 0;
            let threadCost   = 0;
            let threadsCost  = 0;
            let pricePerTon  = 0;
            let metalCost    = 0;

            const couplingSteps = document.getElementById('lcCouplingSteps');
            const pipeSteps     = document.getElementById('lcPipeSteps');

            if (type === 'COUPLING' || type === 'ADAPTER') {
                couplingSteps.style.display = 'block';
                pipeSteps.style.display = 'none';

                const pickerId = type === 'COUPLING' ? 'couplingDiameterPicker' : 'adapterPicker';
                unitCost = getPickerSelectedCost(pickerId);
                baseCost = unitCost * qty;

                // общий вес всех штук
                weightKg = getPickerSelectedWeight(pickerId);
                if (!weightKg && qty > 0) {
                    const D = getPickerSelectedOuter(pickerId);
                    const d = getPickerSelectedInner(pickerId);
                    const L = getPickerSelectedLength(pickerId); // мм на 1 шт
                    if (D > 0 && d > 0 && L > 0) {
                        const S = (D - d) / 2;
                        const totalLenM = (L * qty) / 1000;
                        weightKg = Math.PI * 7850 * S * (D - S) * totalLenM / 1_000_000;
                    }
                }

                setText('lcUnitCost', fmt(unitCost));
                setText('lcQty', qty);
                setText('lcBaseCostCoupling', fmt(baseCost));
            } else {
                couplingSteps.style.display = 'none';
                pipeSteps.style.display = 'block';

                const pickerId = type === 'PIPE'
                    ? 'pipeThreadDiameterPicker'
                    : 'nippleThreadDiameterPicker';
                threadCost = getPickerSelectedCost(pickerId);
                threadsCost = threadCost * 2 * qty;

                if (type === 'PIPE') {
                    pricePerTon = parseFloat(document.getElementById('pipePricePerTon')?.value) || 0;
                    weightKg = parseFloat(document.getElementById('pipeTotalWeight')?.value) || 0;
                } else {
                    pricePerTon = parseFloat(document.getElementById('nipplePricePerTon')?.value) || 0;
                    const wTxt = document.getElementById('nippleComputedWeight')?.textContent || '';
                    weightKg = parseFloat(wTxt.replace(',', '.')) || 0;
                }
                metalCost = pricePerTon * weightKg / 1000;
                baseCost = threadsCost + metalCost;

                setText('lcThreadCost', fmt(threadCost));
                setText('lcThreadQty', qty);
                setText('lcThreadsCost', fmt(threadsCost));
                setText('lcPricePerTon', fmt(pricePerTon));
                setText('lcMetalWeight', fmt(weightKg));
                setText('lcMetalCost', fmt(metalCost));
            }

            // ---- надбавка ----
            const markupCost = baseCost * markup.percent / 100;
            const markupStepCoupling = document.getElementById('lcMarkupStepCoupling');
            const markupStepPipe     = document.getElementById('lcMarkupStepPipe');

            if (markup.percent > 0) {
                if (type === 'COUPLING' || type === 'ADAPTER') {
                    markupStepCoupling.style.display = 'flex';
                    markupStepPipe.style.display = 'none';
                    setText('lcSGCodeCoupling', markup.code || '—');
                    setText('lcMarkupPercentCoupling', markup.percent);
                    setText('lcMarkupPercentCoupling2', markup.percent);
                    setText('lcBaseCostForMarkupCoupling', fmt(baseCost));
                    setText('lcMarkupCostCoupling', fmt(markupCost));
                } else {
                    markupStepCoupling.style.display = 'none';
                    markupStepPipe.style.display = 'flex';
                    setText('lcSGCodePipe', markup.code || '—');
                    setText('lcMarkupPercentPipe', markup.percent);
                    setText('lcMarkupPercentPipe2', markup.percent);
                    setText('lcBaseCostForMarkupPipe', fmt(baseCost));
                    setText('lcMarkupCostPipe', fmt(markupCost));
                }
            } else {
                markupStepCoupling.style.display = 'none';
                markupStepPipe.style.display = 'none';
            }

            // ---- доп. работы ----
            const billingWeight = Math.max(weightKg, 100);
            const worksStep = document.getElementById('lcWorksStep');
            const worksList = document.getElementById('lcWorksList');
            let worksTotal = 0;

            if (works.length > 0) {
                worksStep.style.display = 'flex';
                setText('lcBillingWeight', fmt(billingWeight));
                worksList.innerHTML = '';
                works.forEach(w => {
                    const cost = w.price * billingWeight / 1000;
                    worksTotal += cost;
                    const row = document.createElement('div');
                    row.className = 'cost-work-row';
                    row.innerHTML = `
                    <span class="cw-name">${escapeHtml(w.name)}</span>
                    <span class="cw-formula">
                        <span class="cf-num">${fmt(w.price)} ₽/т</span>
                        <span class="cf-op">×</span>
                        <span class="cf-num">${fmt(billingWeight)} кг</span>
                        <span class="cf-op">=</span>
                        <span class="cf-result">${fmt(cost)} ₽</span>
                    </span>
                `;
                    worksList.appendChild(row);
                });
            } else {
                worksStep.style.display = 'none';
                worksList.innerHTML = '';
            }

            // ---- итого ----
            const totalCost = baseCost + markupCost + worksTotal;
            setText('liveTotalCost', fmt(totalCost));
            setText('lcBreakdownTotal', fmt(totalCost));
        }

        // ---- слушатели ----
        document.addEventListener('pipe:recalc', () => setTimeout(recalc, 30));

        document.addEventListener('input', (e) => {
            const id = e.target?.id;
            if (!id) return;
            if ([
                'globalQuantity',
                'nippleQuantity',
                'nipplePieceLength',
                'nippleWallThickness',
                'nipplePricePerTon',
                'pipeWallThickness',
                'pipeTotalLength',
                'pipeTotalWeight',
                'pipePricePerTon'
            ].includes(id)) {
                setTimeout(recalc, 50);
            }
        });

        document.addEventListener('change', (e) => {
            const t = e.target;
            if (!t) return;
            if (t.name === 'productType' || t.id === 'steelGroupSelect') {
                setTimeout(recalc, 30);
            }
        });

        document.addEventListener('click', (e) => {
            if (e.target.closest('#worksPicker .work-chip')
                || e.target.closest('#worksPicker .selected-work-chip .remove-btn')) {
                setTimeout(recalc, 30);
            }
        });

        // первый расчёт
        recalc();
    }

    /**
     * Переключение 4 типов продукции в форме заказа.
     * Управляет видимостью и disabled-состоянием полей блоков.
     */
    function initPipeNippleBlocks() {
        const radioCoupling = document.getElementById('typeCoupling');
        const radioAdapter  = document.getElementById('typeAdapter');
        const radioPipe     = document.getElementById('typePipe');
        const radioNipple   = document.getElementById('typeNipple');
        if (!radioPipe || !radioNipple) return;

        const form = document.getElementById('orderForm') || radioCoupling?.closest('form');
        if (!form || form.dataset.pipeBlocksWired === 'true') return;
        form.dataset.pipeBlocksWired = 'true';

        const couplingBlock  = document.getElementById('couplingBlock');
        const adapterBlock   = document.getElementById('adapterBlock');
        const pipeBlock      = document.getElementById('pipeBlock');
        const nippleBlock    = document.getElementById('nippleBlock');
        const hiddenKind     = document.getElementById('pipeProductKindHidden');
        const globalQtyField = document.getElementById('globalQuantityField');
        const globalQtyInput = document.getElementById('globalQuantity');

        function setBlockEnabled(block, enabled) {
            if (!block) return;
            block.style.display = enabled ? 'block' : 'none';
            block.querySelectorAll('input, select, textarea').forEach(el => {
                el.disabled = !enabled;
            });
        }

        function toggleBlocks() {
            const isCoupling = radioCoupling?.checked;
            const isAdapter  = radioAdapter?.checked;
            const isPipe     = radioPipe.checked;
            const isNipple   = radioNipple.checked;

            setBlockEnabled(couplingBlock, isCoupling);
            setBlockEnabled(adapterBlock,  isAdapter);
            setBlockEnabled(pipeBlock,     isPipe);
            setBlockEnabled(nippleBlock,   isNipple);

            const needGlobalQty = isCoupling || isAdapter;
            if (globalQtyField) globalQtyField.style.display = needGlobalQty ? '' : 'none';
            if (globalQtyInput) globalQtyInput.disabled = !needGlobalQty;

            if (hiddenKind) {
                if (isPipe) hiddenKind.value = 'PIPE';
                else if (isNipple) hiddenKind.value = 'NIPPLE';
                else hiddenKind.value = '';
            }
        }

        [radioCoupling, radioAdapter, radioPipe, radioNipple].forEach(r => {
            if (r) r.addEventListener('change', toggleBlocks);
        });

        initPipeLiveCalculation();
        initNippleLiveCalculation();

        toggleBlocks();
    }

    // ================================================================
    // УДАЛЕНИЕ ЗАКАЗА — ПУЗЫРЬ + ВЕТЕР
    // ================================================================

    function spawnBubbleBurst(cx, cy) {
        const layer = document.createElement('div');
        layer.className = 'bubble-burst-layer';

        const wave = document.createElement('span');
        wave.className = 'bubble-shockwave';
        wave.style.left = cx + 'px';
        wave.style.top  = cy + 'px';
        layer.appendChild(wave);

        const flash = document.createElement('span');
        flash.className = 'bubble-particle';
        flash.style.left = cx + 'px';
        flash.style.top  = cy + 'px';
        flash.style.width  = '36px';
        flash.style.height = '36px';
        flash.style.setProperty('--dx', '0px');
        flash.style.setProperty('--dy', '0px');
        flash.style.animationDuration = '0.4s';
        layer.appendChild(flash);

        const count = 16;
        for (let i = 0; i < count; i++) {
            const angle = (Math.PI * 2 * i) / count + (Math.random() - 0.5) * 0.7;
            const dist  = 70 + Math.random() * 130;
            const dx    = Math.cos(angle) * dist;
            const dy    = Math.sin(angle) * dist;
            const size  = 4 + Math.random() * 10;
            const delay = Math.random() * 40;

            const p = document.createElement('span');
            p.className = 'bubble-particle';
            p.style.left = cx + 'px';
            p.style.top  = cy + 'px';
            p.style.width  = size + 'px';
            p.style.height = size + 'px';
            p.style.setProperty('--dx', dx + 'px');
            p.style.setProperty('--dy', dy + 'px');
            p.style.animationDelay = delay + 'ms';
            layer.appendChild(p);
        }

        document.body.appendChild(layer);
        setTimeout(() => layer.remove(), 900);
    }

    function spawnWindLeaf(cx, cy) {
        const layer = document.createElement('div');
        layer.className = 'leaf-flight-layer';

        const flyer = document.createElement('div');
        flyer.className = 'leaf-flyer leaf-flyer--wind';
        flyer.style.left = cx + 'px';
        flyer.style.top  = cy + 'px';

        const img = document.createElement('img');
        img.src = '/images/leaf-logo.png';
        img.alt = '';
        img.setAttribute('aria-hidden', 'true');
        flyer.appendChild(img);
        layer.appendChild(flyer);

        const ex = window.innerWidth  + 220;
        const ey = -140;
        const dx = ex - cx;
        const dy = ey - cy;

        const sparkCount = 8;
        for (let i = 0; i < sparkCount; i++) {
            const spark = document.createElement('span');
            spark.className = 'wind-spark';

            const jx = (Math.random() - 0.5) * 90;
            const jy = (Math.random() - 0.5) * 90;
            spark.style.left = (cx + jx) + 'px';
            spark.style.top  = (cy + jy) + 'px';

            const endJx = (Math.random() - 0.5) * 220;
            const endJy = (Math.random() - 0.5) * 160;
            spark.style.setProperty('--dx', (dx + endJx) + 'px');
            spark.style.setProperty('--dy', (dy + endJy) + 'px');

            spark.style.animationDelay = (70 + i * 85) + 'ms';
            layer.appendChild(spark);
        }

        document.body.appendChild(layer);

        const anim = flyer.animate([
            { transform: `translate(-50%, -50%) translate(0px, 0px) scale(0.35) rotate(-30deg)`, opacity: 0 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.06}px, ${dy * 0.18}px) scale(1.28) rotate(80deg)`, opacity: 1, offset: 0.12 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.20}px, ${dy * 0.02}px) scale(1.18) rotate(190deg)`, opacity: 1, offset: 0.28 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.42}px, ${dy * 0.38}px) scale(1.10) rotate(320deg)`, opacity: 1, offset: 0.48 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.66}px, ${dy * 0.60}px) scale(1.02) rotate(450deg)`, opacity: 1, offset: 0.68 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.85}px, ${dy * 0.84}px) scale(0.88) rotate(570deg)`, opacity: 1, offset: 0.88 },
            { transform: `translate(-50%, -50%) translate(${dx}px, ${dy}px) scale(0.5) rotate(700deg)`, opacity: 0 }
        ], {
            duration: 1650,
            easing: 'cubic-bezier(0.3, 0.55, 0.4, 1)',
            fill: 'forwards'
        });

        let finished = false;
        const finish = () => {
            if (finished) return;
            finished = true;
            layer.remove();
        };
        anim.onfinish = finish;
        setTimeout(finish, 2200);
    }

    function playDeleteAnimation(entity, onBurst) {
        const rect = entity.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top  + rect.height / 2;

        entity.classList.add(
            entity.classList.contains('order-detail-card')
                ? 'order-detail-card--popping'
                : 'order-card--popping'
        );

        setTimeout(() => {
            spawnBubbleBurst(cx, cy);
            if (typeof onBurst === 'function') onBurst();
        }, 280);

        setTimeout(() => spawnWindLeaf(cx, cy), 380);
    }

    function initOrderDeletion() {
        if (document.body.dataset.orderDeletionWired === 'true') return;
        document.body.dataset.orderDeletionWired = 'true';

        document.addEventListener('click', function (e) {
            const btn = e.target.closest('[data-delete-btn]');
            if (!btn) return;
            if (btn.disabled) return;

            const form = btn.closest('form');
            if (!form) return;

            if (!window.confirm('Удалить заказ?')) {
                e.preventDefault();
                e.stopPropagation();
                return;
            }

            e.preventDefault();
            e.stopPropagation();

            const entity = btn.closest('.order-card, .order-detail-card');

            if (!entity || prefersReducedMotion()) {
                form.setAttribute('data-turbo-frame', 'app');
                form.removeAttribute('data-turbo');
                form.requestSubmit ? form.requestSubmit() : form.submit();
                return;
            }

            btn.disabled = true;

            let submitted = false;
            const doSubmit = () => {
                if (submitted) return;
                submitted = true;
                form.setAttribute('data-turbo-frame', 'app');
                form.removeAttribute('data-turbo');
                form.requestSubmit ? form.requestSubmit() : form.submit();
            };

            playDeleteAnimation(entity, doSubmit);
            setTimeout(doSubmit, 500);
        }, true);
    }

    // ================================================================
    // ПЕРЕВОД ЗАКАЗА: АНИМАЦИЯ ЛИСТКА
    // ================================================================
    function prefersReducedMotion() {
        return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    }

    function isInViewport(el) {
        if (!el) return false;
        const r = el.getBoundingClientRect();
        const cx = r.left + r.width / 2;
        const cy = r.top + r.height / 2;
        return cx >= 0 && cx <= window.innerWidth
            && cy >= 0 && cy <= window.innerHeight;
    }

    function findTargetElement(href) {
        const headerBtn = href
            ? document.querySelector(`.orders-nav-btns a[href="${href}"]`)
            : null;

        if (headerBtn && isInViewport(headerBtn)) return headerBtn;

        const brand = document.querySelector('.app-glass .navbar .navbar-brand');
        if (brand && isInViewport(brand)) return brand;

        return document.querySelector('.app-glass .navbar');
    }

    function createLeafFlyer(cardRect) {
        const layer = document.createElement('div');
        layer.className = 'leaf-flight-layer';

        const flyer = document.createElement('div');
        flyer.className = 'leaf-flyer';
        flyer.style.left = (cardRect.left + cardRect.width / 2) + 'px';
        flyer.style.top  = (cardRect.top + cardRect.height / 2) + 'px';

        const img = document.createElement('img');
        img.src = '/images/leaf-logo.png';
        img.alt = '';
        img.setAttribute('aria-hidden', 'true');
        flyer.appendChild(img);

        layer.appendChild(flyer);
        document.body.appendChild(layer);
        return { layer, flyer };
    }

    function animateLeafFlight(flyer, cardRect, targetRect) {
        const sx = cardRect.left + cardRect.width / 2;
        const sy = cardRect.top + cardRect.height / 2;
        const ex = targetRect.left + targetRect.width / 2;
        const ey = targetRect.top + targetRect.height / 2;

        const dx = ex - sx;
        const dy = ey - sy;
        const arc = Math.min(160, Math.max(70, Math.abs(dx) * 0.28));

        return flyer.animate([
            { transform: `translate(-50%, -50%) translate(0px, 0px) scale(0.3) rotate(-40deg)`, opacity: 0, offset: 0 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.08}px, ${dy * 0.08 - arc * 0.6}px) scale(1.35) rotate(60deg)`, opacity: 1, offset: 0.18 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.45}px, ${dy * 0.45 - arc}px) scale(1.1) rotate(220deg)`, opacity: 1, offset: 0.55 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.85}px, ${dy * 0.85 - arc * 0.35}px) scale(0.85) rotate(400deg)`, opacity: 1, offset: 0.85 },
            { transform: `translate(-50%, -50%) translate(${dx}px, ${dy}px) scale(0.05) rotate(560deg)`, opacity: 0, offset: 1 }
        ], {
            duration: 1150,
            easing: 'cubic-bezier(0.25, 0.7, 0.35, 1)',
            fill: 'forwards'
        });
    }

    function startLeafTransition(card, target) {
        if (!card || !target || prefersReducedMotion()) return;

        const cardRect = card.getBoundingClientRect();
        const targetRect = target.getBoundingClientRect();

        card.classList.add('order-card--departing');

        const { layer, flyer } = createLeafFlyer(cardRect);

        requestAnimationFrame(() => {
            const anim = animateLeafFlight(flyer, cardRect, targetRect);

            let finished = false;
            const finish = () => {
                if (finished) return;
                finished = true;
                layer.remove();

                if (target) {
                    target.classList.add('leaf-target-pulse');
                    setTimeout(() => target.classList.remove('leaf-target-pulse'), 720);
                }
            };

            anim.onfinish = finish;
            setTimeout(finish, 1600);
        });
    }

    function navigateFrame(url) {
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('data-turbo-frame', 'app');
        link.style.cssText = 'position:absolute;left:-9999px;top:-9999px;';
        document.body.appendChild(link);
        link.click();
        setTimeout(() => link.remove(), 500);
    }

    function initOrderTransitions() {
        if (document.body.dataset.orderTransitionsWired === 'true') return;
        document.body.dataset.orderTransitionsWired = 'true';

        document.addEventListener('click', function (e) {
            const btn = e.target.closest('[data-transition-target]');
            if (!btn) return;

            if (btn.classList.contains('start-production-btn')
                && btn.dataset.hasDeadline === 'false') {
                return;
            }

            const card = btn.closest('.order-card');
            if (card && card.classList.contains('order-card--departing')) {
                e.preventDefault();
                e.stopPropagation();
                return;
            }

            if (!card || prefersReducedMotion()) return;

            const target = findTargetElement(btn.dataset.transitionTarget);
            if (!target) return;

            e.preventDefault();
            e.stopPropagation();

            startLeafTransition(card, target);

            const form = btn.closest('form');
            if (form) {
                if (!form.hasAttribute('data-turbo-frame')) {
                    form.setAttribute('data-turbo-frame', 'app');
                }
                form.removeAttribute('data-turbo');
                form.requestSubmit ? form.requestSubmit() : form.submit();
            } else if (btn.classList.contains('start-production-btn')) {
                const orderId = btn.dataset.orderId;
                const f = document.createElement('form');
                f.method = 'POST';
                f.action = `/orders/${orderId}/status`;
                f.setAttribute('data-turbo-frame', 'app');
                f.style.cssText = 'position:absolute;left:-9999px;top:-9999px;';

                const csrf = document.querySelector('meta[name="_csrf"]');
                if (csrf) {
                    const c = document.createElement('input');
                    c.type = 'hidden';
                    c.name = '_csrf';
                    c.value = csrf.content;
                    f.appendChild(c);
                }

                const s1 = document.createElement('input');
                s1.type = 'hidden';
                s1.name = 'newStatus';
                s1.value = 'IN_PRODUCTION';
                f.appendChild(s1);

                const s2 = document.createElement('input');
                s2.type = 'hidden';
                s2.name = 'returnTo';
                s2.value = '/orders/waiting';
                f.appendChild(s2);

                document.body.appendChild(f);
                f.requestSubmit ? f.requestSubmit() : f.submit();
                setTimeout(() => f.remove(), 1500);
            }
        }, true);
    }

    // ================= Waiting: старт производства =================
    function initStartProduction() {
        if (document.body.dataset.startProdWired === 'true') return;
        document.body.dataset.startProdWired = 'true';

        document.addEventListener('click', function (e) {
            const btn = e.target.closest('.start-production-btn[data-has-deadline="false"]');
            if (btn) {
                e.preventDefault();
                e.stopPropagation();
                openDeadlineModal(btn.dataset.orderId);
                return;
            }

            if (e.target.closest('#confirmDeadlineBtn')) {
                confirmDeadline();
                return;
            }

            if (e.target.classList && e.target.classList.contains('hb-modal-overlay')) {
                closeDeadlineModal(e.target);
                return;
            }

            const closeBtn = e.target.closest('[data-modal-close]');
            if (closeBtn) {
                const ov = closeBtn.closest('.hb-modal-overlay');
                if (ov) closeDeadlineModal(ov);
            }
        });

        document.addEventListener('keydown', function (e) {
            if (e.key !== 'Escape') return;
            const overlay = document.querySelector('.hb-modal-overlay.show');
            if (overlay) closeDeadlineModal(overlay);
        });
    }

    function openDeadlineModal(orderId) {
        const overlay = document.getElementById('deadlineModal');
        if (!overlay) return;

        const hiddenId = document.getElementById('orderIdForDeadline');
        const dateInput = document.getElementById('deadlineInput');
        if (!hiddenId || !dateInput) return;

        hiddenId.value = orderId;

        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const iso = tomorrow.toISOString().split('T')[0];
        dateInput.min = iso;
        dateInput.value = iso;

        overlay.classList.add('show');
        overlay.setAttribute('aria-hidden', 'false');
        document.body.style.overflow = 'hidden';

        setTimeout(() => dateInput.focus(), 120);
    }

    function closeDeadlineModal(overlay) {
        if (!overlay) return;
        overlay.classList.remove('show');
        overlay.setAttribute('aria-hidden', 'true');
        document.body.style.overflow = '';
    }

    function confirmDeadline() {
        const hiddenId  = document.getElementById('orderIdForDeadline');
        const dateInput = document.getElementById('deadlineInput');
        const overlay   = document.getElementById('deadlineModal');

        const orderId  = hiddenId?.value;
        const deadline = dateInput?.value;

        if (!orderId || !deadline) {
            alert('Укажите дедлайн');
            return;
        }

        const headers = {
            'Content-Type': 'application/x-www-form-urlencoded',
            'Accept': 'application/json'
        };
        const tokenMeta  = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        if (tokenMeta && tokenMeta.content && headerMeta && headerMeta.content) {
            headers[headerMeta.content] = tokenMeta.content;
        }

        fetch(`/orders/${orderId}/set-deadline-and-start`, {
            method: 'POST',
            headers: headers,
            body: `deadline=${encodeURIComponent(deadline)}`
        })
            .then(r => r.json())
            .then(res => {
                if (!res.success) {
                    alert('Ошибка: ' + (res.error || 'неизвестная'));
                    return;
                }
                closeDeadlineModal(overlay);

                const startBtn = document.querySelector(
                    `.start-production-btn[data-order-id="${orderId}"]`
                );
                const card   = startBtn?.closest('.order-card');
                const target = findTargetElement('/orders/production');

                if (card && target) {
                    startLeafTransition(card, target);
                }

                navigateFrame('/orders/waiting');
            })
            .catch(err => {
                console.error(err);
                alert('Ошибка сети');
            });
    }

    // ================= КП: добавление/удаление/очистка =================
    function refreshOfferPanelFromDom() {
        const count = document.querySelectorAll('.add-to-offer-btn.active').length;
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

    // ================= КП: раскрытие расшифровки =================
    function initOfferBreakdown() {
        // ─── Верхняя кнопка "Расшифровка стоимости" ───
        const topToggle = document.getElementById('offerBreakdownToggle');
        const topPanel  = document.getElementById('offerCostBreakdown');

        if (topToggle && topPanel && topToggle.dataset.wired !== 'true') {
            topToggle.dataset.wired = 'true';
            topToggle.addEventListener('click', () => {
                const open = topPanel.classList.toggle('open');
                topToggle.classList.toggle('open', open);
                const txt = topToggle.querySelector('.btn-breakdown-text');
                if (txt) txt.textContent = open ? 'Скрыть расшифровку' : 'Расшифровка стоимости';
            });
        }

        // ─── Toggle-ы по позициям ───
        // ВАЖНО: прямые обработчики на каждый элемент (без document.addEventListener),
        // чтобы при Turbo-переходах обработчики не накапливались.
        const toggles = document.querySelectorAll('.offer-detail-toggle');
        console.log('[offer-breakdown] toggles:', toggles.length);

        toggles.forEach((t) => {
            if (t.dataset.wired === 'true') return;
            t.dataset.wired = 'true';

            t.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();

                const body = document.getElementById(t.dataset.target);
                if (!body) {
                    console.warn('[offer-breakdown] body not found:', t.dataset.target);
                    return;
                }

                const willOpen = !t.classList.contains('open');
                t.classList.toggle('open', willOpen);

                if (willOpen) {
                    body.classList.add('open');
                    const h = body.scrollHeight;
                    if (h > 0) {
                        body.style.maxHeight = h + 'px';
                        const onEnd = (ev) => {
                            if (ev.propertyName !== 'max-height') return;
                            body.style.maxHeight = 'none';
                            body.removeEventListener('transitionend', onEnd);
                        };
                        body.addEventListener('transitionend', onEnd);
                        // fallback, если transitionend не сработал
                        setTimeout(() => {
                            if (body.classList.contains('open')) body.style.maxHeight = 'none';
                        }, 500);
                    } else {
                        body.style.maxHeight = 'none';
                    }
                } else {
                    body.style.maxHeight = body.scrollHeight + 'px';
                    body.classList.remove('open');
                    void body.offsetHeight;
                    body.style.maxHeight = '0';
                }
            });
        });
    }

    function initCommercialOffer() {
        refreshOfferPanelFromDom();

        if (document.body.dataset.offerWired === 'true') return;
        document.body.dataset.offerWired = 'true';

        function csrfHeaders(extra) {
            const headers = Object.assign({'Accept': 'application/json'}, extra || {});
            const tokenMeta  = document.querySelector('meta[name="_csrf"]');
            const headerMeta = document.querySelector('meta[name="_csrf_header"]');
            if (tokenMeta && tokenMeta.content && headerMeta && headerMeta.content) {
                headers[headerMeta.content] = tokenMeta.content;
            }
            return headers;
        }

        document.addEventListener('click', function (e) {
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
                        if (data.error) { alert(data.error); return; }

                        if (data.added) {
                            addBtn.classList.add('active');
                            addBtn.innerHTML = '<i class="bi bi-check-lg"></i>';
                            addBtn.title = 'Убрать из КП';
                        } else {
                            addBtn.classList.remove('active');
                            addBtn.innerHTML = '<i class="bi bi-plus-lg"></i>';
                            addBtn.title = 'Добавить в КП';
                        }
                        refreshOfferPanelFromDom();
                    })
                    .catch(err => {
                        addBtn.disabled = false;
                        console.error('Ошибка добавления в КП:', err);
                        alert('Ошибка сети при добавлении в КП');
                    });
                return;
            }

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
                        document.querySelectorAll('.add-to-offer-btn.active').forEach(b => {
                            b.classList.remove('active');
                            b.innerHTML = '<i class="bi bi-plus-lg"></i>';
                            b.title = 'Добавить в КП';
                        });
                        refreshOfferPanelFromDom();
                    })
                    .catch(err => {
                        clearBtn.disabled = false;
                        console.error('Ошибка очистки КП:', err);
                    });
                return;
            }
        });
    }

    // ================= Общий вход =================
    function init() {
        initCouplingPicker();
        initAdapterPicker();
        initThreadPicker('pipe');
        initThreadPicker('nipple');

        initSteelGroupSelects();
        initWorksPicker();
        initStartProduction();
        initCommercialOffer();
        initOfferBreakdown();   // ← ДОБАВИТЬ
        initOrderTransitions();
        initOrderDeletion();
        initPipeNippleBlocks();
        initLiveCostCalculation();
    }

    return {init};
})();