package com.moviebooking.dao;

import com.moviebooking.model.Booking;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class BookingDao {

    private final JdbcTemplate jdbcTemplate;

    public BookingDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Booking> bookingRowMapper = (rs, rowNum) -> {
        Booking booking = new Booking();
        booking.setId(rs.getLong("id"));
        booking.setUserId(rs.getLong("user_id"));
        booking.setShowId(rs.getLong("show_id"));
        booking.setTotalAmount(rs.getBigDecimal("total_amount"));
        booking.setStatus(rs.getString("status"));
        booking.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        booking.setMovieTitle(rs.getString("movie_title"));
        booking.setShowTime(rs.getTimestamp("show_time").toLocalDateTime());
        booking.setScreenName(rs.getString("screen_name"));
        return booking;
    };

    public Long save(Booking booking) {
        String sql = "INSERT INTO bookings (user_id, show_id, total_amount, status) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, booking.getUserId());
            ps.setLong(2, booking.getShowId());
            ps.setBigDecimal(3, booking.getTotalAmount());
            ps.setString(4, booking.getStatus() != null ? booking.getStatus() : "CONFIRMED");
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void saveBookingSeats(Long bookingId, List<Long> seatIds) {
        String sql = "INSERT INTO booking_seats (booking_id, seat_id) VALUES (?, ?)";
        for (Long seatId : seatIds) {
            jdbcTemplate.update(sql, bookingId, seatId);
        }
    }

    public Optional<Booking> findById(Long id) {
        String sql = """
                SELECT b.*, m.title AS movie_title, sh.show_time, sc.name AS screen_name
                FROM bookings b
                JOIN shows sh ON b.show_id = sh.id
                JOIN movies m ON sh.movie_id = m.id
                JOIN screens sc ON sh.screen_id = sc.id
                WHERE b.id = ?
                """;
        return jdbcTemplate.query(sql, bookingRowMapper, id).stream().findFirst();
    }

    public List<Booking> findByUserId(Long userId) {
        String sql = """
                SELECT b.*, m.title AS movie_title, sh.show_time, sc.name AS screen_name
                FROM bookings b
                JOIN shows sh ON b.show_id = sh.id
                JOIN movies m ON sh.movie_id = m.id
                JOIN screens sc ON sh.screen_id = sc.id
                WHERE b.user_id = ?
                ORDER BY b.created_at DESC
                """;
        return jdbcTemplate.query(sql, bookingRowMapper, userId);
    }
}
