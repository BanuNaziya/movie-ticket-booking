package com.moviebooking.controller;

import com.moviebooking.dto.ApiResponse;
import com.moviebooking.dto.BookingRequest;
import com.moviebooking.model.Booking;
import com.moviebooking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<Booking>> bookTickets(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getCredentials();
        Booking booking = bookingService.bookSeats(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmed", booking));
    }

    @GetMapping("/booking/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBooking(
            @PathVariable Long id,
            Authentication authentication) {

        Long userId = (Long) authentication.getCredentials();
        Booking booking = bookingService.getBookingById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Booking fetched", booking));
    }

    @GetMapping("/bookings/my")
    public ResponseEntity<ApiResponse<List<Booking>>> getMyBookings(Authentication authentication) {
        Long userId = (Long) authentication.getCredentials();
        return ResponseEntity.ok(ApiResponse.success("Bookings fetched", bookingService.getMyBookings(userId)));
    }
}
