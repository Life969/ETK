import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

// ---- Свои метрики: замеряем время отдачи каждой страницы отдельно ----
const homeMs      = new Trend('page_home_ms', true);
const recordsMs   = new Trend('page_records_ms', true);
const waitingMs   = new Trend('page_waiting_ms', true);
const prodMs      = new Trend('page_production_ms', true);
const completedMs = new Trend('page_completed_ms', true);

// ---- Конфигурация теста ----
export const options = {
    vus: 20,               // 20 одновременных виртуальных пользователей
    duration: '3m',        // гоняем 3 минуты

    // Пороги: если p(95) больше — k6 подсветит красным
    thresholds: {
        'page_home_ms':       ['p(95)<1500'],
        'page_records_ms':    ['p(95)<2000'],
        'page_waiting_ms':    ['p(95)<2000'],
        'page_production_ms': ['p(95)<2000'],
        'page_completed_ms':  ['p(95)<2500'],
        'http_req_failed':    ['rate<0.01'],
    },
};

const BASE     = __ENV.BASE_URL || 'http://localhost:8080';
const LOGIN    = __ENV.LOGIN    || 'admin';
const PASSWORD = __ENV.PASSWORD || 'admin123';

// ---- setup: один раз логинимся и раздаём cookie всем VU ----
export function setup() {
    // ---- 1. Проверка сети: GET /login должен вернуть 200 ----
    const ping = http.get(`${BASE}/login`);
    if (ping.status !== 200) {
        throw new Error(
            `GET /login failed. status=${ping.status}. ` +
            `body=${(ping.body || '').slice(0, 300)}`
        );
    }

    // ---- 2. POST логина. Делаем РОВНО как curl ----
    const body = `username=${encodeURIComponent(LOGIN)}&password=${encodeURIComponent(PASSWORD)}`;
    const res = http.post(`${BASE}/login`, body, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        redirects: 0,
    });

    // ---- 3. Достаём JSESSIONID ----
    const c = res.cookies['JSESSIONID'];
    if (!c || !c[0]) {
        throw new Error(
            `POST /login failed. status=${res.status}. ` +
            `body=${(res.body || '').slice(0, 300)}`
        );
    }
    return { jsessionid: c[0].value };
}

// ---- Основной цикл каждого виртуального пользователя ----
export default function (data) {
    // Устанавливаем cookie сессии (один раз на VU)
    http.cookieJar().set(BASE, 'JSESSIONID', data.jsessionid);

    // Выбираем страницу взвешенно: журнал и completed — тяжелее
    const r = Math.random();
    if      (r < 0.20) getHome();
    else if (r < 0.40) getRecords();
    else if (r < 0.60) getWaiting();
    else if (r < 0.80) getProduction();
    else               getCompleted();

    // «Думаем» как человек: 1-3 секунды между запросами
    sleep(1 + Math.random() * 2);
}

// ============================================================
// Отдельные шаги
// ============================================================
function getHome() {
    const res = http.get(`${BASE}/home`);
    homeMs.add(res.timings.duration);
    check(res, { 'home 200': (r) => r.status === 200 });
}

function getRecords() {
    // всего ~500 страниц (10000 / 20), берём случайную
    const page = rand(0, 499);
    const res = http.get(`${BASE}/production-records?page=${page}&size=20`);
    recordsMs.add(res.timings.duration);
    check(res, { 'records 200': (r) => r.status === 200 });
}

function getWaiting() {
    // ~25 страниц (300 / 12)
    const page = rand(0, 24);
    const res = http.get(`${BASE}/orders/waiting?page=${page}&size=12`);
    waitingMs.add(res.timings.duration);
    check(res, { 'waiting 200': (r) => r.status === 200 });
}

function getProduction() {
    const page = rand(0, 24);
    const res = http.get(`${BASE}/orders/production?page=${page}&size=12`);
    prodMs.add(res.timings.duration);
    check(res, { 'production 200': (r) => r.status === 200 });
}

function getCompleted() {
    // ~833 страницы (10000 / 12)
    const page = rand(0, 832);
    const res = http.get(`${BASE}/orders/completed?page=${page}&size=12`);
    completedMs.add(res.timings.duration);
    check(res, { 'completed 200': (r) => r.status === 200 });
}

function rand(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}