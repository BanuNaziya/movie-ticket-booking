package com.moviebooking.service;

import com.moviebooking.dao.MovieDao;
import com.moviebooking.dao.ShowDao;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Show;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieDao movieDao;
    private final ShowDao showDao;

    public MovieService(MovieDao movieDao, ShowDao showDao) {
        this.movieDao = movieDao;
        this.showDao = showDao;
    }

    public List<Movie> getAllMovies() {
        return movieDao.findAll();
    }

    public List<Show> getShowsByMovie(Long movieId) {
        movieDao.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Movie not found with id: " + movieId));
        return showDao.findByMovieId(movieId);
    }
}
