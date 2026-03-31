package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.model.Movie;
import com.moviebooking.model.Show;
import com.moviebooking.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    public ResponseEntity<ApiResponse<List<Movie>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success("Movies fetched", movieService.getAllMovies()));
    }

    @GetMapping("/shows/{movieId}")
    public ResponseEntity<ApiResponse<List<Show>>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success("Shows fetched", movieService.getShowsByMovie(movieId)));
    }
}
