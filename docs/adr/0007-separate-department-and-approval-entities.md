# ADR-0007: Department and Approval are separate entities
Status: Accepted
Date: 2026-07-25

## Context
The original schema modeled `department` as a plain string on `Employee`, and modeled the peer/manager decisions as inline columns on `ShiftSwapRequest` (`peerDecisionNote`, `peerDecidedAt`, `managerId`, `managerDecisionNote`, `managerDecidedAt`). When adopting a layered `com.company.hr` package structure with an explicit `repository` package (including `DepartmentRepository` and `ApprovalRepository`), it became clear both concepts deserved to be first-class entities rather than fields:
- A string `department` can't be queried, renamed consistently, or constrained to a known set without an accompanying lookup table.
- Inline decision columns only support exactly one decision per stage per request, hard-code the assumption that a request has exactly two decision points, and make "who decided what, when" awkward to query generically (e.g., for an audit view listing all decisions across requests).

## Decision
Introduce `Department` as its own entity/table (`Employee.department` becomes `Employee.departmentId`, a nullable FK). Introduce `Approval` as its own entity/table: one row per decision, keyed by `(shift_swap_request_id, stage)` with `stage ∈ {PEER, MANAGER}` and a unified `decision ∈ {APPROVED, REJECTED}`. `ShiftSwapRequest` itself no longer stores any decision data or a snapshotted manager — the manager authorized to decide the MANAGER stage is resolved dynamically from `requester.manager` at decision time.

## Consequences
- `ShiftSwapRequestRepository.findAllForManager` joins through `requester.manager.id` instead of a stored `manager_id`, so an org-hierarchy change is reflected immediately rather than only for requests created after the change.
- Querying "all decisions this manager has ever made" or "the full decision history of this request" is a plain `ApprovalRepository` query instead of ad-hoc column reads.
- One extra join is needed to fetch a request's decisions compared to inline columns, and one extra table/repository pair to maintain.
- `Department` becomes the natural place to later add department-level settings (e.g., a department-specific swap-approval policy) without touching `Employee`.
