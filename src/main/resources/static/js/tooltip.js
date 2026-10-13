class WowTooltip {
    static container = null;
    static _selector = null;
    static _delegated = false;

    static escapeHtml(str) {
        if (!str) return '';
        return str.replace(/[&<>]/g, function (m) {
            if (m === '&') return '&amp;';
            if (m === '<') return '&lt;';
            if (m === '>') return '&gt;';
            return m;
        });
    }

    static getValue(value, defaultValue = '—') {
        if (value === undefined || value === null || value === '') return defaultValue;
        return value;
    }

    static updateContent(element) {
        const isAdapter = element.dataset.firstType !== undefined;
        const isPipe = element.dataset.pipeKind !== undefined;

        const name = element.dataset.name || 'Без названия';
        const cost = WowTooltip.getValue(element.dataset.cost);
        const price = WowTooltip.getValue(element.dataset.price);
        let description = element.dataset.description || '';
        const imagePath = element.dataset.image;

        if (description.length > 200) {
            description = description.substring(0, 200) + '...';
        }

        let imageHtml = '';
        if (imagePath && imagePath !== '') {
            let imgUrl = imagePath;
            if (!imgUrl.startsWith('/') && !imgUrl.startsWith('http')) {
                imgUrl = '/uploads/' + imgUrl;
            }
            imageHtml = `<div class="tooltip-image"><img src="${WowTooltip.escapeHtml(imgUrl)}" alt="icon" onerror="this.style.display='none'"></div>`;
        } else {
            imageHtml = `<div class="tooltip-image"><i class="bi bi-cup-straw" style="font-size:24px; color:#c69b6d;"></i></div>`;
        }

        let typeLine, qualityClass, statsHtml;

        if (isPipe) {
            const kind = element.dataset.pipeKind; // 'PIPE' | 'NIPPLE' | 'THREAD'

            // --- Справочник наружных резьб ---
            if (kind === 'THREAD') {
                typeLine = 'Наружная резьба';
                qualityClass = 'quality2';   // зелёный, uncommon

                const condD = element.dataset.conditionalDiameter;
                const standart = element.dataset.standart;

                const rows = [];
                if (condD) {
                    rows.push(`<div class="stat"><span>🎯 Ø условный:</span><span class="stat-value">${condD} мм</span></div>`);
                }
                if (standart) {
                    rows.push(`<div class="stat"><span>📘 ГОСТ / API:</span><span class="stat-value">${standart}</span></div>`);
                }
                statsHtml = rows.join('');
            }
            // --- Труба ---
            else if (kind === 'PIPE') {
                typeLine = 'Труба';
                qualityClass = 'quality2';

                const condD  = element.dataset.conditionalDiameter;
                const wall   = element.dataset.wallThickness;
                const totalL = element.dataset.totalLength;
                const totalW = element.dataset.totalWeight;

                const rows = [];
                if (condD)  rows.push(`<div class="stat"><span>🎯 Ø условный:</span><span class="stat-value">${condD} мм</span></div>`);
                if (wall)   rows.push(`<div class="stat"><span>📐 Толщина стенки:</span><span class="stat-value">${wall} мм</span></div>`);
                if (totalL) rows.push(`<div class="stat"><span>📏 Общий метраж:</span><span class="stat-value">${totalL} м</span></div>`);
                if (totalW) rows.push(`<div class="stat"><span>⚖️ Общий вес:</span><span class="stat-value">${totalW} кг</span></div>`);
                statsHtml = rows.join('');
            }
            // --- Патрубок ---
            else {
                typeLine = 'Патрубок';
                qualityClass = 'quality3';   // синий, rare

                const condD  = element.dataset.conditionalDiameter;
                const pieceL = element.dataset.pieceLength;

                const rows = [];
                if (condD)  rows.push(`<div class="stat"><span>🎯 Ø условный:</span><span class="stat-value">${condD} мм</span></div>`);
                if (pieceL) rows.push(`<div class="stat"><span>📏 Длина:</span><span class="stat-value">${parseFloat(pieceL).toFixed(0)} мм</span></div>`);
                statsHtml = rows.join('');
            }
        } else if (isAdapter) {
            // === ПЕРЕВОДНИК — legendary ===
            typeLine = 'Переводник';
            qualityClass = 'quality5';

            const firstType     = WowTooltip.getValue(element.dataset.firstType);
            const firstDiameter = WowTooltip.getValue(element.dataset.firstDiameter);
            const secondType    = WowTooltip.getValue(element.dataset.secondType);
            const secondDiameter = WowTooltip.getValue(element.dataset.secondDiameter);

            const length = WowTooltip.getValue(element.dataset.length);
            const weight = WowTooltip.getValue(element.dataset.weight);
            const outerD = WowTooltip.getValue(element.dataset.outerDiameter);
            const innerD = WowTooltip.getValue(element.dataset.innerDiameter);

            statsHtml = `
                <div class="stat"><span>🔩 Первая сторона:</span><span class="stat-value">${WowTooltip.escapeHtml(firstType + ' ' + firstDiameter)}</span></div>
                <div class="stat"><span>🔩 Вторая сторона:</span><span class="stat-value">${WowTooltip.escapeHtml(secondType + ' ' + secondDiameter)}</span></div>
                <div class="stat"><span>📏 Длина:</span><span class="stat-value">${length} мм</span></div>
                <div class="stat"><span>⚖️ Вес:</span><span class="stat-value">${weight} кг</span></div>
                <div class="stat"><span>🔘 Наруж. диаметр:</span><span class="stat-value">${outerD} мм</span></div>
                <div class="stat"><span>🎯 Внутр. диаметр:</span><span class="stat-value">${innerD} мм</span></div>
            `;
        } else {
            // === МУФТА — epic ===
            const type = element.dataset.type || '';
            const diameter = element.dataset.diameter || '';
            typeLine = (type && diameter)
                ? `${WowTooltip.escapeHtml(type)} ${WowTooltip.escapeHtml(diameter)}`
                : 'Муфта';
            qualityClass = 'quality4';

            const length = WowTooltip.getValue(element.dataset.length);
            const weight = WowTooltip.getValue(element.dataset.weight);
            const outerD = WowTooltip.getValue(element.dataset.outerDiameter);
            const innerD = WowTooltip.getValue(element.dataset.innerDiameter);

            statsHtml = `
                <div class="stat"><span>📏 Длина:</span><span class="stat-value">${length} мм</span></div>
                <div class="stat"><span>⚖️ Вес:</span><span class="stat-value">${weight} кг</span></div>
                <div class="stat"><span>🔘 Наруж. диаметр:</span><span class="stat-value">${outerD} мм</span></div>
                <div class="stat"><span>🎯 Внутр. диаметр:</span><span class="stat-value">${innerD} мм</span></div>
            `;
        }

        WowTooltip.container.innerHTML = `
            <div class="tooltip-header">
                ${imageHtml}
                <div class="tooltip-title">
                    <div class="item-name ${qualityClass}">${WowTooltip.escapeHtml(name)}</div>
                    <div class="item-type">${WowTooltip.escapeHtml(typeLine)}</div>
                </div>
            </div>
            <div class="stats">${statsHtml}</div>
            <div class="prices">
                <div class="price-row"><span>Цена изготовления:</span><span class="coin"><i class="coin-icon"></i> ${cost} руб</span></div>
                <div class="price-row"><span>Цена для работника:</span><span class="coin"><i class="coin-icon"></i> ${price} руб</span></div>
            </div>
            ${description ? `<div class="description">📜 ${WowTooltip.escapeHtml(description)}</div>` : ''}
        `;
    }

    static position(event) {
        let x = event.clientX + 15;
        let y = event.clientY + 15;
        WowTooltip.container.style.left = x + 'px';
        WowTooltip.container.style.top = y + 'px';

        const rect = WowTooltip.container.getBoundingClientRect();
        if (x + rect.width > window.innerWidth) {
            WowTooltip.container.style.left = (event.clientX - rect.width - 10) + 'px';
        }
        if (y + rect.height > window.innerHeight) {
            WowTooltip.container.style.top = (event.clientY - rect.height - 10) + 'px';
        }
    }

    static hide() {
        if (WowTooltip.container) {
            WowTooltip.container.style.display = 'none';
        }
    }

    // Делегирование: один раз навешиваем на document.
    // Работает и после Turbo-навигации — новые .product-tooltip подхватываются автоматически.
    static init(selector) {
        if (!WowTooltip.container) {
            WowTooltip.container = document.createElement('div');
            WowTooltip.container.className = 'wow-tooltip';
            WowTooltip.container.style.display = 'none';
        }

        if (!WowTooltip.container.isConnected) {
            document.body.appendChild(WowTooltip.container);
        }

        WowTooltip._selector = selector;

        if (WowTooltip._delegated) return;
        WowTooltip._delegated = true;

        document.addEventListener('mouseover', (e) => {
            const el = e.target.closest(WowTooltip._selector);
            if (!el) return;
            if (e.relatedTarget && el.contains(e.relatedTarget)) return;

            WowTooltip.updateContent(el);
            WowTooltip.container.style.display = 'block';
            WowTooltip.position(e);
        });

        document.addEventListener('mousemove', (e) => {
            if (WowTooltip.container.style.display === 'block') {
                WowTooltip.position(e);
            }
        });

        document.addEventListener('mouseout', (e) => {
            const el = e.target.closest(WowTooltip._selector);
            if (!el) return;
            if (e.relatedTarget && el.contains(e.relatedTarget)) return;

            WowTooltip.container.style.display = 'none';
        });
    }
}

// ============================================================
// САМОЗАПУСК — не зависит от inline-скриптов в HTML.
// Класс объявлен выше, значит доступен в этой же области видимости.
// ============================================================
(function autoInitWowTooltip() {
    const SELECTOR = '.product-tooltip';

    function boot() {
        if (document.querySelector(SELECTOR)) {
            WowTooltip.init(SELECTOR);
        }
    }

    // DOM ещё парсится — ждём готовности
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', boot);
    } else {
        boot();
    }

    // Повторный boot при Turbo-навигации и при подгрузке в #app
    document.addEventListener('turbo:load', boot);
    document.addEventListener('turbo:frame-load', (e) => {
        if (e.target.id === 'app') boot();
    });

    // ─── Скрытие тултипа при Turbo-навигации ───
    // mouseout не срабатывает, когда Turbo выкидывает элемент
    // из-под курсора. Прячем тултип вручную на каждом переходе.
    document.addEventListener('turbo:before-frame-render', () => WowTooltip.hide());
    document.addEventListener('turbo:before-visit',       () => WowTooltip.hide());
    document.addEventListener('turbo:frame-load',         () => WowTooltip.hide());
    document.addEventListener('turbo:load',               () => WowTooltip.hide());

})();