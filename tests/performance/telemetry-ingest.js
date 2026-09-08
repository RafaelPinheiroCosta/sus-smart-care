import http from 'k6/http';
import { check, sleep } from 'k6';
export const options = { vus: 20, duration: '30s', thresholds: { http_req_duration: ['p(95)<750'], http_req_failed: ['rate<0.02'] } };
const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN || '';
const SESSION = __ENV.SESSION_ID;
const DEVICE = __ENV.DEVICE_ID;
export default function () {
  if (!SESSION || !DEVICE || !TOKEN) throw new Error('TOKEN, SESSION_ID e DEVICE_ID são obrigatórios');
  const body = JSON.stringify({ deviceId: DEVICE, type: 'HEART_RATE', value: 70 + Math.random()*50, unit: 'bpm', measuredAt: new Date().toISOString() });
  const res = http.post(`${BASE}/api/v1/telemetry/sessions/${SESSION}/observations`, body, { headers: { 'Content-Type':'application/json', Authorization:`Bearer ${TOKEN}` } });
  check(res, { 'telemetry accepted': r => r.status === 202 });
  sleep(0.1);
}
