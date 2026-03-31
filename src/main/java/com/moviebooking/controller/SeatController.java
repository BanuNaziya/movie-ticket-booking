package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.model.Seat;
import com.moviebooking.service.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SeatController {

    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/seats/{showId}")
    public ResponseEntity<ApiResponse<List<Seat>>> getSeatsByShow(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success("Seats fetched", seatService.getSeatsByShow(showId)));
    }
}
