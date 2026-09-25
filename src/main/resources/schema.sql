
-- 1. Users table (Customers and Providers)
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE, -- no users share username
    password TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('CUSTOMER', 'PROVIDER'))
);

-- 2. Providers table (Tutors)
CREATE TABLE IF NOT EXISTS providers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE, -- 1:1 relationship
    display_name TEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. Services table (Tutoring sessions offered)
CREATE TABLE IF NOT EXISTS services (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    price REAL NOT NULL,
    duration_min INTEGER NOT NULL
);

-- 4. Availability slots created by tutors
CREATE TABLE IF NOT EXISTS availability_slots (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    provider_id INTEGER NOT NULL,
    service_id INTEGER NOT NULL,
    start_at TEXT NOT NULL,
    end_at TEXT NOT NULL,
    version INTEGER DEFAULT 0,
    FOREIGN KEY (provider_id) REFERENCES providers(id),
    FOREIGN KEY (service_id) REFERENCES services(id)
);

-- 5. Appointments linking a customer to a slot
CREATE TABLE IF NOT EXISTS appointments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    slot_id INTEGER NOT NULL, -- Unique constraint guards against double-booking
    status TEXT NOT NULL CHECK(status IN ('BOOKED', 'CANCELLED')),
    FOREIGN KEY (customer_id) REFERENCES users(id),
    FOREIGN KEY (slot_id) REFERENCES availability_slots(id)
);

-- Double-booking guard: Ensures a slot can only have one active 'BOOKED' status at a time
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_booking 
ON appointments (slot_id) 
WHERE status = 'BOOKED';