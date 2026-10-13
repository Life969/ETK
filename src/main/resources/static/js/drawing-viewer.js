/**
 * drawing-viewer.js
 * Лайтбокс для чертежей: клик по картинке → плавный зум по центру.
 * Клик по увеличенной картинке → откат к обычному размеру.
 * Esc / клик мимо / крестик → закрытие.
 *
 * Работает для любых элементов с [data-drawing-src].
 */
window.DrawingViewer = (function () {

    let overlay = null;

    function ensureOverlay() {
        if (overlay) return overlay;

        overlay = document.createElement('div');
        overlay.className = 'drawing-lightbox';
        overlay.innerHTML = `
            <button type="button" class="drawing-lightbox-close" aria-label="Закрыть">
                <i class="bi bi-x-lg"></i>
            </button>
            <div class="drawing-lightbox-image-wrap">
                <img class="drawing-lightbox-image" alt="">
            </div>
        `;
        document.body.appendChild(overlay);

        // Закрытие по крестику / клику мимо картинки
        overlay.addEventListener('click', (e) => {
            if (e.target.closest('.drawing-lightbox-image')) return;
            close();
        });

        // Клик по картинке — toggle зума
        const img = overlay.querySelector('.drawing-lightbox-image');
        img.addEventListener('click', (e) => {
            e.stopPropagation();
            img.classList.toggle('zoomed');
        });

        // Esc
        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            if (!overlay.classList.contains('open')) return;
            close();
        });

        return overlay;
    }

    function open(src) {
        if (!src) return;
        const ov = ensureOverlay();
        const img = ov.querySelector('.drawing-lightbox-image');
        img.src = src;
        img.classList.remove('zoomed');

        // rAF — чтобы transition сработал
        requestAnimationFrame(() => {
            ov.classList.add('open');
        });
        document.body.style.overflow = 'hidden';
    }

    function close() {
        if (!overlay) return;
        overlay.classList.remove('open');
        document.body.style.overflow = '';
        const img = overlay.querySelector('.drawing-lightbox-image');
        setTimeout(() => { if (!overlay.classList.contains('open')) img.src = ''; }, 300);
    }

    // Делегирование кликов — новые картинки после Turbo-навигации подхватятся сами
    function init() {
        if (document.body.dataset.drawingViewerWired === 'true') return;
        document.body.dataset.drawingViewerWired = 'true';

        document.addEventListener('click', (e) => {
            const el = e.target.closest('[data-drawing-src]');
            if (!el) return;
            e.preventDefault();
            open(el.dataset.drawingSrc);
        });
    }

    return { init, open, close };
})();