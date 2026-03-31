package com.moviebooking.dao;

import com.moviebooking.model.Show;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ShowDao {

    private final JdbcTemplate jdbcTemplate;

    public ShowDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Show> showRowMapper = (rs, rowNum) -> {
        Show show = new Show();
        show.setId(rs.getLong("id"));
        show.setMovieId(rs.getLong("movie_id"));
        show.setScreenId(rs.getLong("screen_id"));
        show.setShowTime(rs.getTimestamp("show_time").toLocalDateTime());
        show.setPrice(rs.getBigDecimal("price"));
        show.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        show.setMovieTitle(rs.getString("movie_title"));
        show.setScreenName(rs.getString("screen_name"));
        return show;
    };

    public List<Show> findByMovieId(Long movieId) {
        String sql = """
                SELECT s.*, m.title AS movie_title, sc.name AS screen_name
                FROM shows s
                JOIN movies m ON s.movie_id = m.id
                JOIN screens sc ON s.screen_id = sc.id
                WHERE s.movie_id = ?
                ORDER BY s.show_time
                """;
        return jdbcTemplate.query(sql, showRowMapper, movieId);
    }

    public Optional<Show> findById(Long id) {
        String sql = """
                SELECT s.*, m.title AS movie_title, sc.name AS screen_name
                FROM shows s
                JOIN movies m ON s.movie_id = m.id
                JOIN screens sc ON s.screen_id = sc.id
                WHERE s.id = ?
                """;
        return jdbcTemplate.query(sql, showRowMapper, id).stream().findFirst();
    }
}
