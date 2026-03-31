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

    // Constructor injection is preferred over @Autowired because it makes
    // dependencies explicit and allows the class to be easily unit tested
    public BookingService(BookingDao bookingDao, SeatDao seatDao, ShowDao showDao) {
        this.bookingDao = bookingDao;
        this.seatDao = seatDao;
        this.showDao = showDao;
    }

    /**
     * Core booking method — handles the most critical business logic.
     *
     * WHY @Transactional(isolation = SERIALIZABLE)?
     * - @Transactional ensures all DB operations succeed or all rollback together.
     *   If seat update succeeds but booking insert fails, seats are rolled back automatically.
     * - SERIALIZABLE is the strictest isolation level. It prevents two transactions
     *   from reading/writing the same rows simultaneously, which is essential to
     *   avoid race conditions like double booking.
     *
     * WHY Pessimistic Locking (SELECT FOR UPDATE) instead of Optimistic?
     * - Optimistic locking uses a version column and retries on conflict — fine for
     *   low-conflict scenarios, but seat booking has HIGH conflict (many users race
     *   for popular seats). Pessimistic locking is more reliable here: it blocks
     *   concurrent transactions immediately at the DB level rather than failing after
     *   the fact and requiring retries.
     *
     * Booking flow:
     *   1. Validate show exists
     *   2. SELECT seats FOR UPDATE  → acquires row-level DB locks
     *   3. Verify all seats are AVAILABLE (another txn may have booked them while we waited)
     *   4. Mark seats as BOOKED
     *   5. Insert booking + booking_seats records
     *   6. COMMIT → locks released, other waiting transactions unblock
     *   If any step throws → ROLLBACK automatically, no partial state saved
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Booking bookSeats(Long userId, BookingRequest request) {

        // Validate show exists before attempting any seat locks
        Show show = showDao.findById(request.getShowId())
                .orElseThrow(() -> new IllegalArgumentException("Show not found with id: " + request.getShowId()));

        List<Long> requestedSeatIds = request.getSeatIds();

        // Acquire row-level locks via SELECT FOR UPDATE.
        // Any other transaction trying to lock the same seats will WAIT here
        // until this transaction commits or rolls back. This is the key line
        // that prevents double booking.
        List<Seat> availableSeats = seatDao.lockSeatsForUpdate(requestedSeatIds);

        // After acquiring locks, re-verify availability.
        // WHY re-verify? Because between the user viewing seats and calling /book,
        // another transaction may have already booked them. The lock prevents
        // concurrent booking, but we still need to confirm all seats are AVAILABLE.
        if (availableSeats.size() != requestedSeatIds.size()) {
            List<Long> availableIds = availableSeats.stream().map(Seat::getId).toList();
            List<Long> unavailableIds = requestedSeatIds.stream()
                    .filter(id -> !availableIds.contains(id))
                    .toList();
            // Throwing here triggers automatic ROLLBACK — locks are released cleanly
            throw new IllegalStateException(
                "Seats are no longer available: " + unavailableIds +
                ". Please select different seats."
            );
        }

        // All requested seats are confirmed AVAILABLE and locked — safe to book
        seatDao.updateSeatsStatus(requestedSeatIds, "BOOKED");

        // Price × number of seats. Using BigDecimal (not double/float) because
        // floating point arithmetic is imprecise for monetary calculations
        BigDecimal totalAmount = show.getPrice().multiply(BigDecimal.valueOf(requestedSeatIds.size()));

        // Persist the booking record
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setShowId(request.getShowId());
        booking.setTotalAmount(totalAmount);
        booking.setStatus("CONFIRMED");

        Long bookingId = bookingDao.save(booking);

        // booking_seats is a junction table linking one booking to multiple seats
        bookingDao.saveBookingSeats(bookingId, requestedSeatIds);

        // Fetch the full booking with joined movie/screen info for the response
        Booking savedBooking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Failed to retrieve booking after save"));
        savedBooking.setSeatNumbers(seatDao.findSeatNumbersByBookingId(bookingId));

        return savedBooking;
        // COMMIT happens here automatically — all DB locks released
    }

    public Booking getBookingById(Long bookingId, Long userId) {
        Booking booking = bookingDao.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Ownership check — a user should only be able to view their own bookings.
        // This prevents user A from fetching user B's booking by guessing IDs.
        if (!booking.getUserId().equals(userId)) {
            throw new SecurityException("Access denied: booking does not belong to you");
        }

        booking.setSeatNumbers(seatDao.findSeatNumbersByBookingId(bookingId));
        return booking;
    }

    public List<Booking> getMyBookings(Long userId) {
        List<Booking> bookings = bookingDao.findByUserId(userId);
        // Enrich each booking with its seat numbers in a second query.
        // WHY not a single JOIN? A booking can have multiple seats, which would
        // cause duplicate booking rows in a JOIN result and complicate mapping.
        bookings.forEach(b -> b.setSeatNumbers(seatDao.findSeatNumbersByBookingId(b.getId())));
        return bookings;
    }
}
