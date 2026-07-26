import { describe, it, expect } from "vitest";
import { PactV3, MatchersV2 } from "@pact-foundation/pact";
import axios from "axios";
import path from "node:path";
import { fileURLToPath } from "node:url";

// Consumer-driven contract test (SPEC.md "Testing Strategy" > Contract Testing, docs/adr).
// Generates a pact file describing exactly what this frontend expects from POST /auth/login;
// backend/src/test/java/com/company/hr/contract/PactProviderVerificationTest.java replays it
// against the real controller in CI. Run via `npm run test:pact` (separate from `npm test` —
// see vitest.pact.config.ts — since this spins up a real local mock HTTP server per test).

const { like, string } = MatchersV2;

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const provider = new PactV3({
  consumer: "shiftswap-frontend",
  provider: "shiftswap-backend",
  dir: path.resolve(__dirname, "../../../../contract-tests/pacts"),
});

describe("Auth API contract", () => {
  it("POST /api/v1/auth/login returns tokens for valid credentials", async () => {
    provider
      .given("an active employee alice.employee@example.com exists with password password123")
      .uponReceiving("a login request with valid credentials")
      .withRequest({
        method: "POST",
        path: "/api/v1/auth/login",
        headers: { "Content-Type": "application/json" },
        body: { email: "alice.employee@example.com", password: "password123" },
      })
      .willRespondWith({
        status: 200,
        headers: { "Content-Type": "application/json" },
        body: {
          accessToken: string("eyJhbGciOiJIUzI1NiJ9.example.token"),
          refreshToken: string("opaque-refresh-token-example"),
          expiresIn: like(900),
        },
      });

    await provider.executeTest(async (mockServer) => {
      const response = await axios.post(`${mockServer.url}/api/v1/auth/login`, {
        email: "alice.employee@example.com",
        password: "password123",
      });

      expect(response.status).toBe(200);
      expect(response.data.accessToken).toBeTruthy();
      expect(response.data.refreshToken).toBeTruthy();
      expect(typeof response.data.expiresIn).toBe("number");
    });
  });

  it("POST /api/v1/auth/login returns the standard error envelope for bad credentials", async () => {
    provider
      .given("an active employee alice.employee@example.com exists with password password123")
      .uponReceiving("a login request with an incorrect password")
      .withRequest({
        method: "POST",
        path: "/api/v1/auth/login",
        headers: { "Content-Type": "application/json" },
        body: { email: "alice.employee@example.com", password: "wrong-password" },
      })
      .willRespondWith({
        status: 401,
        headers: { "Content-Type": "application/json" },
        body: {
          error: {
            code: string("INVALID_CREDENTIALS"),
            message: string("Email or password is incorrect."),
            traceId: string("a1b2c3d4"),
          },
        },
      });

    await provider.executeTest(async (mockServer) => {
      await expect(
        axios.post(`${mockServer.url}/api/v1/auth/login`, {
          email: "alice.employee@example.com",
          password: "wrong-password",
        }),
      ).rejects.toMatchObject({
        response: { status: 401, data: { error: { code: "INVALID_CREDENTIALS" } } },
      });
    });
  });
});
