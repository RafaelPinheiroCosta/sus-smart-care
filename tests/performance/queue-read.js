import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = { stages: [ { duration: '20s', target: 20 }, { duration: '40s', target: 100 }, { duration: '20s', target: 0 } ], thresholds: { http_req_duration: ['p(95)<500'], http_req_failed: ['rate<0.01'] } };
const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const FACILITY = __ENV.FACILITY_ID || '00000000-0000-0000-0000-000000000001';
export default function () {
  const res = http.get(`${BASE}/api/v1/queues/${FACILITY}/public-view`);
  check(res, { 'public queue 200': r => r.status === 200 });
  sleep(0.2);
}
