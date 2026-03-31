-- Seed data (only insert if tables are empty)
INSERT IGNORE INTO screens (id, name, total_seats) VALUES
(1, 'Screen 1', 50),
(2, 'Screen 2', 80),
(3, 'Screen 3', 100);

INSERT IGNORE INTO movies (id, title, duration, genre, description) VALUES
(1, 'Inception', 148, 'Sci-Fi', 'A thief who steals corporate secrets through dream-sharing technology.'),
(2, 'Interstellar', 169, 'Sci-Fi', 'A team of explorers travel through a wormhole in space.'),
(3, 'The Dark Knight', 152, 'Action', 'Batman faces the Joker in a battle for Gotham City.');

INSERT IGNORE INTO shows (id, movie_id, screen_id, show_time, price) VALUES
(1, 1, 1, '2026-04-01 10:00:00', 150.00),
(2, 1, 2, '2026-04-01 14:00:00', 180.00),
(3, 2, 1, '2026-04-01 17:00:00', 150.00),
(4, 3, 3, '2026-04-01 20:00:00', 200.00);

-- Seats for show 1 (Screen 1 - 20 seats A1-A10, B1-B10)
INSERT IGNORE INTO seats (show_id, seat_number, status) VALUES
(1, 'A1', 'AVAILABLE'), (1, 'A2', 'AVAILABLE'), (1, 'A3', 'AVAILABLE'),
(1, 'A4', 'AVAILABLE'), (1, 'A5', 'AVAILABLE'), (1, 'A6', 'AVAILABLE'),
(1, 'A7', 'AVAILABLE'), (1, 'A8', 'AVAILABLE'), (1, 'A9', 'AVAILABLE'),
(1, 'A10', 'AVAILABLE'), (1, 'B1', 'AVAILABLE'), (1, 'B2', 'AVAILABLE'),
(1, 'B3', 'AVAILABLE'), (1, 'B4', 'AVAILABLE'), (1, 'B5', 'AVAILABLE'),
(1, 'B6', 'AVAILABLE'), (1, 'B7', 'AVAILABLE'), (1, 'B8', 'AVAILABLE'),
(1, 'B9', 'AVAILABLE'), (1, 'B10', 'AVAILABLE');

-- Seats for show 2
INSERT IGNORE INTO seats (show_id, seat_number, status) VALUES
(2, 'A1', 'AVAILABLE'), (2, 'A2', 'AVAILABLE'), (2, 'A3', 'AVAILABLE'),
(2, 'A4', 'AVAILABLE'), (2, 'A5', 'AVAILABLE'), (2, 'A6', 'AVAILABLE'),
(2, 'A7', 'AVAILABLE'), (2, 'A8', 'AVAILABLE'), (2, 'A9', 'AVAILABLE'),
(2, 'A10', 'AVAILABLE'), (2, 'B1', 'AVAILABLE'), (2, 'B2', 'AVAILABLE'),
(2, 'B3', 'AVAILABLE'), (2, 'B4', 'AVAILABLE'), (2, 'B5', 'AVAILABLE'),
(2, 'B6', 'AVAILABLE'), (2, 'B7', 'AVAILABLE'), (2, 'B8', 'AVAILABLE'),
(2, 'B9', 'AVAILABLE'), (2, 'B10', 'AVAILABLE');

-- Seats for show 3
INSERT IGNORE INTO seats (show_id, seat_number, status) VALUES
(3, 'A1', 'AVAILABLE'), (3, 'A2', 'AVAILABLE'), (3, 'A3', 'AVAILABLE'),
(3, 'A4', 'AVAILABLE'), (3, 'A5', 'AVAILABLE'), (3, 'A6', 'AVAILABLE'),
(3, 'A7', 'AVAILABLE'), (3, 'A8', 'AVAILABLE'), (3, 'A9', 'AVAILABLE'),
(3, 'A10', 'AVAILABLE'), (3, 'B1', 'AVAILABLE'), (3, 'B2', 'AVAILABLE'),
(3, 'B3', 'AVAILABLE'), (3, 'B4', 'AVAILABLE'), (3, 'B5', 'AVAILABLE');

-- Seats for show 4
INSERT IGNORE INTO seats (show_id, seat_number, status) VALUES
(4, 'A1', 'AVAILABLE'), (4, 'A2', 'AVAILABLE'), (4, 'A3', 'AVAILABLE'),
(4, 'A4', 'AVAILABLE'), (4, 'A5', 'AVAILABLE'), (4, 'A6', 'AVAILABLE'),
(4, 'A7', 'AVAILABLE'), (4, 'A8', 'AVAILABLE'), (4, 'A9', 'AVAILABLE'),
(4, 'A10', 'AVAILABLE'), (4, 'B1', 'AVAILABLE'), (4, 'B2', 'AVAILABLE'),
(4, 'B3', 'AVAILABLE'), (4, 'B4', 'AVAILABLE'), (4, 'B5', 'AVAILABLE'),
(4, 'B6', 'AVAILABLE'), (4, 'B7', 'AVAILABLE'), (4, 'B8', 'AVAILABLE'),
(4, 'B9', 'AVAILABLE'), (4, 'B10', 'AVAILABLE');
