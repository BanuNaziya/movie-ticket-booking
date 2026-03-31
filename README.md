# Movie Ticket Booking System — Backend API

A production-ready REST API backend for movie ticket booking with **concurrency-safe seat reservation** using pessimistic locking.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database Access | Spring JDBC (JdbcTemplate) — no ORM |
| Database | MySQL 8 |
| Authentication | JWT + Spring Security |
| Build Tool | Maven |

---

## Features

- JWT-based authentication (register / login)
- Browse movies and show timings
- Real-time seat availability
- **Concurrency-safe booking** — no double booking under concurrent requests
- Automatic DB schema creation and seed data on startup
- Consistent JSON response envelope for all endpoints

---

## Project Structure

```
src/main/java/com/moviebooking/
├── config/
│   ├── SecurityConfig.java          # Spring Security + stateless JWT filter chain
│   ├── JwtAuthFilter.java           # Extracts and validates JWT from Authorization header
│   └── GlobalExceptionHandler.java  # Centralized error handling for all controllers
├── controller/
│   ├── AuthController.java          # POST /api/auth/register, /login, /health
│   ├── MovieController.java         # GET /api/movies, /shows/{movieId}
│   ├── SeatController.java          # GET /api/seats/{showId}
│   └── BookingController.java       # POST /api/book, GET /api/booking/{id}
├── service/
│   ├── AuthService.java             # Register/login with BCrypt password hashing
│   ├── MovieService.java            # Movie and show queries
│   ├── SeatService.java             # Seat availability queries
│   └── BookingService.java          # Booking with pessimistic locking (SERIALIZABLE)
├── dao/
│   ├── UserDao.java                 # JDBC — users table
│   ├── MovieDao.java                # JDBC — movies table
│   ├── ShowDao.java                 # JDBC — shows table (with JOIN)
│   ├── SeatDao.java                 # JDBC — seats table + SELECT FOR UPDATE
│   └── BookingDao.java              # JDBC — bookings + booking_seats tables
├── model/                           # Plain Java model classes
├── dto/                             # Request/Response DTOs with validation
└── util/
    └── JwtUtil.java                 # Token generation and validation (jjwt)
```

---

## Database Schema

```
users          → id, name, email, password
movies         → id, title, duration, genre, description
screens        → id, name, total_seats
shows          → id, movie_id, screen_id, show_time, price
seats          → id, show_id, seat_number, status (AVAILABLE/LOCKED/BOOKED)
bookings       → id, user_id, show_id, total_amount, status
booking_seats  → booking_id, seat_id
```

---

## Concurrency Control — Pessimistic Locking

The critical section of the booking flow uses `SELECT ... FOR UPDATE` inside a `SERIALIZABLE` transaction to prevent double booking:

```java
// BookingService.java
@Transactional(isolation = Isolation.SERIALIZABLE)
public Booking bookSeats(Long userId, BookingRequest request) {
    // Acquires row-level locks — concurrent requests block here
    List<Seat> availableSeats = seatDao.lockSeatsForUpdate(request.getSeatIds());

    if (availableSeats.size() != request.getSeatIds().size()) {
        throw new IllegalStateException("Seats are no longer available");
    }

    seatDao.updateSeatsStatus(request.getSeatIds(), "BOOKED");
    // create booking record...
}
```

```sql
-- SeatDao.java — locks rows until transaction commits
SELECT * FROM seats
WHERE id IN (?, ?) AND status = 'AVAILABLE'
FOR UPDATE;
```

**Result:** Two users booking the same seat simultaneously → one succeeds, the other gets a `409 Conflict` with a clear error message. No data corruption possible.

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+

### 1. Create the database

```sql
CREATE DATABASE movie_booking_db;
```

### 2. Configure credentials

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Run

```bash
mvn spring-boot:run
```

Schema and seed data (3 movies, 4 shows, seats) are created automatically on first startup.

Server: `http://localhost:8080`

---

## API Reference

All endpoints except `/api/auth/**` require: `Authorization: Bearer <token>`

### Authentication
| Method | Endpoint | Body |
|--------|----------|------|
| POST | `/api/auth/register` | `{ name, email, password }` |
| POST | `/api/auth/login` | `{ email, password }` |

### Movies & Shows
| Method | Endpoint |
|--------|----------|
| GET | `/api/movies` |
| GET | `/api/shows/{movieId}` |

### Seats
| Method | Endpoint |
|--------|----------|
| GET | `/api/seats/{showId}` |

### Booking
| Method | Endpoint | Body |
|--------|----------|------|
| POST | `/api/book` | `{ showId, seatIds: [1, 2] }` |
| GET | `/api/booking/{id}` | — |
| GET | `/api/bookings/my` | — |

---

## Response Format

All responses follow a consistent envelope:

```json
{
  "success": true,
  "message": "Booking confirmed",
  "data": {
    "id": 1,
    "movieTitle": "Inception",
    "showTime": "2026-04-01T10:00:00",
    "seatNumbers": ["A1", "A2"],
    "totalAmount": 300.00,
    "status": "CONFIRMED"
  }
}
```
