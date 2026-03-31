package com.moviebooking.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    private Long id;
    private Long userId;
    private Long showId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    // Joined fields
    private List<String> seatNumbers;
    private String movieTitle;
    private LocalDateTime showTime;
    private String screenName;
}
