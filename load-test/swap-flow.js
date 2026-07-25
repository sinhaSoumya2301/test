import http from "k6/http";
import { check, sleep } from "k6";

// See SPEC.md "Testing Strategy" > Load Testing. Manual only — run against a freshly-seeded
// docker-compose stack (backend/src/main/resources/db/seed/V1001__seed_test_data.sql).
//
// NOTE: unlike login.js, this exercises the mutating create/peer-decision/manager-decision
// endpoints against the *same three seeded accounts*, which each only have one seeded shift.
// Deliberately low VU count for that reason — this is a light smoke-level latency check on the
// critical path, not a high-concurrency stress test of the swap-creation flow (an unbounded
// number of concurrent virtual users creating swap requests would need a bulk test-data seeding
// endpoint that doesn't exist — out of scope, see SPEC.md "Load Testing").
//
// Usage: BASE_URL=http://localhost:8080 k6 run load-test/swap-flow.js
export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<1000"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PASSWORD = "password123";

function login(email) {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password: PASSWORD }),
    { headers: { "Content-Type": "application/json" } },
  );
  check(res, { "login succeeded": (r) => r.status === 200 });
  return res.json("accessToken");
}

function authHeaders(token) {
  return { headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` } };
}

export default function () {
  const aliceToken = login("alice.employee@example.com");
  const bobToken = login("bob.colleague@example.com");
  const carolToken = login("carol.manager@example.com");

  const shiftsRes = http.get(
    `${BASE_URL}/api/v1/shifts/me?from=2020-01-01&to=2030-01-01`,
    authHeaders(aliceToken),
  );
  check(shiftsRes, { "fetched alice's shifts": (r) => r.status === 200 });
  const shifts = shiftsRes.json();
  if (!shifts || shifts.length === 0) {
    throw new Error("Alice has no shifts left to swap — re-seed the stack before re-running.");
  }

  const colleaguesRes = http.get(`${BASE_URL}/api/v1/employees/colleagues`, authHeaders(aliceToken));
  const bob = colleaguesRes.json().find((c) => c.email === "bob.colleague@example.com");

  const createRes = http.post(
    `${BASE_URL}/api/v1/shift-swaps`,
    JSON.stringify({
      requesterShiftId: shifts[0].id,
      targetEmployeeId: bob.id,
      targetShiftId: null,
      reason: `k6 load test ${Date.now()}`,
    }),
    authHeaders(aliceToken),
  );
  check(createRes, { "create swap succeeded": (r) => r.status === 201 });
  const requestId = createRes.json("id");

  sleep(1);

  const peerRes = http.post(
    `${BASE_URL}/api/v1/shift-swaps/${requestId}/peer-decision`,
    JSON.stringify({ approve: true }),
    authHeaders(bobToken),
  );
  check(peerRes, { "peer accept succeeded": (r) => r.status === 200 });

  sleep(1);

  const managerRes = http.post(
    `${BASE_URL}/api/v1/shift-swaps/${requestId}/manager-decision`,
    JSON.stringify({ approve: true }),
    authHeaders(carolToken),
  );
  check(managerRes, { "manager approve succeeded": (r) => r.status === 200 });
}
