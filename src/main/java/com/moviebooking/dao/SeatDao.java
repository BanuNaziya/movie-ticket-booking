package com.moviebooking.dao;

import com.moviebooking.model.Seat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SeatDao {

    private final JdbcTemplate jdbcTemplate;

    public SeatDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Seat> seatRowMapper = (rs, rowNum) -> {
        Seat seat = new Seat();
        seat.setId(rs.getLong("id"));
        seat.setShowId(rs.getLong("show_id"));
        seat.setSeatNumber(rs.getString("seat_number"));
        seat.setStatus(rs.getString("status"));
        var lockedAt = rs.getTimestamp("locked_at");
        if (lockedAt != null) seat.setLockedAt(lockedAt.toLocalDateTime());
        return seat;
    };

    public List<Seat> findByShowId(Long showId) {
        String sql = "SELECT * FROM seats WHERE show_id = ? ORDER BY seat_number";
        return jdbcTemplate.query(sql, seatRowMapper, showId);
    }

    /**
     * Lock seats using SELECT FOR UPDATE (pessimistic locking).
     * Must be called within a transaction.
     */
    public List<Seat> lockSeatsForUpdate(List<Long> seatIds) {
        String placeholders = String.join(",", seatIds.stream().map(id -> "?").toList());
        String sql = "SELECT * FROM seats WHERE id IN (" + placeholders + ") AND status = 'AVAILABLE' FOR UPDATE";
        return jdbcTemplate.query(sql, seatRowMapper, seatIds.toArray());
    }

    public int updateSeatsStatus(List<Long> seatIds, String status) {
        String placeholders = String.join(",", seatIds.stream().map(id -> "?").toList());
        String sql = "UPDATE seats SET status = ?, locked_at = CASE WHEN ? = 'LOCKED' THEN NOW() ELSE NULL END WHERE id IN (" + placeholders + ")";
        Object[] params = new Object[2 + seatIds.size()];
        params[0] = status;
        params[1] = status;
        for (int i = 0; i < seatIds.size(); i++) {
            params[2 + i] = seatIds.get(i);
        }
        return jdbcTemplate.update(sql, params);
    }

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
