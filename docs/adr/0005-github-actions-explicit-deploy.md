# ADR-0005: GitHub Actions drives deploys explicitly rather than platform auto-deploy
Status: Accepted
Date: 2026-07-25

## Context
Both Render and Vercel can auto-deploy directly from a connected GitHub repo without any Actions workflow at all. That's less to build, but it means the deploy pipeline's logic (what gets built, what gets scanned, what gates a deploy, how a smoke test runs) lives entirely inside a third-party dashboard rather than in version-controlled, reviewable YAML.

## Decision
Drive both backend and frontend deploys from explicit GitHub Actions workflows (`cd-backend.yml`, `cd-frontend.yml`) that call Render's deploy hook / the Vercel CLI respectively, rather than relying on the platforms' native Git integration.

## Consequences
- The entire deploy pipeline — build, security scans, smoke test, then deploy — is one reviewable, version-controlled artifact in `.github/workflows/`, matching the explicit "deploy through GitHub workflow" requirement.
- Deploys are gated on CI (CodeQL/Trivy/gitleaks/tests) passing, which native platform auto-deploy would not enforce by default.
- Slightly more setup than "just connect the repo": requires `RENDER_DEPLOY_HOOK`/`RENDER_API_KEY` and `VERCEL_TOKEN`/`VERCEL_ORG_ID`/`VERCEL_PROJECT_ID` as GitHub Actions secrets.
- Rollback (ADR-adjacent, see SPEC.md → Rollback Strategy) is also scripted through Actions/API calls for the same reason — consistency and auditability of "what deployed when."
