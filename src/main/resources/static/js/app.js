/**
 * app.js
 * Оркестратор страницы:
 *  - инициализация тултипов, форм заказов и подсветки навбара
 *  - активная вкладка в навбаре (по URL + мгновенный отклик на клик)
 *  - превью изображения в формах
 *  - аккордеон справочников
 */
(function () {

    // ============================================================
    // Тултипы + инициализация страниц заказов
    // ============================================================
    function initPageScripts() {
        if (window.WowTooltip) WowTooltip.init('.product-tooltip');
        if (window.OrdersForm) OrdersForm.init();
        if (window.JournalForm) JournalForm.init();
        if (window.refreshNavBeam) window.refreshNavBeam();
    }

    document.addEventListener('DOMContentLoaded', initPageScripts);
    document.addEventListener('turbo:load', initPageScripts);
    document.addEventListener('turbo:frame-load', (e) => {
        if (e.target.id === 'app') initPageScripts();
    });
    if (document.readyState !== 'loading') initPageScripts();

    // ============================================================
    // ПОДСВЕТКА АКТИВНОЙ ВКЛАДКИ В НАВБАРЕ
    // ============================================================
    function setNavActive(section) {
        document.querySelectorAll('.app-glass .navbar .nav-link[data-nav]').forEach(link => {
            link.classList.toggle('active', link.dataset.nav === section);
        });

        const brand = document.querySelector('.app-glass .navbar .navbar-brand');
        if (brand) brand.classList.toggle('active', section === 'home');

        if (window.refreshNavBeam) window.refreshNavBeam();
    }

    /**
     * Определяем секцию по текущему URL.
     * Используем includes() — на случай context-path (/app/orders/waiting).
     */
    function detectActiveSection() {
        const p = window.location.pathname;

        if (p.includes('/orders'))             return 'orders';
        if (p.includes('/reports'))            return 'reports';
        if (p.includes('/production-records')) return 'journal';

        if (p.includes('/couplings') ||
            p.includes('/pipe-adapters') ||
            p.includes('/external-threads') ||
            p.includes('/employees') ||
            p.includes('/machines') ||
            p.includes('/additionalWork') ||
            p.includes('/steel-groups'))       return 'handbooks';

        return 'home';
    }

    /**
     * Ставим подсветку с задержкой 0 — даём Turbo сначала
     * обновить window.location. Иначе на turbo:frame-load
     * URL ещё старый, и подсветка слетает на «Главную».
     */
    function syncNavFromUrl() {
        setTimeout(() => setNavActive(detectActiveSection()), 0);
    }

    // ─── Мгновенный отклик на клики в навбаре ───
    document.addEventListener('click', (e) => {

        // 0. Клик по логотипу-листику
        const brand = e.target.closest('.app-glass .navbar .navbar-brand');
        if (brand) {
            setNavActive('home');
            return;
        }

        // 1. Клик по верхней ссылке БЕЗ дропдауна
        const topLink = e.target.closest('.app-glass .navbar .nav-link[data-nav]');
        if (topLink && !topLink.classList.contains('dropdown-toggle')) {
            setNavActive(topLink.dataset.nav);
            return;
        }

        // 2. Клик по пункту дропдауна
        const dropdownItem = e.target.closest('.app-glass .navbar .dropdown-item');
        if (dropdownItem) {
            const parentItem = dropdownItem.closest('.nav-item.dropdown');
            const parentLink = parentItem?.querySelector('.nav-link[data-nav]');
            if (parentLink) setNavActive(parentLink.dataset.nav);
            return;
        }

        // 3. Клик по любой внутренней ссылке без data-nav (например,
        //    «компания» в срочном заказе на главной) — сразу ставим
        //    подсветку по целевому URL, чтобы не ждать Turbo.
        const anyLink = e.target.closest('a[href^="/"]');
        if (anyLink && !anyLink.target) {
            const href = anyLink.getAttribute('href') || '';
            if (href.includes('/orders'))             setNavActive('orders');
            else if (href.includes('/reports'))       setNavActive('reports');
            else if (href.includes('/production-records')) setNavActive('journal');
            else if (href.includes('/couplings') ||
                href.includes('/pipe-adapters') ||
                href.includes('/external-threads') ||
                href.includes('/employees') ||
                href.includes('/machines') ||
                href.includes('/additionalWork') ||
                href.includes('/steel-groups')) setNavActive('handbooks');
            else if (href === '/home' || href === '/') setNavActive('home');
        }
    });

    // ─── Автодетект по URL: при загрузке и на каждой Turbo-навигации ───
    document.addEventListener('DOMContentLoaded', syncNavFromUrl);
    document.addEventListener('turbo:load',       syncNavFromUrl);
    document.addEventListener('turbo:frame-load', (e) => {
        if (e.target.id === 'app') syncNavFromUrl();
    });

    // ─── Страховка на pushState/replaceState/popstate ───
    // Turbo использует history.pushState под капотом. Ловим это —
    // тогда подсветка гарантированно синхронизируется с URL.
    ['pushState', 'replaceState'].forEach(method => {
        const orig = history[method];
        history[method] = function () {
            const result = orig.apply(this, arguments);
            window.dispatchEvent(new Event('locationchange'));
            return result;
        };
    });
    window.addEventListener('locationchange', syncNavFromUrl);
    window.addEventListener('popstate',       syncNavFromUrl);

    // ============================================================
    // Превью изображения в формах справочников
    // ============================================================
    document.addEventListener('change', (e) => {
        if (e.target && e.target.id === 'imageFileInput') {
            const file = e.target.files[0];
            if (!file) return;
            if (file.size > 5 * 1024 * 1024) {
                alert('Файл слишком большой. Максимальный размер 5 МБ.');
                e.target.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = function (ev) {
                const hidden = document.getElementById('imageBase64');
                if (hidden) hidden.value = ev.target.result;
            };
            reader.readAsDataURL(file);
        }
    });

    // ============================================================
    // Аккордеон справочников
    // ============================================================
    document.addEventListener('click', (e) => {
        const toggle = e.target.closest('.hb-accordion-toggle');
        if (!toggle) return;
        const targetId = toggle.dataset.target;
        const body = document.getElementById(targetId);
        if (!body) return;
        toggle.classList.toggle('open');
        body.classList.toggle('open');
    });

    // ============================================================
    // УМНАЯ КНОПКА «НАЗАД» — запоминаем откуда пришёл юзер
    // ============================================================

    const RETURN_TO_KEY = 'orders:returnTo';
    const LIST_PATHS = ['/orders/waiting', '/orders/production', '/orders/completed'];

    /**
     * Запоминаем URL последней «нетранзитной» страницы.
     * Вызывается при каждой загрузке (в т.ч. Turbo-навигации).
     */
    function rememberReturnUrl() {
        const p = window.location.pathname;
        const search = window.location.search || '';

        if (LIST_PATHS.includes(p)) {
            sessionStorage.setItem(RETURN_TO_KEY, p + search);
            return;
        }
        if (p === '/home' || p === '/' || p === '') {
            sessionStorage.setItem(RETURN_TO_KEY, '/home');
        }
        // view / edit / create / другие — не трогаем память.
    }

    document.addEventListener('DOMContentLoaded', rememberReturnUrl);
    document.addEventListener('turbo:load', rememberReturnUrl);
    document.addEventListener('turbo:frame-load', (e) => {
        if (e.target.id === 'app') rememberReturnUrl();
    });

    /**
     * Клик по кнопке «Назад» с атрибутом data-smart-back.
     * Идём по запомненному URL или по фолбэку (data-back-fallback).
     */
    document.addEventListener('click', function (e) {
        const btn = e.target.closest('[data-smart-back]');
        if (!btn) return;

        e.preventDefault();
        e.stopPropagation();

        const fallback = btn.dataset.backFallback || '/orders/waiting';
        const url = sessionStorage.getItem(RETURN_TO_KEY) || fallback;

        // Переход внутри turbo-frame="app" — без дёрга
        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('data-turbo-frame', 'app');
        link.style.cssText = 'position:absolute;left:-9999px;top:-9999px;';
        document.body.appendChild(link);
        link.click();
        setTimeout(() => link.remove(), 500);
    }, true);

})();