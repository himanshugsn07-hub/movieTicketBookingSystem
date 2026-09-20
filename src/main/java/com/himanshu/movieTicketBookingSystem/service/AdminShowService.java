package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import com.himanshu.movieTicketBookingSystem.repository.ScreenRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AdminShowService {

    private final ShowRepository showRepo;
    private final ScreenRepository screenRepo;
    private final BookingRepository bookingRepo;
    private final NotificationService notificationService;
    private final MovieRepository movieRepo;
    private final RefundPolicyConfigRepository refundPolicyRepo;
    private final Clock clock;

    public AdminShowService(ShowRepository showRepo, ScreenRepository screenRepo, BookingRepository bookingRepo, NotificationService notificationService, MovieRepository movieRepo, RefundPolicyConfigRepository refundPolicyRepo,
            Clock clock) {
        this.showRepo = showRepo;
        this.screenRepo = screenRepo;
        this.bookingRepo = bookingRepo;
        this.notificationService = notificationService;
        this.movieRepo = movieRepo;
        this.refundPolicyRepo = refundPolicyRepo;
        this.clock = clock;
    }

    // Schedules a show on a screen (locked to serialize scheduling) and generates its seats from the screen layout.
    public Show createShow(int screenId, String movieId, BigDecimal basePrice, PricingTier pricingTier,
                           LocalDateTime startTime) {
        Screen screen = screenRepo.findByIdForUpdate(screenId)
                .orElseThrow(() -> NotFoundException.of("Screen", screenId));
        Movie movie = movieRepo.findById(movieId)
                .orElseThrow(() -> NotFoundException.of("Movie", movieId));
        if (!startTime.isAfter(LocalDateTime.now(clock))) {
            throw new ValidationException("Show must start in the future");
        }
        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMin());
        if (showRepo.existsOverlapping(screenId, startTime, endTime)) {
            throw new ConflictException("Screen already has a show overlapping " + startTime + " - " + endTime);
        }
        Show show = new Show(screen, movie, basePrice, pricingTier, startTime, endTime);
        show.generateSeats(screen.getRows(), screen.getColumns());
        return showRepo.save(show);
    }

    // Assigns a refund policy to a show that has not started, or clears it (null) to use the default policy.
    public Show assignRefundPolicy(int showId, Integer refundPolicyId) {
        Show show = findShow(showId);
        if (show.isCancelled() || show.hasStarted(LocalDateTime.now(clock))) {
            throw new InvalidStateException("Cannot change the refund policy of a cancelled or started show");
        }
        show.assignRefundPolicy(refundPolicyId == null ? null : refundPolicyRepo.findById(refundPolicyId)
                .orElseThrow(() -> NotFoundException.of("Refund policy", refundPolicyId)));
        return show;
    }

    public record CancellationResult(int showId, int refundedBookings, int expiredBookings, BigDecimal totalRefunded) {
    }

    // Cancels a show that has not started: CONFIRMED bookings are cancelled with a full refund of what was paid
    // (refund policy ignored), CREATED holds are expired, and all their seats are released, all in one transaction.
    public CancellationResult cancelShow(int showId) {
        Show show = showRepo.findByIdForUpdate(showId).orElseThrow(() -> NotFoundException.of("Show", showId));
        if (show.isCancelled()) {
            throw new InvalidStateException("Show is already cancelled");
        }
        if (show.hasStarted(LocalDateTime.now(clock))) {
            throw new InvalidStateException("Cannot cancel a show that has already started");
        }
        show.cancel();
        int refunded = 0;
        int expired = 0;
        BigDecimal totalRefunded = BigDecimal.ZERO;
        List<BookingStatus> liveStatuses = List.of(BookingStatus.CREATED, BookingStatus.CONFIRMED);
        for (String id : bookingRepo.findIdsByShowIdAndStatuses(showId, liveStatuses)) {
            Booking booking = bookingRepo.findByIdForUpdate(id).orElse(null);
            if (booking == null) {
                continue;
            }
            if (booking.getBookingStatus() == BookingStatus.CONFIRMED) {
                totalRefunded = totalRefunded.add(refundInFull(booking));
                refunded++;
            } else if (booking.getBookingStatus() == BookingStatus.CREATED) {
                releaseHold(booking);
                expired++;
            }
        }
        return new CancellationResult(showId, refunded, expired, totalRefunded);
    }

    // Cancels a confirmed booking, refunds everything it paid, frees its seats and tells the customer; returns the refund.
    private BigDecimal refundInFull(Booking booking) {
        BigDecimal refund = booking.getPayableAmount();
        booking.cancel(refund);
        if (refund.signum() > 0) {
            booking.getPayment().markRefunded();
        }
        booking.getSeats().forEach(Seat::release);
        notificationService.showCancelled(booking, refund);
        return refund;
    }

    // Expires a held booking, frees its seats and tells the customer.
    private void releaseHold(Booking booking) {
        booking.expire();
        booking.getSeats().forEach(Seat::release);
        notificationService.showCancelled(booking, BigDecimal.ZERO);
    }

    // Lists shows, optionally filtered by screen.
    @Transactional(readOnly = true)
    public List<Show> listShows(Integer screenId) {
        return screenId == null ? showRepo.findAll() : showRepo.findByScreenId(screenId);
    }

    // Changes a show's base price and pricing tier; not allowed once it is cancelled or started.
    public Show updatePricing(int showId, BigDecimal basePrice, PricingTier pricingTier) {
        Show show = findShow(showId);
        if (show.isCancelled() || show.hasStarted(LocalDateTime.now(clock))) {
            throw new InvalidStateException("Cannot reprice a cancelled or started show");
        }
        show.reprice(basePrice, pricingTier);
        return show;
    }

    private Show findShow(int showId) {
        return showRepo.findById(showId).orElseThrow(() -> NotFoundException.of("Show", showId));
    }
}
