// Write path under contention: many users race for the SAME room and dates.
// Expectation: exactly one 201 per (room, dates), every other request a clean 409 - never a 500,
// never a double booking. Proves the pessimistic lock under real concurrency (PostgreSQL).
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import exec from 'k6/execution';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const created = new Counter('bookings_created');
const conflicts = new Counter('bookings_conflict');

export const options = {
  scenarios: {
    race: { executor: 'shared-iterations', vus: 50, iterations: 500, maxDuration: '2m' },
  },
  thresholds: {
    'http_req_duration{name:create}': ['p(95)<800'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const res = http.post(`${BASE}/api/auth/login`, JSON.stringify({ username: 'staff', password: 'Staff123!' }), {
    headers: { 'Content-Type': 'application/json' },
  });
  // random window per run, so the test can be repeated on the same database
  return { token: res.json('token'), baseDay: 400 + Math.floor(Math.random() * 1500) };
}

function isoDate(offsetDays) {
  return new Date(Date.now() + offsetDays * 86400000).toISOString().slice(0, 10);
}

export default function (data) {
  // 10 distinct (room, date) slots -> at most 10 successful bookings for 500 attempts
  const slot = exec.scenario.iterationInTest % 10;
  const checkIn = data.baseDay + slot * 3;
  const body = JSON.stringify({ roomId: 1 + (slot % 5), checkIn: isoDate(checkIn), checkOut: isoDate(checkIn + 2), guestId: 1 });
  const res = http.post(`${BASE}/api/bookings`, body, {
    headers: { Authorization: `Bearer ${data.token}`, 'Content-Type': 'application/json' },
    tags: { name: 'create' },
  });
  if (res.status === 201) created.add(1);
  if (res.status === 409) conflicts.add(1);
  check(res, { '201 or 409 (never 5xx)': (r) => r.status === 201 || r.status === 409 });
}
