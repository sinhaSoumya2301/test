# ADR-0006: This service owns its own Employee/Auth model
Status: Accepted
Date: 2026-07-25

## Context
"HR shift swap" implies an HR ecosystem, which often means an existing HRIS/employee directory to integrate with. This repository, however, starts empty with no such system to integrate against.

## Decision
Build this as a standalone service that owns its own `Employee` entity, authentication, and role model (`EMPLOYEE`/`MANAGER`/`ADMIN`) rather than assuming or stubbing an external HR system.

## Consequences
- No integration risk or dependency on an external system that doesn't exist yet in this context.
- The service is immediately runnable and demoable end-to-end (`docker compose up`) without mocking a third-party directory.
- If this later needs to integrate with a real HRIS, the `Employee` entity and auth layer are the identified seam to replace/federate (e.g., swap local password auth for SSO against the real HRIS, keep `Shift`/`ShiftSwapRequest` as-is) — not a full redesign.
