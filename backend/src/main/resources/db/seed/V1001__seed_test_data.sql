-- Local/e2e-only seed data. Only ever applied when the "local" Spring profile is active
-- (see application-local.yml: spring.flyway.locations adds classpath:db/seed) — never in the
-- default application.yml locations used by Render/production. Version 1001+ is reserved for
-- seed data so it never collides with real schema migrations in db/migration.
--
-- All three accounts share the password: password123

INSERT INTO department (id, name, description)
VALUES ('00000000-0000-0000-0000-000000000001', 'Customer Support', 'Seeded department for local/e2e testing');

INSERT INTO employee (id, employee_code, full_name, email, password_hash, role, department_id, manager_id, active)
VALUES (
    '00000000-0000-0000-0000-000000000010',
    'MGR-0001',
    'Carol Manager',
    'carol.manager@example.com',
    '$2a$10$k8GK.3u/3FY6Z22eNOVMuuJp6lVCVUszhfU5Gp79R5.qIMMSM2ecm',
    'MANAGER',
    '00000000-0000-0000-0000-000000000001',
    NULL,
    TRUE
);

INSERT INTO employee (id, employee_code, full_name, email, password_hash, role, department_id, manager_id, active)
VALUES (
    '00000000-0000-0000-0000-000000000011',
    'EMP-0001',
    'Alice Employee',
    'alice.employee@example.com',
    '$2a$10$haGVKBcu.QHL60bMpGhJge4LN2V5ZVTpP7dQwKoqzPMQUxnN4dZfW',
    'EMPLOYEE',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000010',
    TRUE
);

INSERT INTO employee (id, employee_code, full_name, email, password_hash, role, department_id, manager_id, active)
VALUES (
    '00000000-0000-0000-0000-000000000012',
    'EMP-0002',
    'Bob Colleague',
    'bob.colleague@example.com',
    '$2a$10$CRdiDauzuqx7lkHsdCnns.VdeEZ1xouXlK8UP1eAk1GcSAKKnUEEK',
    'EMPLOYEE',
    '00000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000010',
    TRUE
);

-- Future-dated shifts so FR-2's "not-yet-started shift" validation passes in manual/e2e testing.
INSERT INTO shift (id, employee_id, shift_date, start_time, end_time, status)
VALUES (
    '00000000-0000-0000-0000-000000000021',
    '00000000-0000-0000-0000-000000000011',
    CURRENT_DATE + INTERVAL '5 days',
    '09:00:00',
    '17:00:00',
    'SCHEDULED'
);

INSERT INTO shift (id, employee_id, shift_date, start_time, end_time, status)
VALUES (
    '00000000-0000-0000-0000-000000000022',
    '00000000-0000-0000-0000-000000000012',
    CURRENT_DATE + INTERVAL '6 days',
    '09:00:00',
    '17:00:00',
    'SCHEDULED'
);
