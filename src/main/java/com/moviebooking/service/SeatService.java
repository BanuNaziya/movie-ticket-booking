package com.moviebooking.service;

import com.moviebooking.dao.SeatDao;
import com.moviebooking.dao.ShowDao;
import com.moviebooking.model.Seat;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SeatService {

    private final SeatDao seatDao;
    private final ShowDao showDao;

    public SeatService(SeatDao seatDao, ShowDao showDao) {
        this.seatDao = seatDao;
        this.showDao = showDao;
    }

    public List<Seat> getSeatsByShow(Long showId) {
        showDao.findById(showId)
                .orElseThrow(() -> new IllegalArgumentException("Show not found with id: " + showId));
        return seatDao.findByShowId(showId);
    }
}
