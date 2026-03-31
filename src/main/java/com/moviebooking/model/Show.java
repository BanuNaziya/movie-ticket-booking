package com.moviebooking.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Show {
    private Long id;
    private Long movieId;
    private Long screenId;
    private LocalDateTime showTime;
    private BigDecimal price;
    private LocalDateTime createdAt;

    // Joined fields
    private String movieTitle;
    private String screenName;
}
