// Read path: availability search (booking-service -> pricing-service -> fx-service).
//   make perf        (runs this in Docker against the compose stack)
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
  scenarios: {
    search: {
      executor: 'ramping-vus',
      stages: [
        { duration: '20s', target: 20 },
        { duration: '40s', target: 50 },
        { duration: '20s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({ username: 'guest1', password: 'Guest123!' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  return { token: res.json('token') };
}

function isoDate(offsetDays) {
  const d = new Date(Date.now() + offsetDays * 86400000);
  return d.toISOString().slice(0, 10);
}

export default function (data) {
  const start = 10 + Math.floor(Math.random() * 300);
  const nights = 1 + Math.floor(Math.random() * 5);
  const res = http.get(
    `${BASE}/api/rooms/availability?checkin=${isoDate(start)}&checkout=${isoDate(start + nights)}`,
    { headers: { Authorization: `Bearer ${data.token}`, 'X-Session-Id': `k6-vu-${__VU}-session` }, tags: { name: 'availability' } },
  );
  check(res, { '200': (r) => r.status === 200, 'has rooms': (r) => Array.isArray(r.json('rooms')) });
  sleep(0.2);
}
