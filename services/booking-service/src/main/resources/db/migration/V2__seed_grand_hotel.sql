-- Assignment assumption: a single hotel, id 1, "Grand Hotel".
INSERT INTO hotels (id, name) VALUES (1, 'Grand Hotel');

INSERT INTO rooms (hotel_id, room_number, room_type, status) VALUES
    (1, '101', 'STANDARD', 'ACTIVE'),
    (1, '102', 'STANDARD', 'ACTIVE'),
    (1, '103', 'STANDARD', 'ACTIVE'),
    (1, '104', 'STANDARD', 'ACTIVE'),
    (1, '105', 'STANDARD', 'ACTIVE'),
    (1, '106', 'STANDARD', 'MAINTENANCE'),
    (1, '201', 'DELUXE',   'ACTIVE'),
    (1, '202', 'DELUXE',   'ACTIVE'),
    (1, '203', 'DELUXE',   'ACTIVE'),
    (1, '204', 'DELUXE',   'ACTIVE'),
    (1, '301', 'SUITE',    'ACTIVE'),
    (1, '302', 'SUITE',    'ACTIVE');

-- Guest profiles owned by the demo accounts guest1 / guest2 (auth-service).
INSERT INTO guests (first_name, last_name, email, phone) VALUES
    ('Dana', 'Levi',  'dana.levi@example.com',  '+972-50-000-0001'),
    ('John', 'Smith', 'john.smith@example.com', '+1-202-555-0102');
