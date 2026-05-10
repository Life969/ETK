class WowTooltip {
    // Единый контейнер на страницу
    static container = null;

    // Безопасный вывод текста
    static escapeHtml(str) {
        if (!str) return '';
        return str.replace(/[&<>]/g, function (m) {
            if (m === '&') return '&amp;';
            if (m === '<') return '&lt;';
            if (m === '>') return '&gt;';
            return m;
        });
    }

    // Простановка значения по умолчанию
    static getValue(value, defaultValue = '—') {
        if (value === undefined || value === null || value === '') return defaultValue;
        return value;
    }

    // Строим HTML тултипа из data-атрибутов элемента
    static updateContent(element) {
        const isAdapter = element.dataset.firstType !== undefined; // есть firstType — переводник

        const name = element.dataset.name || 'Без названия';
        const length = WowTooltip.getValue(element.dataset.length);
        const weight = WowTooltip.getValue(element.dataset.weight);
        const outerD = WowTooltip.getValue(element.dataset.outerDiameter);
        const innerD = WowTooltip.getValue(element.dataset.innerDiameter);
        const cost = WowTooltip.getValue(element.dataset.cost);
        const price = WowTooltip.getValue(element.dataset.price);
        let description = element.dataset.description || '';
        const imagePath = element.dataset.image;

        if (description.length > 200) {
            description = description.substring(0, 200) + '...';
        }

        // Изображение или иконка
        let imageHtml = '';
        if (imagePath && imagePath !== '') {
            let imgUrl = imagePath;
            if (!imgUrl.startsWith('/') && !imgUrl.startsWith('http')) {
                imgUrl = isAdapter ? '/uploads/adapters/' + imgUrl : '/uploads/couplings/' + imgUrl;
            }
            imageHtml = `<div class="tooltip-image"><img src="${WowTooltip.escapeHtml(imgUrl)}" alt="icon" onerror="this.style.display='none'"></div>`;
        } else {
            imageHtml = `<div class="tooltip-image"><i class="bi bi-cup-straw" style="font-size:24px; color:#c69b6d;"></i></div>`;
        }

        // Тип изделия и quality-класс
        let typeLine, qualityClass;
        if (isAdapter) {
            typeLine = 'Переводник';
            qualityClass = 'quality5';
        } else {
            const type = element.dataset.type || '';
            const diameter = element.dataset.diameter || '';
            typeLine = (type && diameter)
                ? `${WowTooltip.escapeHtml(type)} ${WowTooltip.escapeHtml(diameter)}`
                : 'Муфта';
            qualityClass = 'quality4';
        }

        WowTooltip.container.innerHTML = `
            <div class="tooltip-header">
                ${imageHtml}
                <div class="tooltip-title">
                    <div class="item-name ${qualityClass}">${WowTooltip.escapeHtml(name)}</div>
                    <div class="item-type">${WowTooltip.escapeHtml(typeLine)}</div>
                </div>
            </div>
            <div class="stats">
                <div class="stat"><span>📏 Длина:</span><span class="stat-value">${length} мм</span></div>
                <div class="stat"><span>⚖️ Вес:</span><span class="stat-value">${weight} кг</span></div>
                <div class="stat"><span>🔘 Наруж. диаметр:</span><span class="stat-value">${outerD} мм</span></div>
                <div class="stat"><span>🎯 Внутр. диаметр:</span><span class="stat-value">${innerD} мм</span></div>
            </div>
            <div class="prices">
                <div class="price-row"><span>Цена изготовления:</span><span class="coin"><i class="coin-icon"></i> ${cost} руб</span></div>
                <div class="price-row"><span>Цена для работника:</span><span class="coin"><i class="coin-icon"></i> ${price} руб</span></div>
            </div>
            ${description ? `<div class="description">📜 ${WowTooltip.escapeHtml(description)}</div>` : ''}
        `;
    }

    // Позиционирование тултипа с корректировкой, чтобы не вылезал за экран
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

    // Главный метод — привязать тултип ко всем элементам по селектору
    static init(selector) {
        // Создаём контейнер, если ещё нет
        if (!WowTooltip.container) {
            WowTooltip.container = document.createElement('div');
            WowTooltip.container.className = 'wow-tooltip';
            WowTooltip.container.style.display = 'none';
            document.body.appendChild(WowTooltip.container);
        }

        const elements = document.querySelectorAll(selector);
        elements.forEach(el => {
            el.addEventListener('mouseenter', (e) => {
                WowTooltip.updateContent(el);
                WowTooltip.container.style.display = 'block';
                WowTooltip.position(e);
            });
            el.addEventListener('mousemove', WowTooltip.position);
            el.addEventListener('mouseleave', () => {
                WowTooltip.container.style.display = 'none';
            });
        });
    }
}