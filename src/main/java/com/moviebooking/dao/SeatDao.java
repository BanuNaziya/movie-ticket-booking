package com.moviebooking.dao;

import com.moviebooking.model.Seat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository marks this as a Spring-managed DAO bean.
// It also translates raw SQL exceptions into Spring's DataAccessException hierarchy,
// which makes error handling consistent across different database drivers.
@Repository
public class SeatDao {

    // JdbcTemplate is Spring's wrapper around raw JDBC.
    // WHY use JdbcTemplate instead of plain JDBC?
    // Plain JDBC requires manual connection handling, PreparedStatement creation,
    // result set iteration, and exception handling — all boilerplate.
    // JdbcTemplate handles all of that, letting us focus on the SQL itself.
    private final JdbcTemplate jdbcTemplate;

    public SeatDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // RowMapper defines how a single DB row maps to a Seat object.
    // Defined once as a field so it can be reused across multiple query methods
    // instead of repeating the mapping logic everywhere.
    private final RowMapper<Seat> seatRowMapper = (rs, rowNum) -> {
        Seat seat = new Seat();
        seat.setId(rs.getLong("id"));
        seat.setShowId(rs.getLong("show_id"));
        seat.setSeatNumber(rs.getString("seat_number"));
        seat.setStatus(rs.getString("status"));
        // locked_at is nullable — must check for null before converting to LocalDateTime
        var lockedAt = rs.getTimestamp("locked_at");
        if (lockedAt != null) seat.setLockedAt(lockedAt.toLocalDateTime());
        return seat;
    };

    public List<Seat> findByShowId(Long showId) {
        String sql = "SELECT * FROM seats WHERE show_id = ? ORDER BY seat_number";
        // Using '?' placeholders (PreparedStatement) — WHY?
        // Direct string concatenation risks SQL injection. '?' binds values safely
        // at the driver level, preventing any injected SQL from being executed.
        return jdbcTemplate.query(sql, seatRowMapper, showId);
    }

    /**
     * Acquires pessimistic row-level locks on the requested seats.
     *
     * WHY "FOR UPDATE"?
     * MySQL's FOR UPDATE tells the DB engine to lock the selected rows for the
     * duration of the current transaction. Any other transaction trying to SELECT
     * the same rows FOR UPDATE will WAIT (block) until this transaction commits
     * or rolls back. This is the DB-level mechanism that prevents two users
     * from booking the same seat simultaneously.
     *
     * WHY also filter by status = 'AVAILABLE'?
     * If a seat is already BOOKED, we don't want to lock it (no point) and we
     * want the count mismatch check in BookingService to detect it as unavailable.
     *
     * IMPORTANT: This method MUST be called inside an active @Transactional context.
     * Locks are held until the transaction ends — calling this outside a transaction
     * would release the lock immediately, defeating the entire purpose.
     */
    public List<Seat> lockSeatsForUpdate(List<Long> seatIds) {
        // Build dynamic IN clause: (?, ?, ?) for however many seat IDs were passed
        String placeholders = String.join(",", seatIds.stream().map(id -> "?").toList());
        String sql = "SELECT * FROM seats WHERE id IN (" + placeholders + ") AND status = 'AVAILABLE' FOR UPDATE";
        return jdbcTemplate.query(sql, seatRowMapper, seatIds.toArray());
    }

    public int updateSeatsStatus(List<Long> seatIds, String status) {
        String placeholders = String.join(",", seatIds.stream().map(id -> "?").toList());
        // CASE expression: set locked_at timestamp only when status is LOCKED,
        // clear it (NULL) when status is BOOKED or AVAILABLE.
        // This keeps the locked_at column accurate for potential lock expiry logic.
        String sql = "UPDATE seats SET status = ?, locked_at = CASE WHEN ? = 'LOCKED' THEN NOW() ELSE NULL END WHERE id IN (" + placeholders + ")";
        Object[] params = new Object[2 + seatIds.size()];
        params[0] = status;
        params[1] = status;
        for (int i = 0; i < seatIds.size(); i++) {
            params[2 + i] = seatIds.get(i);
        }
        return jdbcTemplate.update(sql, params);
    }

    // Fetches seat numbers for a given booking via the booking_seats junction table.
    // WHY a separate query and not a JOIN in the bookings query?
    // One booking → many seats. A JOIN would produce multiple rows per booking,
    // requiring complex deduplication. A separate query is simpler and clearer.
    public List<String> findSeatNumbersByBookingId(Long bookingId) {
        String sql = """
                SELECT s.seat_number FROM seats s
                JOIN booking_seats bs ON s.id = bs.seat_id
                WHERE bs.booking_id = ?
                ORDER BY s.seat_number
                """;
        return jdbcTemplate.queryForList(sql, String.class, bookingId);
    }
}
