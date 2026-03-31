package com.moviebooking.service;

import com.moviebooking.dao.BookingDao;
import com.moviebooking.dao.SeatDao;
import com.moviebooking.dao.ShowDao;
import com.moviebooking.dto.BookingRequest;
import com.moviebooking.model.Booking;
import com.moviebooking.model.Seat;
import com.moviebooking.model.Show;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BookingService {

    private final BookingDao bookingDao;
    private final SeatDao seatDao;
    private final ShowDao showDao;

    public BookingService(BookingDao bookingDao, SeatDao seatDao, ShowDao showDao) {
        this.bookingDao = bookingDao;
        this.seatDao = seatDao;
        this.showDao = showDao;
    }

    /**
     * Book seats with SERIALIZABLE isolation + SELECT FOR UPDATE (pessimistic locking).
     * Guarantees no double booking even under high concurrency.
     *
     * Flow:
     * 1. Start transaction (SERIALIZABLE isolation)
     * 2. SELECT seats FOR UPDATE → acquires row-level locks
     * 3. Verify all requested seats are AVAILABLE
     * 4. Mark seats as BOOKED
     * 5. Create booking record
     * 6. Commit → locks released
     * If any step fails → automatic rollback
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Booking bookSeats(Long userId, BookingRequest request) {
        // Step 1: Validate show exists
        Show show = showDao.findById(request.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found with id: " + request.getShowId()));

        List<Long> requestedSeatIds = request.getSeatIds();

        // Step 2: Lock seats using SELECT FOR UPDATE (pessimistic locking)
        // This blocks any concurrent transaction trying to book the same seats
        List<Seat> availableSeats = seatDao.lockSeatsForUpdate(requestedSeatIds);

        // Step 3: Verify ALL requested seats are available
        if (availableSeats.size() != requestedSeatIds.size()) {
            List<Long> availableIds = availableSeats.stream().map(Seat::getId).toList();
            List<Long> unavailableIds = requestedSeatIds.stream()
                    .filter(id -> !availableIds.contains(id))
                    .toList();
            throw new IllegalStateException(
                "Seats are no longer available: " + unavailableIds +
                ". Please select different seats."
            );
        }

        // Step 4: Mark seats as BOOKED
        seatDao.updateSeatsStatus(requestedSeatIds, "BOOKED");

        // Step 5: Calculate total amount
        BigDecimal totalAmount = show.getPrice().multiply(BigDecimal.valueOf(requestedSeatIds.size()));

        // Step 6: Create booking record
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowId(request.getShowId());
        booking.setTotalAmount(totalAmount);
        booking.setStatus("CONFIRMED");

        Long bookingId = bookingDao.save(booking);
        bookingDao.saveBookingSeats(bookingId, requestedSeatIds);

        // Step 7: Return full booking details
        Booking savedBooking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve booking after save"));
        savedBooking.setSeatNumbers(seatDao.findSeatNumbersByBookingId(bookingId));

        return savedBooking;
    }

    public Booking getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: booking does not belong to you");
        }

        booking.setSeatNumbers(seatDao.findSeatNumbersByBookingId(bookingId));
        return booking;
    }

    public List<Booking> getMyBookings(Long userId) {
        List<Booking> bookings = bookingDao.findByUserId(userId);
        bookings.forEach(b -> b.setSeatNumbers(seatDao.findSeatNumbersByBookingId(b.getId())));
        return bookings;
    }
}
