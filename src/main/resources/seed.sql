INSERT INTO users(id, username, password_hash, role)
VALUES (1,'sjsustudentA', '$2a$10$4DRBPlbjKO7WuL2ndpbisOheLfgVwDlngY7t18/ZZBFNcW3HdWFGm', 'CUSTOMER')
ON CONFLICT(id) DO NOTHING;
-- Encrypted password for plaintext: pass123
INSERT INTO users(id, username, password_hash, role)
VALUES (2, 'sjsuTutorA', '$2a$10$4DRBPlbjKO7WuL2ndpbisOheLfgVwDlngY7t18/ZZBFNcW3HdWFGm', 'PROVIDER')
ON CONFLICT(id) DO NOTHING;

INSERT INTO providers(id, user_id, display_name)
VALUES(2, 2, 'D Teach')
ON CONFLICT(user_id) DO NOTHING; -- Unique

INSERT INTO services(id, name, price, duration_min)
VALUES(1, 'sessionA', 20.00, '30')
ON CONFLICT(id) DO NOTHING;

--slot to test booking,  id = 1
INSERT INTO availability_slots(id, provider_id, service_id, start_at, end_at, version)
VALUES (1, 2, 1, '09-20-2026', '09-24-2026', 0)
ON CONFLICT(id) DO NOTHING;
