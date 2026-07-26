import { defineConfig, devices } from "@playwright/test";

/**
 * Runs against the docker-compose stack (SPEC.md "Testing Strategy" > Frontend e2e), using the
 * accounts seeded by backend/src/main/resources/db/seed/V1001__seed_test_data.sql. Not run as
 * part of `npm test` / CI unit runs — see .github/workflows/ci.yml for how this is wired in.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false, // the seeded scenario shares state (one shift per user) across specs
  retries: process.env.CI ? 1 : 0,
  reporter: [["html", { open: "never" }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:5173",
    trace: "on-first-retry",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
