// ============================================================
// КОМЕТА по контуру активной кнопки навбара.
// 30 перекрывающихся сегментов с интерполяцией цвета,
// толщины и прозрачности → визуально сплошной градиентный хвост.
// ============================================================
(function () {
    const NS = 'http://www.w3.org/2000/svg';
    const SPEED   = 55;    // px/сек по контуру
    const N       = 30;    // число сегментов
    const TAIL    = 0.25;  // длина хвоста как доля периметра
    const OVERLAP = 1.6;   // перекрытие соседних сегментов

    // Опорные точки градиента: [t, r, g, b, alpha, width]
    // t=0 — голова, t=1 — кончик хвоста
    const STOPS = [
        [0.00, 255, 255, 255, 1.00, 3.0],  // белая голова
        [0.05, 255, 243, 200, 0.98, 2.8],  // тёплый блик
        [0.15, 255, 217, 102, 0.85, 2.5],  // жёлтый
        [0.35, 235, 168,  70, 0.55, 2.1],  // оранжевый
        [0.60, 175, 105,  40, 0.28, 1.8],  // тёмный янтарь
        [0.82, 110,  60,  20, 0.10, 1.5],  // коричневый
        [1.00,  60,  30,   8, 0.00, 1.2],  // растворяется
    ];

    function sample(t) {
        for (let i = 1; i < STOPS.length; i++) {
            const a = STOPS[i - 1], b = STOPS[i];
            if (t <= b[0]) {
                const k = (t - a[0]) / (b[0] - a[0]);
                return {
                    r: a[1] + (b[1] - a[1]) * k,
                    g: a[2] + (b[2] - a[2]) * k,
                    b: a[3] + (b[3] - a[3]) * k,
                    a: a[4] + (b[4] - a[4]) * k,
                    w: a[5] + (b[5] - a[5]) * k,
                };
            }
        }
        const l = STOPS[STOPS.length - 1];
        return { r: l[1], g: l[2], b: l[3], a: l[4], w: l[5] };
    }

    function buildBeam(link) {
        // Гасим старый observer и убираем старый SVG
        const oldSvg = link.querySelector(':scope > svg.nav-beam');
        if (oldSvg) {
            if (oldSvg.__ro) oldSvg.__ro.disconnect();
            oldSvg.remove();
        }

        if (!link.classList.contains('active')) return;

        const svg = document.createElementNS(NS, 'svg');
        svg.setAttribute('class', 'nav-beam');
        svg.setAttribute('preserveAspectRatio', 'none');

        // Одно свечение на всю комету — быстрее, чем фильтр на каждом сегменте
        svg.style.filter =
            'drop-shadow(0 0 3px rgba(255, 220, 130, 0.85))' +
            ' drop-shadow(0 0 9px rgba(230, 126, 34, 0.55))' +
            ' drop-shadow(0 0 18px rgba(230, 126, 34, 0.30))';

        const rects = [];
        for (let i = 0; i < N; i++) {
            const t = i / (N - 1);
            const c = sample(t);
            if (c.a <= 0.005) continue;

            const r = document.createElementNS(NS, 'rect');
            r.setAttribute('fill', 'none');
            r.setAttribute('stroke', `rgb(${c.r | 0},${c.g | 0},${c.b | 0})`);
            r.setAttribute('stroke-width', c.w.toFixed(2));
            r.setAttribute('stroke-linecap', 'round');
            r.setAttribute('rx', '11');
            r.setAttribute('ry', '11');
            r.style.opacity = c.a.toFixed(3);
            r.dataset.t = t;

            svg.appendChild(r);
            rects.push(r);
        }

        // Если всё утонуло в прозрачности (не должно, но на всякий случай) — выходим
        if (rects.length === 0) return;

        link.appendChild(svg);

        function layout() {
            // Страховки: элемент может быть уже отключён от DOM
            // или скрыт (display:none у родителя) — тогда getTotalLength падает.
            if (!link.isConnected) return;
            if (!svg.isConnected) return;
            if (!link.offsetWidth || !link.offsetHeight) return;

            const w = link.offsetWidth;
            const h = link.offsetHeight;
            const W = w + 4, H = h + 4;

            svg.setAttribute('width',  W);
            svg.setAttribute('height', H);
            svg.setAttribute('viewBox', `0 0 ${W} ${H}`);

            rects.forEach(r => {
                r.setAttribute('x', 1);
                r.setAttribute('y', 1);
                r.setAttribute('width',  w + 2);
                r.setAttribute('height', h + 2);
            });

            let perim;
            try {
                perim = rects[0].getTotalLength();
            } catch (e) {
                return;
            }
            if (!perim || !isFinite(perim)) return;

            const duration = perim / SPEED;
            const tailPx   = perim * TAIL;
            const slot     = tailPx / (N - 1);
            const segLen   = slot * OVERLAP;

            rects.forEach(r => {
                const t = parseFloat(r.dataset.t);
                const d = t * tailPx;   // насколько сегмент отстаёт от головы

                // Один короткий отрезок на весь периметр — второго
                // («параллельного») не появится ни при каких условиях.
                r.setAttribute('stroke-dasharray',
                    `${segLen.toFixed(2)} ${(perim - segLen + 0.01).toFixed(2)}`);

                // Смещение сегмента на d назад — и тот же проход на -perim.
                r.style.setProperty('--beam-start', d.toFixed(2));
                r.style.setProperty('--beam-end',   (d - perim).toFixed(2));

                r.style.animationDuration = duration + 's';
                r.style.animationName = 'none';
                void r.getBoundingClientRect();    // рестарт анимации
                r.style.animationName = 'navBeamRun';
            });
        }

        layout();

        if (window.ResizeObserver) {
            const ro = new ResizeObserver(layout);
            ro.observe(link);
            svg.__ro = ro;
        }
    }

    window.refreshNavBeam = function () {
        document
            .querySelectorAll('.app-glass .navbar .nav-link')
            .forEach(buildBeam);
    };

    document.addEventListener('DOMContentLoaded', window.refreshNavBeam);
    document.addEventListener('turbo:load',     window.refreshNavBeam);
})();