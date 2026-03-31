package com.moviebooking.dao;

import com.moviebooking.model.Movie;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MovieDao {

    private final JdbcTemplate jdbcTemplate;

    public MovieDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Movie> movieRowMapper = (rs, rowNum) -> {
        Movie movie = new Movie();
        movie.setId(rs.getLong("id"));
        movie.setTitle(rs.getString("title"));
        movie.setDuration(rs.getInt("duration"));
        movie.setGenre(rs.getString("genre"));
        movie.setDescription(rs.getString("description"));
        movie.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return movie;
    };

    public List<Movie> findAll() {
        return jdbcTemplate.query("SELECT * FROM movies ORDER BY title", movieRowMapper);
    }

    public Optional<Movie> findById(Long id) {
        String sql = "SELECT * FROM movies WHERE id = ?";
        return jdbcTemplate.query(sql, movieRowMapper, id).stream().findFirst();
    }
}
