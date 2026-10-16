document.addEventListener('DOMContentLoaded', () => {
    const sweepVideo = document.querySelector('.bear-sweep-video');
    if (!sweepVideo) return;

    // Прячем остальные видео
    document.querySelectorAll('.bear-video').forEach(v => {
        v.classList.remove('active');
        v.pause();
    });

    // Показываем и запускаем только подметание
    sweepVideo.classList.add('active');
    sweepVideo.playbackRate = 0.5;
    sweepVideo.play().catch(() => {});

    // =========================================
    // РУБИЛЬНИК + СВЕТ
    // =========================================
    (function initLightSwitch() {
        const switchBtn = document.getElementById('lightSwitch');
        if (!switchBtn) return;

        const STORAGE_KEY = 'zapolyarye-lights-on';

        if (localStorage.getItem(STORAGE_KEY) === '1') {
            document.body.classList.add('lights-on');
        }

        switchBtn.addEventListener('click', () => {
            const isOn = document.body.classList.toggle('lights-on');
            localStorage.setItem(STORAGE_KEY, isOn ? '1' : '0');
        });
    })();

    // =========================================
    // WELCOME — кнопка входа + логин-форма
    // =========================================
    (function initWelcomeAuth() {
        const btnEnter = document.getElementById('btnEnter');
        const btnBack = document.getElementById('btnBack');
        const startEl = document.getElementById('authStart');
        const formEl = document.getElementById('authForm');
        const panelBody = document.getElementById('panelBody');
        if (!btnEnter || !formEl || !panelBody) return;

        function measureHeight(el) {
            const prev = {
                position: el.style.position,
                opacity: el.style.opacity,
                pointer: el.style.pointerEvents,
                visibility: el.style.visibility,
            };
            el.style.position = 'static';
            el.style.opacity = '0';
            el.style.pointerEvents = 'none';
            el.style.visibility = 'hidden';

            const h = el.offsetHeight;

            el.style.position = prev.position;
            el.style.opacity = prev.opacity;
            el.style.pointerEvents = prev.pointer;
            el.style.visibility = prev.visibility;

            return h;
        }

        function setPanelHeight(targetState) {
            const targetHeight = measureHeight(targetState);
            panelBody.style.height = targetHeight + 'px';
        }

        // ⚡ ВОТ ЭТО МЫ ДОБАВИЛИ:
        // Если в URL есть ?error — значит Spring Security вернул нас
        // на страницу логина после неудачной попытки.
        // Сразу показываем форму и фокусимся на логине.
        const hasError = new URLSearchParams(location.search).has('error');

        if (hasError) {
            startEl.style.display = 'none';
            // form уже отмечен .active через Thymeleaf (th:classappend)
            // в welcome.html — сюда JS не лезет.
        }

        // Стартовая высота — по нужному состоянию
        setPanelHeight(hasError ? formEl : startEl);

        // Фокус на поле логина при ошибке
        if (hasError) {
            setTimeout(() => {
                const input = document.getElementById('username');
                if (input) input.focus();
            }, 350);
        }

        // Пересчёт при ресайзе
        window.addEventListener('resize', () => {
            const active = startEl.classList.contains('active') ? startEl : formEl;
            setPanelHeight(active);
        });

        // Взрыв кнопки с частицами
        function burstParticles(btn) {
            const rect = btn.getBoundingClientRect();
            const cx = rect.left + rect.width / 2;
            const cy = rect.top + rect.height / 2;
            const COUNT = 26;
            const colors = ['#ff9540', '#e67e22', '#c0392b', '#ffd07a', '#f5e8d0'];

            for (let i = 0; i < COUNT; i++) {
                const p = document.createElement('div');
                p.className = 'burst-particle';
                p.style.left = cx + 'px';
                p.style.top = cy + 'px';
                p.style.background = `radial-gradient(circle at 30% 30%, #fff, ${colors[i % colors.length]})`;

                const angle = (Math.PI * 2 * i) / COUNT + (Math.random() - 0.5) * 0.6;
                const dist = 90 + Math.random() * 130;
                p.style.setProperty('--dx', (Math.cos(angle) * dist).toFixed(0) + 'px');
                p.style.setProperty('--dy', (Math.sin(angle) * dist).toFixed(0) + 'px');

                const size = 6 + Math.random() * 6;
                p.style.width = size + 'px';
                p.style.height = size + 'px';
                p.style.animationDelay = (Math.random() * 60) + 'ms';

                document.body.appendChild(p);
                setTimeout(() => p.remove(), 1200);
            }
        }

        // Переход к форме логина
        function showLoginForm() {
            setPanelHeight(formEl);
            startEl.classList.remove('active');
            formEl.classList.add('active');

            setTimeout(() => {
                btnEnter.classList.remove('bursting');
                startEl.style.display = 'none';
            }, 700);

            setTimeout(() => {
                const input = document.getElementById('username');
                if (input) input.focus();
            }, 400);
        }

        // Возврат к кнопке
        function showStart() {
            startEl.style.display = '';
            btnEnter.classList.remove('bursting');
            setPanelHeight(startEl);

            formEl.classList.remove('active');
            startEl.classList.add('active');

            const u = document.getElementById('username');
            const p = document.getElementById('password');
            if (u) u.value = '';
            if (p) p.value = '';
        }

        // Клик по "Войти"
        btnEnter.addEventListener('click', () => {
            btnEnter.classList.add('bursting');
            burstParticles(btnEnter);

            const isAuth = btnEnter.dataset.auth === 'true';

            setTimeout(() => {
                if (isAuth) {
                    window.location.href = '/layout';
                } else {
                    showLoginForm();
                }
            }, 420);
        });

        // Клик по "Назад"
        if (btnBack) btnBack.addEventListener('click', showStart);
    })();
});