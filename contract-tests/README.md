# Contract Tests

See `SPEC.md` → "Testing Strategy" > Contract Testing.

Consumer-driven contract tests (Pact) between the React frontend (consumer) and Spring Boot
backend (provider). The actual test code lives with each side's own toolchain/dependencies —
this directory just holds the shared contract artifact both sides agree on.

- **Consumer test**: `frontend/src/api/__contract__/*.pact.test.ts` — run via `npm run test:pact`
  (frontend). Exercises the real API-client code against a local Pact mock server and writes the
  resulting contract to `contract-tests/pacts/*.json`.
- **Provider verification**: `backend/src/test/java/com/company/hr/contract/PactProviderVerificationTest.java` —
  run via `mvn test -Dtest=PactProviderVerificationTest` (backend). Reads `contract-tests/pacts/*.json`
  and replays every interaction against the real controllers (Testcontainers Postgres, same as
  the integration test), seeding whatever `@State(...)` the interaction declares it needs.

`.github/workflows/ci.yml`'s `contract-tests` job runs both in the correct order: generate the
pact (frontend) first, then verify it (backend) — this cannot run in reverse, since the backend
verification step needs the pact file to already exist.

`pacts/` is generated output — safe to delete and regenerate, not meant to be hand-edited.
