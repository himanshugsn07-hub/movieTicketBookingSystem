package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    private final BookingRepository bookingRepo;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public BookingExpiryService(BookingRepository bookingRepo, TransactionTemplate transactionTemplate, Clock clock) {
        this.bookingRepo = bookingRepo;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    // Expires CREATED bookings past their hold time and releases their seats; runs every 30 seconds.
    @Scheduled(fixedDelay = Constants.Scheduling.EXPIRY_SWEEP_MS)
    public void releaseExpiredBookings() {
        expireAll(bookingRepo.findExpiredBookingIds(LocalDateTime.now(clock)));
    }

    // Same as the scheduled sweep but only for one show; run before a booking is created so lapsed holds free their seats at once.
    public void releaseExpiredBookings(int showId) {
        expireAll(bookingRepo.findExpiredBookingIdsByShowId(LocalDateTime.now(clock), showId));
    }

    // Expires each booking in its own transaction so one failure does not stop the others.
    private void expireAll(List<String> ids) {
        for (String id : ids) {
            try {
                transactionTemplate.executeWithoutResult(status -> expire(id));
            } catch (RuntimeException e) {
                log.warn("Could not expire booking {}", id, e);
            }
        }
    }

    // Locks the booking, re-checks it is still an expired CREATED hold, then expires it and frees its seats.
    private void expire(String confirmationId) {
        bookingRepo.findByIdForUpdate(confirmationId)
                .filter(b -> b.getBookingStatus() == BookingStatus.CREATED
                        && b.getExpiresAt().isBefore(LocalDateTime.now(clock)))
                .ifPresent(b -> {
                    b.expire();
                    b.getSeats().forEach(Seat::release);
                    log.info("Expired booking {} and released {} seat(s)", b.getConfirmationId(), b.getSeats().size());
                });
    }
}
