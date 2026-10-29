/**
 * handbook.js
 * Инициализация страниц справочников:
 *   - аккордеон по типам (муфты, переводники, наружные резьбы…)
 *   - клик по карточке → навигация внутри turbo-frame="app"
 *
 * Идемпотентно. Вызывается из app.js на DOMContentLoaded / turbo:load /
 * turbo:frame-load (#app).
 */
window.Handbooks = (function () {


    function initDrawingUpload() {
        if (document.body.dataset.hbDrawingWired === 'true') return;
        document.body.dataset.hbDrawingWired = 'true';

        document.addEventListener('change', (e) => {
            if (!e.target || e.target.id !== 'drawingFileInput') return;

            const file = e.target.files[0];
            if (!file) return;

            const MAX = 10 * 1024 * 1024;
            if (file.size > MAX) {
                alert('Файл слишком большой. Максимум 10 МБ.');
                e.target.value = '';
                return;
            }

            const okTypes = ['application/pdf', 'image/jpeg', 'image/png'];
            if (!okTypes.includes(file.type)) {
                alert('Поддерживаются PDF, JPG, PNG.');
                e.target.value = '';
                return;
            }

            const reader = new FileReader();
            reader.onload = (ev) => {
                const hidden  = document.getElementById('drawingBase64');
                const nameEl  = document.getElementById('drawingOriginalName');
                if (hidden) hidden.value = ev.target.result;
                if (nameEl) nameEl.value = file.name;
            };
            reader.readAsDataURL(file);
        });
    }

    function initDrawingPanel() {
        if (document.body.dataset.hbDrawingPanelWired === 'true') return;
        document.body.dataset.hbDrawingPanelWired = 'true';

        document.addEventListener('click', (e) => {
            const btn = e.target.closest('#drawingToggle');
            if (!btn) return;

            const panel = document.getElementById('drawingPanel');
            if (!panel) return;

            const willOpen = !panel.classList.contains('open');
            if (willOpen) {
                panel.classList.add('open');
                panel.style.maxHeight = panel.scrollHeight + 'px';

                const onEnd = (ev) => {
                    if (ev.propertyName !== 'max-height') return;
                    panel.style.maxHeight = 'none';
                    panel.removeEventListener('transitionend', onEnd);
                };
                panel.addEventListener('transitionend', onEnd);

                btn.classList.add('active');
            } else {
                panel.style.maxHeight = panel.scrollHeight + 'px';
                panel.classList.remove('open');
                void panel.offsetHeight;
                panel.style.maxHeight = '0';
                btn.classList.remove('active');
            }
        });
    }

    // ================================================================
    // АККОРДЕОН ПО ТИПАМ
    // ================================================================
    function initAccordion() {
        if (document.body.dataset.hbAccordionWired === 'true') return;
        document.body.dataset.hbAccordionWired = 'true';

        document.addEventListener('click', (e) => {
            const toggle = e.target.closest('.hb-accordion-toggle');
            if (!toggle) return;

            const body = document.getElementById(toggle.dataset.target);
            if (!body) return;

            const willOpen = !toggle.classList.contains('open');
            toggle.classList.toggle('open', willOpen);

            if (willOpen) {
                // 1. Меряем реальную высоту контента (max-height не влияет на scrollHeight)
                const target = body.scrollHeight;
                // 2. Открываем состояние
                body.classList.add('open');
                // 3. Ставим точную высоту → плавно едем от 0 к ней
                body.style.maxHeight = target + 'px';

                // 4. После анимации снимаем ограничение — чтобы при resize высота подстраивалась
                const onEnd = (ev) => {
                    if (ev.propertyName !== 'max-height') return;
                    body.style.maxHeight = 'none';
                    body.removeEventListener('transitionend', onEnd);
                };
                body.addEventListener('transitionend', onEnd);
            } else {
                // Закрываем: сначала фиксируем текущую высоту, потом едем в 0
                body.style.maxHeight = body.scrollHeight + 'px';
                body.classList.remove('open');
                void body.offsetHeight;       // форсируем reflow
                body.style.maxHeight = '0';
            }
        });
    }

    // ================================================================
    // КЛИКАБЕЛЬНАЯ КАРТОЧКА СПРАВОЧНИКА
    // Любой .handbook-card[data-card-href] ведёт себя как ссылка,
    // кроме кликов по внутренним интерактивным элементам.
    // ================================================================
    function initCardClicks() {
        if (document.body.dataset.hbCardsWired === 'true') return;
        document.body.dataset.hbCardsWired = 'true';

        document.addEventListener('click', (e) => {
            // Интерактивные элементы внутри карточки — не мешаем им
            if (e.target.closest('a, button, form, input, select, textarea, label, [data-no-card-click]')) {
                return;
            }

            const card = e.target.closest('.handbook-card');
            if (!card) return;

            // Выделение текста — пользователь что-то копирует, не навигируем
            const sel = window.getSelection();
            if (sel && sel.toString().length > 0) return;

            const href = card.dataset.cardHref;
            if (!href) return;

            // Ctrl / Cmd / Shift / СКМ — открыть в новой вкладке
            if (e.ctrlKey || e.metaKey || e.shiftKey || e.button === 1) {
                e.preventDefault();
                window.open(href, '_blank', 'noopener');
                return;
            }

            if (e.button !== 0) return;

            navigateFrame(href);
        });
    }

    // ================================================================
    // Навигация через фейковую ссылку — Turbo подхватит data-turbo-frame
    // ================================================================
    function navigateFrame(url) {
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('data-turbo-frame', 'app');
        link.style.cssText = 'position:absolute;left:-9999px;top:-9999px;';
        document.body.appendChild(link);
        link.click();
        setTimeout(() => link.remove(), 500);
    }

    // ================================================================
    // УДАЛЕНИЕ КАРТОЧКИ — плавное схлопывание + ветер
    // ================================================================
    function initHandbookDeletion() {
        if (document.body.dataset.hbDeleteWired === 'true') return;
        document.body.dataset.hbDeleteWired = 'true';

        document.addEventListener('click', async (e) => {
            const btn = e.target.closest('[data-hb-delete]');
            if (!btn) return;
            if (btn.disabled) return;

            const card = btn.closest('.handbook-card');
            if (!card) return;

            const url   = btn.dataset.hbDelete;
            const label = btn.dataset.hbDeleteLabel || 'запись';
            if (!url) return;

            e.preventDefault();
            e.stopPropagation();

            if (!confirm(`Удалить ${label}?`)) return;

            btn.disabled = true;

            const rect = card.getBoundingClientRect();
            const cx = rect.left + rect.width / 2;
            const cy = rect.top  + rect.height / 2;

            // 1. Плавно схлопываем карточку
            card.style.pointerEvents = 'none';
            card.style.transition =
                'transform 0.42s cubic-bezier(0.55, 0, 0.55, 1), opacity 0.42s ease';
            card.style.transform = 'scale(0.55)';
            card.style.opacity = '0';

            // 2. Параллельно выпускаем листик с ветром (чуть с задержкой)
            setTimeout(() => spawnWindLeaf(cx, cy), 200);

            // 3. После схлопывания — AJAX-удаление
            setTimeout(async () => {
                try {
                    const headers = {
                        'Accept': 'text/html',
                        'X-Requested-With': 'XMLHttpRequest'
                    };
                    const tokenMeta  = document.querySelector('meta[name="_csrf"]');
                    const headerMeta = document.querySelector('meta[name="_csrf_header"]');
                    if (tokenMeta && headerMeta && tokenMeta.content) {
                        headers[headerMeta.content] = tokenMeta.content;
                    }

                    const response = await fetch(url, {
                        method: 'POST',
                        headers: headers,
                        credentials: 'same-origin'
                    });

                    if (!response.ok) {
                        throw new Error('HTTP ' + response.status);
                    }

                    onHandbookCardDeleted(card);

                } catch (err) {
                    console.error('Ошибка удаления:', err);
                    alert('Не удалось удалить. Попробуйте ещё раз.');

                    // Возвращаем карточку как было
                    card.style.transition = '';
                    card.style.transform  = '';
                    card.style.opacity    = '';
                    card.style.pointerEvents = '';
                    btn.disabled = false;
                }
            }, 450);
        });
    }

    // Пересчёт счётчиков + автоудаление пустой группы
    function onHandbookCardDeleted(card) {
        const grid = card.closest('.hb-cards-grid');
        const item = card.closest('.hb-accordion-item');

        if (!grid || !item) {
            card.remove();
            return;
        }

        card.remove();

        // Обновляем счётчик позиций в шапке группы
        const remaining = grid.querySelectorAll('.handbook-card').length;
        const countEl = item.querySelector('.hb-accordion-count');
        if (countEl) countEl.textContent = remaining + ' поз.';

        // Обновляем общий счётчик справочника
        const totalEl = document.querySelector('.handbook-count');
        if (totalEl) {
            const m = totalEl.textContent.match(/(\d+)/);
            if (m) {
                const n = Math.max(0, parseInt(m[1], 10) - 1);
                const suffix = totalEl.textContent.includes('шт.') ? ' шт.' : ' поз.';
                totalEl.textContent = n + suffix;
            }
        }

        // Группа опустела — плавно убираем весь аккордеон
        if (remaining === 0) {
            item.style.transition =
                'max-height 0.45s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.35s ease, margin-bottom 0.45s ease';
            item.style.maxHeight = item.scrollHeight + 'px';
            item.style.overflow  = 'hidden';
            void item.offsetHeight;
            item.style.maxHeight    = '0';
            item.style.opacity      = '0';
            item.style.marginBottom = '0';
            setTimeout(() => item.remove(), 500);
        }
    }

    // Ветер с листиком — без пузыря.
    // Стили .leaf-flyer--wind и .wind-spark уже есть в styles.css.
    function spawnWindLeaf(cx, cy) {
        const layer = document.createElement('div');
        layer.className = 'leaf-flight-layer';

        const flyer = document.createElement('div');
        flyer.className = 'leaf-flyer leaf-flyer--wind';
        flyer.style.left = cx + 'px';
        flyer.style.top  = cy + 'px';

        const img = document.createElement('img');
        img.src = '/images/red_leaf.png';
        img.alt = '';
        img.setAttribute('aria-hidden', 'true');
        flyer.appendChild(img);
        layer.appendChild(flyer);

        const ex = window.innerWidth + 220;
        const ey = -140;
        const dx = ex - cx;
        const dy = ey - cy;

        // Искры ветра в шлейфе
        for (let i = 0; i < 8; i++) {
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
            { transform: 'translate(-50%, -50%) translate(0px, 0px) scale(0.35) rotate(-30deg)', opacity: 0 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.06}px, ${dy * 0.18}px) scale(1.28) rotate(80deg)`,  opacity: 1, offset: 0.12 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.20}px, ${dy * 0.02}px) scale(1.18) rotate(190deg)`, opacity: 1, offset: 0.28 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.42}px, ${dy * 0.38}px) scale(1.10) rotate(320deg)`, opacity: 1, offset: 0.48 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.66}px, ${dy * 0.60}px) scale(1.02) rotate(450deg)`, opacity: 1, offset: 0.68 },
            { transform: `translate(-50%, -50%) translate(${dx * 0.85}px, ${dy * 0.84}px) scale(0.88) rotate(570deg)`, opacity: 1, offset: 0.88 },
            { transform: `translate(-50%, -50%) translate(${dx}px, ${dy}px) scale(0.5) rotate(700deg)`,              opacity: 0 }
        ], {
            duration: 1450,
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
        setTimeout(finish, 1900);
    }

    // ================================================================
    // ОБЩИЙ ВХОД
    // ================================================================
    function init() {
        initAccordion();
        initCardClicks();
        initHandbookDeletion();
        initDrawingUpload();
        initDrawingPanel();
        if (window.DrawingViewer) DrawingViewer.init();
    }

    return { init };
})();