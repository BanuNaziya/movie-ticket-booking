package com.moviebooking.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Seat {
    private Long id;
    private Long showId;
    private String seatNumber;
    private String status; // AVAILABLE, LOCKED, BOOKED
    private LocalDateTime lockedAt;
}
