class WaitingOrders {

    static getCsrfToken() {
        const tokenMeta = document.querySelector('meta[name="_csrf"]');
        const headerMeta = document.querySelector('meta[name="_csrf_header"]');
        if (!tokenMeta || !headerMeta) {
            console.warn('CSRF meta tags not found');
            return {token: '', header: ''};
        }
        return {
            token: tokenMeta.content,
            header: headerMeta.content
        };
    }

    // AJAX-запрос на изменение статуса (когда дедлайн уже есть)
    static async changeStatus(orderId) {
        const {token, header} = WaitingOrders.getCsrfToken();

        const url = `/orders/${orderId}/status`;
        const formData = new URLSearchParams();
        formData.append('newStatus', 'IN_PRODUCTION');
        const returnTo = window.location.pathname + window.location.search;
        formData.append('returnTo', returnTo);

        const fetchHeaders = {
            'Content-Type': 'application/x-www-form-urlencoded'
        };
        if (header && token) {
            fetchHeaders[header] = token;
        }

        const response = await fetch(url, {
            method: 'POST',
            headers: fetchHeaders,
            body: formData.toString()
        });

        if (response.ok) {
            window.location.reload();
        } else {
            const text = await response.text();
            alert('Ошибка при переводе в производство: ' + text);
        }
    }

    // Инициализация модального окна дедлайна
    static initDeadlineModal() {
        const modalEl = document.getElementById('deadlineModal');
        if (!modalEl) return;
        const modal = new bootstrap.Modal(modalEl);

        document.querySelectorAll('.start-production-btn').forEach(btn => {
            btn.addEventListener('click', async (e) => {
                e.preventDefault();
                const orderId = btn.dataset.orderId;
                const hasDeadline = btn.dataset.hasDeadline === 'true';
                if (hasDeadline) {
                    await WaitingOrders.changeStatus(orderId);
                } else {
                    document.getElementById('orderIdForDeadline').value = orderId;
                    modal.show();
                }
            });
        });

        document.getElementById('confirmDeadlineBtn').addEventListener('click', async () => {
            const {token, header} = WaitingOrders.getCsrfToken();

            const orderId = document.getElementById('orderIdForDeadline').value;
            const deadline = document.getElementById('deadlineInput').value;
            if (!deadline) {
                alert('Пожалуйста, выберите дату дедлайна');
                return;
            }

            const url = `/orders/${orderId}/set-deadline-and-start`;
            const formData = new URLSearchParams();
            formData.append('deadline', deadline);

            const fetchHeaders = {
                'Content-Type': 'application/x-www-form-urlencoded'
            };
            if (header && token) {
                fetchHeaders[header] = token;
            }

            const response = await fetch(url, {
                method: 'POST',
                headers: fetchHeaders,
                body: formData.toString()
            });

            const result = await response.json();
            if (result.success) {
                modal.hide();
                window.location.reload();
            } else {
                alert('Ошибка: ' + result.error);
            }
        });
    }

    // Логика кнопок КП
    static initOfferButtons() {
        const offerActions = document.getElementById('offerActions');
        const offerCount = document.getElementById('offerCount');

        // Обновление панели КП по текущему состоянию кнопок
        function refreshOfferPanel() {
            const activeButtons = document.querySelectorAll('.add-to-offer-btn.btn-success');
            const count = activeButtons.length;
            if (offerCount) offerCount.textContent = count;
            if (offerActions) {
                offerActions.style.display = count > 0 ? 'flex' : 'none';
            }
        }

        // Навешиваем обработчики на все кнопки "+" / "✓"
        document.querySelectorAll('.add-to-offer-btn').forEach(btn => {
            btn.addEventListener('click', async function (e) {
                e.preventDefault();
                const {token, header} = WaitingOrders.getCsrfToken();
                const orderId = this.dataset.orderId;
                try {
                    const fetchHeaders = {
                        'Content-Type': 'application/x-www-form-urlencoded'
                    };
                    if (header && token) {
                        fetchHeaders[header] = token;
                    }

                    const response = await fetch(`/commercial-offer/add/${orderId}`, {
                        method: 'POST',
                        headers: fetchHeaders
                    });
                    const data = await response.json();
                    if (data.error) {
                        alert(data.error);
                        return;
                    }
                    // Меняем внешний вид кнопки в соответствии с ответом
                    const icon = this.querySelector('i');
                    if (data.added) {
                        this.classList.remove('btn-outline-success');
                        this.classList.add('btn-success');
                        icon.classList.remove('bi-plus-lg');
                        icon.classList.add('bi-check-lg');
                    } else {
                        this.classList.remove('btn-success');
                        this.classList.add('btn-outline-success');
                        icon.classList.remove('bi-check-lg');
                        icon.classList.add('bi-plus-lg');
                    }
                    refreshOfferPanel();
                } catch (err) {
                    console.error('Ошибка:', err);
                }
            });
        });

        // Кнопка очистки
        const clearBtn = document.getElementById('clearOfferBtn');
        if (clearBtn) {
            clearBtn.addEventListener('click', async function () {
                const {token, header} = WaitingOrders.getCsrfToken();
                const fetchHeaders = {};
                if (header && token) {
                    fetchHeaders[header] = token;
                }
                await fetch('/commercial-offer/clear', {
                    method: 'POST',
                    headers: fetchHeaders
                });
                window.location.reload();
            });
        }

        // При загрузке страницы синхронизируем панель с уже отмеченными кнопками
        refreshOfferPanel();
    }

    // Главный метод инициализации
    static init() {
        WaitingOrders.initDeadlineModal();
        WaitingOrders.initOfferButtons();
    }
}