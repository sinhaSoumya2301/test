# ADR-0002: Swap workflow is peer-accept, then manager-approve
Status: Accepted
Date: 2026-07-25

## Context
Two workflows were considered for how a shift swap gets authorized: (a) the requester picks a colleague and it goes straight to the manager for approval, or (b) the target colleague must first accept, and only then does it go to the manager. Option (a) is simpler to build (one approval gate, fewer states) but risks a manager approving a swap the target colleague never actually agreed to — the manager would be the first person to represent the colleague's consent, which they cannot actually speak to.

## Decision
Require the target colleague to explicitly accept or decline before the request is ever visible to the manager. Only a peer-accepted request reaches `PENDING_MANAGER_APPROVAL`.

## Consequences
- Adds a third status/actor to the state machine (`PENDING_PEER_APPROVAL` → `PENDING_MANAGER_APPROVAL` → terminal), and a second notification/authorization path to build and test.
- The manager's approval now genuinely means "both employees agreed and I'm authorizing it," not "I'm assuming the colleague is fine with it."
- A decline at the peer stage never reaches the manager's queue, keeping manager-facing volume lower and more meaningful.
