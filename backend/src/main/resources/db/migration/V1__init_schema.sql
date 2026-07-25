-- Initial schema for the HR Shift Swap Service. See /SPEC.md "Domain Model / ERD".
-- gen_random_uuid() is built into PostgreSQL core since v13 — no extension required.

CREATE TABLE department (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE employee (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code  VARCHAR(50)  NOT NULL UNIQUE,
    full_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYEE', 'MANAGER', 'ADMIN')),
    department_id  UUID REFERENCES department (id),
    manager_id     UUID REFERENCES employee (id),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_employee_manager_id ON employee (manager_id);
CREATE INDEX idx_employee_department_id ON employee (department_id);

CREATE TABLE shift (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID        NOT NULL REFERENCES employee (id),
    shift_date  DATE        NOT NULL,
    start_time  TIME        NOT NULL,
    end_time    TIME        NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
                    CHECK (status IN ('SCHEDULED', 'SWAPPED', 'CANCELLED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (employee_id, shift_date, start_time)
);
CREATE INDEX idx_shift_employee_date ON shift (employee_id, shift_date);

-- No peer/manager decision columns here by design — each decision is a separate row in
-- `approval` below. "Who may decide the MANAGER stage" is resolved dynamically from
-- employee.manager_id at decision time, not snapshotted on this table.
CREATE TABLE shift_swap_request (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id        UUID         NOT NULL REFERENCES employee (id),
    requester_shift_id  UUID         NOT NULL REFERENCES shift (id),
    target_employee_id  UUID         NOT NULL REFERENCES employee (id),
    target_shift_id     UUID REFERENCES shift (id),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING_PEER_APPROVAL'
                            CHECK (status IN (
                                'PENDING_PEER_APPROVAL', 'PENDING_MANAGER_APPROVAL',
                                'REJECTED_BY_PEER', 'REJECTED_BY_MANAGER',
                                'APPROVED', 'CANCELLED', 'EXPIRED'
                            )),
    reason              VARCHAR(1000) NOT NULL,
    expires_at          TIMESTAMPTZ  NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version             BIGINT       NOT NULL DEFAULT 0,
    CHECK (requester_id <> target_employee_id)
);
CREATE INDEX idx_swap_status ON shift_swap_request (status);
CREATE INDEX idx_swap_requester ON shift_swap_request (requester_id);
CREATE INDEX idx_swap_target ON shift_swap_request (target_employee_id);

-- One row per decision in the two-stage workflow (docs/adr/0002). approver_id is the target
-- employee for a PEER-stage row, the requester's manager for a MANAGER-stage row.
CREATE TABLE approval (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_swap_request_id UUID        NOT NULL REFERENCES shift_swap_request (id),
    stage                VARCHAR(20)  NOT NULL CHECK (stage IN ('PEER', 'MANAGER')),
    approver_id          UUID         NOT NULL REFERENCES employee (id),
    decision             VARCHAR(20)  NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    note                 VARCHAR(1000),
    decided_at           TIMESTAMPTZ  NOT NULL,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (shift_swap_request_id, stage)
);
CREATE INDEX idx_approval_request ON approval (shift_swap_request_id);

CREATE TABLE notification (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id            UUID         NOT NULL REFERENCES employee (id),
    type                    VARCHAR(40)  NOT NULL,
    related_swap_request_id UUID REFERENCES shift_swap_request (id),
    message                 VARCHAR(1000) NOT NULL,
    is_read                 BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notification_recipient_unread ON notification (recipient_id, is_read);

CREATE TABLE refresh_token (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id  UUID        NOT NULL REFERENCES employee (id),
    token_hash   VARCHAR(255) NOT NULL UNIQUE,
    expires_at   TIMESTAMPTZ NOT NULL,
    revoked      BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_token_employee ON refresh_token (employee_id);

CREATE TABLE audit_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type  VARCHAR(50) NOT NULL,
    entity_id    UUID,
    action       VARCHAR(50) NOT NULL,
    performed_by UUID REFERENCES employee (id),
    old_value    JSONB,
    new_value    JSONB,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);

-- Backs the Idempotency-Key header contract in /SPEC.md "API Contract".
CREATE TABLE idempotency_record (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(100) NOT NULL,
    employee_id     UUID         NOT NULL REFERENCES employee (id),
    endpoint        VARCHAR(100) NOT NULL,
    response_status INT          NOT NULL,
    response_body   TEXT         NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (idempotency_key, endpoint, employee_id)
);
CREATE INDEX idx_idempotency_created_at ON idempotency_record (created_at);
