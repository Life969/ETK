// ============================================================
// КОМЕТА по контуру активной кнопки навбара.
// Медленная, мягкая, с плавным появлением при turbo-навигации.
// ============================================================
(function () {
    const NS = 'http://www.w3.org/2000/svg';
    const SPEED   = 34;    // ← было 55 — теперь медленнее в ~2 раза
    const N       = 32;    // сегментов чуть больше — хвост плавнее
    const TAIL    = 0.25;  // ← было 0.25 — хвост длиннее
    const OVERLAP = 2.0;   // ← было 1.6 — больше перекрытие

    // Опорные точки градиента: [t, r, g, b, alpha, width]
    // t=0 — голова, t=1 — кончик хвоста.
    // Голова — не чисто-белая, а тёплая бежевая: мягче, не «режет» глаз.
    const STOPS = [
        [0.00, 255, 245, 220, 0.78, 2.4],  // тёплая голова
        [0.06, 255, 230, 170, 0.62, 2.2],
        [0.18, 255, 205, 110, 0.44, 2.0],
        [0.38, 235, 160,  70, 0.26, 1.8],
        [0.62, 175, 100,  40, 0.13, 1.5],
        [0.84, 110,  60,  20, 0.05, 1.3],
        [1.00,  60,  30,   8, 0.00, 1.1],
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

        // Мягкое свечение — три слоя, но слабее и уже
        svg.style.filter =
            'drop-shadow(0 0 2px rgba(255, 220, 130, 0.45))' +
            ' drop-shadow(0 0 6px rgba(230, 126, 34, 0.28))' +
            ' drop-shadow(0 0 12px rgba(230, 126, 34, 0.14))';

        // Плавное появление при turbo-навигации
        svg.style.opacity = '0';
        svg.style.transition = 'opacity 1.2s ease-out';

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

        if (rects.length === 0) return;

        link.appendChild(svg);

        // Запускаем fade-in на следующем кадре, чтобы transition сработал
        requestAnimationFrame(() => {
            requestAnimationFrame(() => {
                svg.style.opacity = '1';
            });
        });

        function layout() {
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
                const d = t * tailPx;

                r.setAttribute('stroke-dasharray',
                    `${segLen.toFixed(2)} ${(perim - segLen + 0.01).toFixed(2)}`);

                r.style.setProperty('--beam-start', d.toFixed(2));
                r.style.setProperty('--beam-end',   (d - perim).toFixed(2));

                r.style.animationDuration = duration + 's';
                r.style.animationName = 'none';
                void r.getBoundingClientRect();
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