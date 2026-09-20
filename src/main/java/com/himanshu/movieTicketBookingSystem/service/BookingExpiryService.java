package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingExpiryService {

    private static final Logger log = LoggerFactory.getLogger(BookingExpiryService.class);

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private SeatRepository seatRepo;

    @Autowired
    private PlatformTransactionManager txManager;

    // Expires CREATED bookings past their hold time and releases their seats; runs every 30 seconds.
    @Scheduled(fixedDelay = 30_000)
    public void releaseExpiredBookings() {
        expireAll(bookingRepo.findExpiredBookingIds(LocalDateTime.now()));
    }

    // Same as the scheduled sweep but only for one show; run before a booking is created so lapsed holds free their seats at once.
    public void releaseExpiredBookings(int showId) {
        expireAll(bookingRepo.findExpiredBookingIdsByShowId(LocalDateTime.now(), showId));
    }

    // Expires each booking in its own transaction so one failure does not stop the others.
    private void expireAll(List<String> ids) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        for (String id : ids) {
            try {
                tx.executeWithoutResult(status -> expire(id));
            } catch (RuntimeException e) {
                log.warn("Could not expire booking {}", id, e);
            }
        }
    }

    // Locks the booking, re-checks it is still an expired CREATED hold, then expires it and frees its seats.
    private void expire(String confirmationId) {
        bookingRepo.findByIdForUpdate(confirmationId)
                .filter(b -> b.getBookingStatus() == BookingStatus.CREATED
                        && b.getExpiresAt().isBefore(LocalDateTime.now()))
                .ifPresent(b -> {
                    b.expire();
                    b.getSeats().forEach(Seat::release);
                    seatRepo.saveAll(b.getSeats());
                    bookingRepo.save(b);
                    log.info("Expired booking {} and released {} seat(s)", b.getConfirmationId(), b.getSeats().size());
                });
    }
}
