/**
 * journal-form.js
 * Инициализация страниц журнала производства:
 *   - форма создания/редактирования записи (4 типа продукции)
 *   - фильтры списка записей
 *
 * Идемпотентно. Вызывается из app.js на DOMContentLoaded / turbo:load /
 * turbo:frame-load (#app).
 */
window.JournalForm = (function () {

    // ================================================================
    // ФОРМА СОЗДАНИЯ / РЕДАКТИРОВАНИЯ
    // ================================================================
    function initProductionRecordForm() {
        const form = document.getElementById('recordForm');
        if (!form || form.dataset.journalFormWired === 'true') return;
        form.dataset.journalFormWired = 'true';

        const radioCoupling = document.getElementById('typeCoupling');
        const radioAdapter  = document.getElementById('typeAdapter');
        const radioPipe     = document.getElementById('typePipe');
        const radioNipple   = document.getElementById('typeNipple');
        if (!radioCoupling) return;

        const couplingBlock = document.getElementById('couplingBlock');
        const adapterBlock  = document.getElementById('adapterBlock');
        const pipeBlock     = document.getElementById('pipeBlock');
        const hiddenType    = document.getElementById('productTypeHidden');

        // --- Муфта (новые id) ---
        const couplingTypeSelect     = document.getElementById('recCouplingTypeSelect');
        const couplingDiameterSelect = document.getElementById('recCouplingDiameterSelect');
        const couplingIdHidden       = document.getElementById('recCouplingId');

        // --- Переводник ---
        const adapterFirstSideTypeSelect = document.getElementById('recAdapterFstSelect');
        const adapterSelect              = document.getElementById('recAdapterSelect');
        const adapterIdHidden            = document.getElementById('recAdapterId');

        // --- Труба / патрубок ---
        const pipeThreadTypeSelect     = document.getElementById('recThreadTypeSelect');
        const pipeThreadDiameterSelect = document.getElementById('recThreadDiameterSelect');
        const pipeExternalThreadId     = document.getElementById('recThreadId');

        // --- Длина патрубка ---
        const nippleLengthField = document.getElementById('nippleLengthField');
        const nippleLengthInput = document.getElementById('recPieceLengthMm');

        const initialCouplingType       = form.dataset.currentCouplingType || '';
        const initialAdapterFirstSide   = form.dataset.currentAdapterFirstSideType || '';
        const initialExternalThreadType = form.dataset.currentExternalThreadType || '';

        function setBlockEnabled(block, enabled) {
            if (!block) return;
            block.style.display = enabled ? 'block' : 'none';
            block.querySelectorAll('input, select, textarea').forEach(el => {
                // Не трогаем nippleLengthField — он управляется отдельно
                if (el.id === 'recPieceLengthMm') return;
                el.disabled = !enabled;
            });
        }

        // ... loadCouplingDiameters / loadAdapters / loadThreadDiameters
        //     без изменений, только id обновлены

        function loadCouplingDiameters(type, selectedId) {
            if (!couplingDiameterSelect) return;
            couplingDiameterSelect.innerHTML = '<option value="">-- Выберите диаметр --</option>';
            if (!type) {
                couplingDiameterSelect.disabled = true;
                if (couplingIdHidden) couplingIdHidden.value = '';
                return;
            }
            couplingDiameterSelect.disabled = false;
            fetch('/production-records/api/couplings/by-type?type=' + encodeURIComponent(type))
                .then(r => r.json())
                .then(list => {
                    list.forEach(c => couplingDiameterSelect.add(new Option(c.diameter, c.id)));
                    if (selectedId) {
                        [...couplingDiameterSelect.options].forEach(o => {
                            if (String(o.value) === String(selectedId)) {
                                o.selected = true;
                                if (couplingIdHidden) couplingIdHidden.value = selectedId;
                            }
                        });
                    }
                })
                .catch(err => console.error('Ошибка загрузки диаметров муфт:', err));
        }

        function loadAdapters(type, selectedId) {
            if (!adapterSelect) return;
            adapterSelect.innerHTML = '<option value="">-- Выберите переводник --</option>';
            if (!type) {
                adapterSelect.disabled = true;
                if (adapterIdHidden) adapterIdHidden.value = '';
                return;
            }
            adapterSelect.disabled = false;
            fetch('/production-records/api/adapters/by-first-side-type?type=' + encodeURIComponent(type))
                .then(r => r.json())
                .then(list => {
                    list.forEach(a => adapterSelect.add(new Option(a.fullName, a.id)));
                    if (selectedId) {
                        [...adapterSelect.options].forEach(o => {
                            if (String(o.value) === String(selectedId)) {
                                o.selected = true;
                                if (adapterIdHidden) adapterIdHidden.value = selectedId;
                            }
                        });
                    }
                })
                .catch(err => console.error('Ошибка загрузки переводников:', err));
        }

        function loadThreadDiameters(type, selectedId) {
            if (!pipeThreadDiameterSelect) return;
            pipeThreadDiameterSelect.innerHTML = '<option value="">-- Выберите диаметр --</option>';
            if (!type) {
                pipeThreadDiameterSelect.disabled = true;
                if (pipeExternalThreadId) pipeExternalThreadId.value = '';
                return;
            }
            pipeThreadDiameterSelect.disabled = false;
            fetch('/production-records/api/external-threads/by-type?type=' + encodeURIComponent(type))
                .then(r => r.json())
                .then(list => {
                    list.forEach(t => pipeThreadDiameterSelect.add(new Option(t.name, t.id)));
                    if (selectedId) {
                        [...pipeThreadDiameterSelect.options].forEach(o => {
                            if (String(o.value) === String(selectedId)) {
                                o.selected = true;
                                if (pipeExternalThreadId) pipeExternalThreadId.value = selectedId;
                            }
                        });
                    }
                })
                .catch(err => console.error('Ошибка загрузки резьб:', err));
        }

        function toggleBlocks() {
            const isCoupling   = radioCoupling.checked;
            const isAdapter    = radioAdapter && radioAdapter.checked;
            const isPipe       = radioPipe && radioPipe.checked;
            const isNipple     = radioNipple && radioNipple.checked;
            const isPipeFamily = isPipe || isNipple;

            setBlockEnabled(couplingBlock, isCoupling);
            setBlockEnabled(adapterBlock,  isAdapter);
            setBlockEnabled(pipeBlock,     isPipeFamily);

            // Длина патрубка — только для NIPPLE
            if (nippleLengthField) {
                nippleLengthField.style.display = isNipple ? '' : 'none';
            }
            if (nippleLengthInput) {
                nippleLengthInput.disabled = !isNipple;
            }

            if (hiddenType) {
                if (isCoupling) hiddenType.value = 'COUPLING';
                else if (isAdapter) hiddenType.value = 'ADAPTER';
                else if (isPipe) hiddenType.value = 'PIPE';
                else if (isNipple) hiddenType.value = 'NIPPLE';
                else hiddenType.value = '';
            }
        }

        // --- Обработчики каскадов (только с новыми id) ---
        if (couplingTypeSelect) {
            couplingTypeSelect.addEventListener('change', function () {
                if (couplingIdHidden) couplingIdHidden.value = '';
                loadCouplingDiameters(this.value, null);
            });
        }
        if (couplingDiameterSelect) {
            couplingDiameterSelect.addEventListener('change', function () {
                if (couplingIdHidden) couplingIdHidden.value = this.value || '';
            });
        }

        if (adapterFirstSideTypeSelect) {
            adapterFirstSideTypeSelect.addEventListener('change', function () {
                if (adapterIdHidden) adapterIdHidden.value = '';
                loadAdapters(this.value, null);
            });
        }
        if (adapterSelect) {
            adapterSelect.addEventListener('change', function () {
                if (adapterIdHidden) adapterIdHidden.value = this.value || '';
            });
        }

        if (pipeThreadTypeSelect) {
            pipeThreadTypeSelect.addEventListener('change', function () {
                if (pipeExternalThreadId) pipeExternalThreadId.value = '';
                loadThreadDiameters(this.value, null);
            });
        }
        if (pipeThreadDiameterSelect) {
            pipeThreadDiameterSelect.addEventListener('change', function () {
                if (pipeExternalThreadId) pipeExternalThreadId.value = this.value || '';
            });
        }

        [radioCoupling, radioAdapter, radioPipe, radioNipple].forEach(r => {
            if (r) r.addEventListener('change', toggleBlocks);
        });

        const anyChecked = [radioCoupling, radioAdapter, radioPipe, radioNipple]
            .some(r => r && r.checked);
        if (!anyChecked) radioCoupling.checked = true;

        if (initialCouplingType && couplingTypeSelect) {
            couplingTypeSelect.value = initialCouplingType;
            loadCouplingDiameters(initialCouplingType, couplingIdHidden?.value || '');
        }
        if (initialAdapterFirstSide && adapterFirstSideTypeSelect) {
            adapterFirstSideTypeSelect.value = initialAdapterFirstSide;
            loadAdapters(initialAdapterFirstSide, adapterIdHidden?.value || '');
        }
        if (initialExternalThreadType && pipeThreadTypeSelect) {
            pipeThreadTypeSelect.value = initialExternalThreadType;
            loadThreadDiameters(initialExternalThreadType, pipeExternalThreadId?.value || '');
        }

        toggleBlocks();

        // --- Валидация перед отправкой ---
        form.addEventListener('submit', function (e) {
            const pt = hiddenType ? hiddenType.value : '';
            if (!pt) { e.preventDefault(); alert('Выберите тип продукции'); return; }

            if (pt === 'COUPLING' && !(couplingIdHidden && couplingIdHidden.value)) {
                e.preventDefault(); alert('Выберите муфту'); return;
            }
            if (pt === 'ADAPTER' && !(adapterIdHidden && adapterIdHidden.value)) {
                e.preventDefault(); alert('Выберите переводник'); return;
            }
            if ((pt === 'PIPE' || pt === 'NIPPLE')
                && !(pipeExternalThreadId && pipeExternalThreadId.value)) {
                e.preventDefault(); alert('Выберите резьбу'); return;
            }
            if (pt === 'NIPPLE') {
                const len = nippleLengthInput ? parseFloat(nippleLengthInput.value) : NaN;
                if (!(len > 0)) {
                    e.preventDefault(); alert('Укажите длину патрубка, мм');
                }
            }
        });
    }

    // ================================================================
    // ФИЛЬТРЫ СПИСКА
    // ================================================================
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

        const couplingBlock       = document.getElementById('filterCouplingBlock');
        const diameterBlock       = document.getElementById('filterDiameterBlock');
        const adapterBlock        = document.getElementById('filterAdapterBlock');
        const adapterSelect       = document.getElementById('filterAdapterSelect');

        const typeSelect          = document.getElementById('filterTypeSelect');
        const diameterSelect      = document.getElementById('filterDiameterSelect');
        const couplingIdHidden    = document.getElementById('filterCouplingIdHidden');

        const threadTypeBlock      = document.getElementById('filterExternalThreadTypeBlock');
        const threadDiameterBlock  = document.getElementById('filterExternalThreadDiameterBlock');
        const threadTypeSelect     = document.getElementById('filterExternalThreadTypeSelect');
        const threadDiameterSelect = document.getElementById('filterExternalThreadDiameterSelect');
        const threadIdHidden       = document.getElementById('filterExternalThreadIdHidden');

        const initialCouplingId       = couplingIdHidden?.value || '';
        const initialAdapterId        = adapterSelect?.value || '';
        const initialExternalThreadId = threadIdHidden?.value || '';

        function toggleFilterBlocks() {
            const pt = productTypeSelect.value;
            const isCoupling   = pt === 'COUPLING';
            const isAdapter    = pt === 'ADAPTER';
            const isPipeFamily = pt === 'PIPE' || pt === 'NIPPLE';

            if (couplingBlock)  couplingBlock.style.display  = isCoupling ? '' : 'none';
            if (diameterBlock)  diameterBlock.style.display  = isCoupling ? '' : 'none';
            if (adapterBlock)   adapterBlock.style.display   = isAdapter ? '' : 'none';
            if (threadTypeBlock)     threadTypeBlock.style.display     = isPipeFamily ? '' : 'none';
            if (threadDiameterBlock) threadDiameterBlock.style.display = isPipeFamily ? '' : 'none';

            if (isCoupling && typeSelect && typeSelect.options.length <= 1) loadCouplingTypes();
            if (isAdapter && adapterSelect && adapterSelect.options.length <= 1) loadAllAdapters();
        }

        function loadCouplingTypes() {
            fetch('/production-records/api/couplings/types')
                .then(r => r.json())
                .then(types => {
                    typeSelect.innerHTML = '<option value="">-- Все типы --</option>';
                    types.forEach(t => typeSelect.add(new Option(t, t)));
                    if (initialCouplingId) {
                        fetch('/production-records/api/couplings/' + initialCouplingId)
                            .then(r => r.json())
                            .then(data => {
                                if (data.type) {
                                    typeSelect.value = data.type;
                                    typeSelect.dispatchEvent(new Event('change'));
                                }
                            })
                            .catch(() => {});
                    }
                })
                .catch(err => console.error('Ошибка загрузки типов муфт:', err));
        }

        if (typeSelect) {
            typeSelect.addEventListener('change', function () {
                const type = this.value;
                diameterSelect.innerHTML = '<option value="">-- Все диаметры --</option>';
                if (couplingIdHidden) couplingIdHidden.value = '';
                if (!type) { diameterSelect.disabled = true; return; }
                diameterSelect.disabled = false;
                fetch('/production-records/api/couplings/by-type?type=' + encodeURIComponent(type))
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
                    })
                    .catch(() => {});
            });
        }

        if (diameterSelect) {
            diameterSelect.addEventListener('change', function () {
                if (couplingIdHidden) couplingIdHidden.value = this.value;
            });
        }

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
                })
                .catch(() => {});
        }

        if (threadTypeSelect) {
            threadTypeSelect.addEventListener('change', function () {
                const type = this.value;
                threadDiameterSelect.innerHTML = '<option value="">-- Все диаметры --</option>';
                if (threadIdHidden) threadIdHidden.value = '';
                if (!type) { threadDiameterSelect.disabled = true; return; }
                threadDiameterSelect.disabled = false;
                fetch('/production-records/api/external-threads/by-type?type=' + encodeURIComponent(type))
                    .then(r => r.json())
                    .then(list => {
                        list.forEach(t => threadDiameterSelect.add(new Option(t.name, t.id)));
                        if (initialExternalThreadId) {
                            [...threadDiameterSelect.options].forEach(o => {
                                if (String(o.value) === String(initialExternalThreadId)) {
                                    o.selected = true;
                                    if (threadIdHidden) threadIdHidden.value = initialExternalThreadId;
                                }
                            });
                        }
                    })
                    .catch(() => {});
            });
        }

        if (threadDiameterSelect) {
            threadDiameterSelect.addEventListener('change', function () {
                if (threadIdHidden) threadIdHidden.value = this.value;
            });
        }

        toggleFilterBlocks();

        // Если открыли страницу с уже выбранным фильтром по резьбе —
        // надо раскрыть каскад тип → диаметр
        if (initialExternalThreadId && threadTypeSelect) {
            fetch('/production-records/api/external-threads/' + initialExternalThreadId)
                .then(r => r.json())
                .then(data => {
                    if (data.type) {
                        threadTypeSelect.value = data.type;
                        threadTypeSelect.dispatchEvent(new Event('change'));
                    }
                })
                .catch(() => {});
        }
    }

    // ================================================================
    // ОБЩИЙ ВХОД
    // ================================================================
    function init() {
        initProductionRecordForm();
        initProductionRecordFilters();
    }

    return {init};
})();