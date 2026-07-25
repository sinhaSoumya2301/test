import { defineConfig } from "vitest/config";
import path from "node:path";

// Separate from vite.config.ts's `test` block deliberately — pact tests spin up a real local
// mock HTTP server per test and write pact files to disk, so they're run explicitly via
// `npm run test:pact` (see .github/workflows/ci.yml's contract-tests job), not on every
// `npm test`. See src/api/__contract__/auth.pact.test.ts.
export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  test: {
    environment: "node",
    include: ["src/api/__contract__/**/*.pact.test.ts"],
  },
});
