package com.moviebooking.dao;

import com.moviebooking.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password")); // stored as BCrypt hash, never plain text
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return user;
    };

    public Long save(User user) {
        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";

        // WHY GeneratedKeyHolder?
        // After INSERT, we need the auto-generated primary key (id) to return
        // to the caller (so we can immediately generate a JWT with the userId).
        // Statement.RETURN_GENERATED_KEYS tells JDBC to capture the DB-generated id.
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(conn -> {
            PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    // Returns Optional<User> instead of User to force callers to handle
    // the "user not found" case explicitly, preventing NullPointerExceptions.
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        // .query() returns a List — .stream().findFirst() converts to Optional safely
        // even when the query returns 0 rows (unlike queryForObject which throws).
        return jdbcTemplate.query(sql, userRowMapper, email).stream().findFirst();
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return jdbcTemplate.query(sql, userRowMapper, id).stream().findFirst();
    }

    // WHY a separate existsByEmail instead of findByEmail + checking Optional?
    // COUNT(*) is faster — the DB doesn't need to fetch and transfer the full row,
    // just confirm existence. Also makes the intent of the check explicit.
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email);
        return count != null && count > 0;
    }
}
