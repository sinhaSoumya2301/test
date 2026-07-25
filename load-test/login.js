import http from "k6/http";
import { check, sleep } from "k6";

// See SPEC.md "Testing Strategy" > Load Testing. Manual only (load-test.yml is
// workflow_dispatch-only) — run against the local docker-compose stack or a staging deploy,
// never automatically against the live free-tier deployment.
//
// Usage: k6 run load-test/login.js
//   BASE_URL   defaults to http://localhost:8080
export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m", target: 20 },
    { duration: "15s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<500"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

// Seeded by backend/src/main/resources/db/seed/V1001__seed_test_data.sql.
const CREDENTIALS = [
  { email: "alice.employee@example.com", password: "password123" },
  { email: "bob.colleague@example.com", password: "password123" },
  { email: "carol.manager@example.com", password: "password123" },
];

export default function () {
  const creds = CREDENTIALS[Math.floor(Math.random() * CREDENTIALS.length)];
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(creds), {
    headers: { "Content-Type": "application/json" },
  });

  check(res, {
    "status is 200": (r) => r.status === 200,
    "has accessToken": (r) => !!r.json("accessToken"),
  });

  sleep(1);
}
