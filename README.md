# Movie Ticket Booking System

A production-ready backend REST API for movie ticket booking with concurrency-safe seat reservation.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database Access | Spring JDBC (JdbcTemplate) — no ORM |
| Database | MySQL 8+ |
| Auth | JWT (jjwt 0.11.5) + Spring Security |
| Build | Maven |

---

## Project Structure

```
src/main/java/com/moviebooking/
├── MovieTicketBookingApplication.java   # Entry point
├── config/
│   ├── SecurityConfig.java              # Spring Security + JWT filter chain
│   ├── JwtAuthFilter.java               # Extracts JWT from Authorization header
│   └── GlobalExceptionHandler.java      # Centralized error responses
├── controller/
│   ├── AuthController.java              # POST /api/auth/register, /login
│   ├── MovieController.java             # GET /api/movies, /shows/{movieId}
│   ├── SeatController.java              # GET /api/seats/{showId}
│   └── BookingController.java           # POST /api/book, GET /api/booking/{id}
├── service/
│   ├── AuthService.java                 # Register/login business logic
│   ├── MovieService.java                # Movie & show queries
│   ├── SeatService.java                 # Seat availability queries
│   └── BookingService.java              # Booking + concurrency control
├── dao/
│   ├── UserDao.java                     # JDBC queries for users table
│   ├── MovieDao.java                    # JDBC queries for movies table
│   ├── ShowDao.java                     # JDBC queries for shows table
│   ├── SeatDao.java                     # JDBC queries + FOR UPDATE locking
│   └── BookingDao.java                  # JDBC queries for bookings table
├── model/
│   ├── User.java, Movie.java, Show.java
│   ├── Seat.java, Booking.java
├── dto/
│   ├── RegisterRequest.java, LoginRequest.java
│   ├── BookingRequest.java, ApiResponse.java
└── util/
    └── JwtUtil.java                     # Token generation & validation

src/main/resources/
├── application.properties               # DB config, JWT secret, port
├── schema.sql                           # Auto-runs on startup: CREATE TABLE statements
└── data.sql                             # Seed data: movies, shows, seats
```

---

## Database Schema

```
users          → id, name, email, password
movies         → id, title, duration, genre, description
screens        → id, name, total_seats
shows          → id, movie_id, screen_id, show_time, price
seats          → id, show_id, seat_number, status (AVAILABLE/LOCKED/BOOKED), locked_at
bookings       → id, user_id, show_id, total_amount, status
booking_seats  → booking_id, seat_id  (junction table)
```

---

## How Concurrency Control Works

This is the most critical part of the system. Two users trying to book the same seat simultaneously is handled via **Pessimistic Locking**:

```
User A                          User B
  |                               |
  | BEGIN TRANSACTION             |
  |                               | BEGIN TRANSACTION
  | SELECT * FROM seats           |
  | WHERE id IN (1,2)             |
  | AND status='AVAILABLE'        |
  | FOR UPDATE  ← acquires lock  |
  |                               | SELECT * FROM seats
  |                               | WHERE id IN (1,2)
  |                               | FOR UPDATE  ← WAITS (blocked by User A)
  | UPDATE seats SET status=BOOKED|
  | INSERT INTO bookings ...      |
  | COMMIT  → lock released       |
  |                               | ← unblocked, but seats no longer AVAILABLE
  |                               | → throws "Seats no longer available" error
  |                               | ROLLBACK
```

**Transaction isolation**: `SERIALIZABLE` — strongest guarantee, prevents phantom reads.

**Code location**: [BookingService.java](src/main/java/com/moviebooking/service/BookingService.java) → `bookSeats()` method, and [SeatDao.java](src/main/java/com/moviebooking/dao/SeatDao.java) → `lockSeatsForUpdate()` method.

---

## Setup & Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8+

### 1. Configure Database

```properties
# src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/movie_booking_db?...
spring.datasource.username=root
spring.datasource.password=root     # ← change to your MySQL password
```

### 2. Create MySQL Database

```sql
CREATE DATABASE movie_booking_db;
```

The schema and seed data auto-run on startup via `schema.sql` and `data.sql`.

### 3. Build & Run

```bash
mvn clean package
java -jar target/movie-ticket-booking-1.0.0.jar
```

Or run directly:

```bash
mvn spring-boot:run
```

Server starts at: `http://localhost:8080`

---

## API Reference

All protected endpoints require header: `Authorization: Bearer <token>`

### Auth

| Method | URL | Body | Auth Required |
|--------|-----|------|--------------|
| POST | `/api/auth/register` | `{name, email, password}` | No |
| POST | `/api/auth/login` | `{email, password}` | No |

### Movies

| Method | URL | Auth Required |
|--------|-----|--------------|
| GET | `/api/movies` | Yes |
| GET | `/api/shows/{movieId}` | Yes |

### Seats

| Method | URL | Auth Required |
|--------|-----|--------------|
| GET | `/api/seats/{showId}` | Yes |

### Booking

| Method | URL | Body | Auth Required |
|--------|-----|------|--------------|
| POST | `/api/book` | `{showId, seatIds: [1,2]}` | Yes |
| GET | `/api/booking/{id}` | — | Yes |
| GET | `/api/bookings/my` | — | Yes |

---

## Example API Flow (Postman)

### Step 1 — Register
```json
POST /api/auth/register
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secret123"
}
```
Response includes `token` → copy it.

### Step 2 — Browse movies
```
GET /api/movies
Authorization: Bearer <token>
```

### Step 3 — Check shows for a movie
```
GET /api/shows/1
Authorization: Bearer <token>
```

### Step 4 — Check available seats
```
GET /api/seats/1
Authorization: Bearer <token>
```
Note seat IDs with `status: "AVAILABLE"`.

### Step 5 — Book seats
```json
POST /api/book
Authorization: Bearer <token>
{
  "showId": 1,
  "seatIds": [1, 2]
}
```
Response includes `bookingId`.

### Step 6 — Get booking details
```
GET /api/booking/1
Authorization: Bearer <token>
```

---

## Response Format

All endpoints return a consistent envelope:

```json
{
  "success": true,
  "message": "Booking confirmed",
  "data": { ... }
}
```

On error:
```json
{
  "success": false,
  "message": "Seats are no longer available: [3]. Please select different seats.",
  "data": null
}
```
