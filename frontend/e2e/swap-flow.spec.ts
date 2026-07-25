import { test, expect, type Page } from "@playwright/test";

/**
 * Full journey (SPEC.md "Testing Strategy" > Frontend e2e): employee creates a request,
 * colleague accepts, manager approves, UI reflects the outcome.
 *
 * PRECONDITION: a freshly-seeded docker-compose stack (`docker compose up --build` against an
 * empty Postgres volume) so the seeded accounts each still have their one SCHEDULED shift
 * available — this test consumes Alice's only seeded shift, so it is not repeatable against the
 * same running stack without re-seeding (`docker compose down -v && docker compose up --build`).
 */

const PASSWORD = "password123";
const ALICE = "alice.employee@example.com";
const BOB = "bob.colleague@example.com";
const CAROL = "carol.manager@example.com";
const REASON = `Family event ${Date.now()}`;

async function login(page: Page, email: string) {
  await page.goto("/login");
  await page.getByLabel(/email/i).fill(email);
  await page.getByLabel(/password/i).fill(PASSWORD);
  await page.getByRole("button", { name: /log in/i }).click();
  await expect(page).toHaveURL("/");
}

async function logout(page: Page) {
  await page.getByRole("button", { name: /log out/i }).click();
  await expect(page).toHaveURL(/\/login$/);
}

test("full swap approval journey: create → peer accept → manager approve", async ({ page }) => {
  await login(page, ALICE);

  await page.locator("#requesterShiftId").selectOption({ index: 1 });
  await page.locator("#targetEmployeeId").selectOption({ label: "Bob Colleague" });
  await page.locator("#reason").fill(REASON);
  await page.getByRole("button", { name: /request swap/i }).click();

  const requestCard = page.locator("div", { hasText: REASON }).first();
  await expect(requestCard).toContainText("PENDING PEER APPROVAL");

  await logout(page);

  await login(page, BOB);
  const bobCard = page.locator("div", { hasText: REASON }).first();
  await bobCard.getByRole("button", { name: /accept/i }).click();
  await expect(bobCard).toContainText("PENDING MANAGER APPROVAL");

  await logout(page);

  await login(page, CAROL);
  const carolCard = page.locator("div", { hasText: REASON }).first();
  await carolCard.getByRole("button", { name: /approve/i }).click();
  await expect(carolCard).toContainText("APPROVED");

  await logout(page);

  // Alice's dashboard now reflects the swap in her own request history.
  await login(page, ALICE);
  const aliceFinalCard = page.locator("div", { hasText: REASON }).first();
  await expect(aliceFinalCard).toContainText("APPROVED");
});
