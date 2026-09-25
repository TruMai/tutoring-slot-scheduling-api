INSERT INTO users(id, username, password, role)
VALUES (1,'sjsustudentA', 'pass123', 'CUSTOMER')
ON CONFLICT(id) DO NOTHING;

INSERT INTO users(id, username, password, role)
VALUES (2, 'sjsuTutorA', 'pass123', 'PROVIDER')
ON CONFLICT(id) DO NOTHING;

INSERT INTO providers(id, user_id, display_name)
VALUES(2, 2, 'D Teach')
ON CONFLICT(user_id) DO NOTHING; -- Unique

INSERT INTO services(id, name, price, duration_min)
VALUES(1, 'sessionA', 20.00, '30')
ON CONFLICT(id) DO NOTHING;


INSERT INTO availability_slots(provider_id, service_id, start_at, end_at, version)
VALUES (2, 1, '09-20-2026', '09-24-2026', 0)
ON CONFLICT(id) DO NOTHING;