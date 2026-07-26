# HR Shift Swap Service

Employees can request to swap a shift with a colleague; the colleague accepts, then the
requester's manager approves. React (TypeScript) frontend, Java 21 / Spring Boot backend,
PostgreSQL, Docker for local dev, GitHub Actions → Render + Neon + Vercel.

**Full spec, architecture, API contract, ADRs, security/threat model, and rollback strategy:
see [`SPEC.md`](./SPEC.md).**

## Quick start (local)

```bash
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend: http://localhost:8080 (Swagger UI at `/swagger-ui.html`, health at `/actuator/health`)
- Three ready-to-use accounts (seeded by `backend/src/main/resources/db/seed/V1001__seed_test_data.sql`),
  all with password `password123`:
  - `alice.employee@example.com` — employee
  - `bob.colleague@example.com` — employee (Alice's colleague, same manager)
  - `carol.manager@example.com` — manager

## Running tests

```bash
# Backend — unit tests (no Docker needed)
cd backend && mvn test -Dtest='!ShiftSwapFlowIntegrationTest,!PactProviderVerificationTest'

# Backend — full suite, incl. Testcontainers integration test (needs Docker)
cd backend && mvn verify

# Frontend — unit tests, lint, typecheck, build
cd frontend && npm test && npm run lint && npx tsc -b && npm run build

# Frontend — e2e (needs the docker-compose stack running, freshly seeded)
cd frontend && npm run e2e

# Contract tests — generate the consumer pact, then verify it on the provider (needs Docker)
cd frontend && npm run test:pact
cd backend && mvn test -Dtest=PactProviderVerificationTest
```

## Repository layout

See `SPEC.md` → "Repository Structure" for the annotated full tree.

```
backend/           Spring Boot API (com.company.hr)
frontend/           React + Vite + TypeScript SPA
contract-tests/     Pact consumer/provider contract artifacts
load-test/           k6 scripts (manual only — see load-test.yml)
docs/adr/            Architecture Decision Records
.github/workflows/   CI, CD, rollback, release-please, load-test
```
