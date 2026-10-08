/**
 * orders-form.js
 * Инициализация страниц заказов: формы create/edit + waiting/production/completed
 * Работает на turbo:frame-load через layout.html
 */
window.OrdersForm = (function () {

    function qsa(sel, root) {
        return Array.from((root || document).querySelectorAll(sel));
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
            if (!type) {
                diamSelect.disabled = true;
                return;
            }
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
            if (!type) {
                adapterSelect.disabled = true;
                return;
            }
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
    // ТРУБА / ПАТРУБОК — блоки, каскады, живой пересчёт
    // ================================================================

    /** Плотность стали, кг/м³ — синхронно с WorkpieceCalculator. */
    const STEEL_DENSITY_JS = 7850.0;

    /** Из "НКТ 73" → 73.0; из "ОТТМ 89" → 89.0; null если не нашли. */
    function parseDiameter(text) {
        if (!text) return null;
        const m = String(text).match(/(\d+(\.\d+)?)/);
        if (!m) return null;
        const v = parseFloat(m[1]);
        return isFinite(v) && v > 0 ? v : null;
    }

    /** M = π · ρ · S · (D − S) · L / 1e6  (та же формула, что в WorkpieceCalculator). */
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
     * Универсальный каскад «тип резьбы → условный диаметр».
     * Заполняет hidden-input externalThreadId.
     * После смены диаметра триггерит событие pipe:recalc для live-пересчёта.
     */
    function initExternalThreadCascade(typeId, diamId, hiddenId) {
        const typeSelect = document.getElementById(typeId);
        const diamSelect = document.getElementById(diamId);
        const hidden     = document.getElementById(hiddenId);
        if (!typeSelect || !diamSelect) return;
        if (typeSelect.dataset.wired === 'true') return;
        typeSelect.dataset.wired = 'true';

        const form = typeSelect.closest('form');
        const initialType = form?.dataset.initialExternalThreadType || '';
        const initialId   = form?.dataset.initialExternalThreadId || '';

        // Загружаем типы — они уже в опциях (th:each), но initialType надо выставить
        if (initialType) {
            typeSelect.value = initialType;
            // сразу загрузить диаметры для этого типа
            loadDiameters(initialType, initialId);
        }

        function loadDiameters(type, selectedId) {
            diamSelect.innerHTML = '<option value="">-- Выберите диаметр --</option>';
            if (hidden) hidden.value = '';
            if (!type) {
                diamSelect.disabled = true;
                return;
            }
            diamSelect.disabled = false;
            fetch(`/orders/api/external-threads/by-type?type=${encodeURIComponent(type)}`)
                .then(r => r.json())
                .then(list => {
                    list.forEach(t => {
                        const o = new Option(t.name, t.id);
                        if (selectedId && String(t.id) === String(selectedId)) o.selected = true;
                        diamSelect.add(o);
                    });
                    if (selectedId && hidden) hidden.value = selectedId;
                    document.dispatchEvent(new CustomEvent('pipe:recalc'));
                })
                .catch(err => {
                    console.error('Ошибка загрузки диаметров резьбы:', err);
                    diamSelect.innerHTML = '<option value="">-- Ошибка --</option>';
                });
        }

        typeSelect.addEventListener('change', function () {
            loadDiameters(this.value, null);
        });

        diamSelect.addEventListener('change', function () {
            if (hidden) hidden.value = this.value || '';
            document.dispatchEvent(new CustomEvent('pipe:recalc'));
        });
    }

    /** Живой пересчёт для трубы: метраж ↔ тоннаж + оценка количества. */
    function initPipeLiveCalculation() {
        const block = document.getElementById('pipeBlock');
        if (!block || block.dataset.liveWired === 'true') return;
        block.dataset.liveWired = 'true';

        const modeLengthRadio = document.getElementById('pipeModeLength');
        const modeWeightRadio = document.getElementById('pipeModeWeight');
        const diamSelect = document.getElementById('pipeThreadDiameterSelect');
        const wallInput  = document.getElementById('pipeWallThickness');
        const totalLen   = document.getElementById('pipeTotalLength');
        const totalW     = document.getElementById('pipeTotalWeight');
        const avgLen     = document.getElementById('pipeAvgLength');
        const computedQty = document.getElementById('pipeComputedQty');

        if (!totalLen || !totalW) return;

        function getDiameter() {
            if (!diamSelect || !diamSelect.value) return null;
            const opt = diamSelect.options[diamSelect.selectedIndex];
            return parseDiameter(opt ? opt.text : '');
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

        const diamSelect = document.getElementById('nippleThreadDiameterSelect');
        const wallInput  = document.getElementById('nippleWallThickness');
        const pieceLen   = document.getElementById('nipplePieceLength');
        const qtyInput   = document.getElementById('nippleQuantity');
        const outLength  = document.getElementById('nippleComputedLength');
        const outWeight  = document.getElementById('nippleComputedWeight');
        if (!pieceLen || !qtyInput) return;

        function getDiameter() {
            if (!diamSelect || !diamSelect.value) return null;
            const opt = diamSelect.options[diamSelect.selectedIndex];
            return parseDiameter(opt ? opt.text : '');
        }

        function recalc() {
            const D = getDiameter();
            const S = parseFloat(wallInput?.value);
            const Lp = parseFloat(pieceLen.value);
            const Q = parseInt(qtyInput.value);

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

    /**
     * Переключение 4 типов продукции в форме заказа.
     * Полностью управляет видимостью и disabled-состоянием полей блоков.
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

        const couplingBlock = document.getElementById('couplingBlock');
        const adapterBlock  = document.getElementById('adapterBlock');
        const pipeBlock     = document.getElementById('pipeBlock');
        const nippleBlock   = document.getElementById('nippleBlock');
        const hiddenKind    = document.getElementById('pipeProductKindHidden');
        const globalQtyField = document.getElementById('globalQuantityField');
        const globalQtyInput = document.getElementById('globalQuantity');

        /** Показать/скрыть блок + включить/отключить его поля. */
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

            // global quantity — только для COUPLING / ADAPTER
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

        // Каскады и live-пересчёты — один раз
        initExternalThreadCascade('pipeThreadTypeSelect', 'pipeThreadDiameterSelect', 'pipeExternalThreadId');
        initExternalThreadCascade('nippleThreadTypeSelect', 'nippleThreadDiameterSelect', 'nippleExternalThreadId');
        initPipeLiveCalculation();
        initNippleLiveCalculation();

        // Первичная раскладка
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
        initCouplingSelects();
        initAdapterSelects();
        initSteelGroupSelects();
        initWorksPicker();
        initStartProduction();
        initCommercialOffer();
        initOrderTransitions();
        initOrderDeletion();
        initPipeNippleBlocks();
    }

    return {init};
})();