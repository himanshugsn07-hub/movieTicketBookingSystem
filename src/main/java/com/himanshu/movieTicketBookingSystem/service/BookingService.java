package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.SeatUnavailableException;
import com.himanshu.movieTicketBookingSystem.exception.UnauthorizedException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.strategy.PaymentStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.PricingStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.RefundPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private MovieRepository movieRepo;

    @Autowired
    private ShowRepository showRepo;

    @Autowired
    private SeatRepository seatRepo;

    @Autowired
    private PaymentStrategyFactory paymentStrategyFactory;

    @Autowired
    private PricingStrategyFactory pricingStrategyFactory;

    @Autowired
    private RefundPolicy refundPolicy;

    // Returns the user's bookings, newest first, optionally filtered by status.
    @Transactional(readOnly = true)
    public List<Booking> getBookingHistory(int userId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return status == null
                ? bookingRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : bookingRepo.findByUserIdAndBookingStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    // Returns one of the user's bookings, or throws NotFoundException or UnauthorizedException.
    @Transactional(readOnly = true)
    public Booking getBooking(int userId, String confirmationId) {
        Booking booking = bookingRepo.findById(confirmationId)
                .orElseThrow(() -> new NotFoundException("Booking " + confirmationId + " not found"));
        if (booking.getUserId() != userId) {
            throw new UnauthorizedException("Booking does not belong to user " + userId);
        }
        return booking;
    }

    // Loads the show or throws NotFoundException.
    private Show findShow(int showId) {
        return showRepo.findById(showId)
                .orElseThrow(() -> new NotFoundException("Show " + showId + " not found"));
    }

    // Loads the booking under a write lock and throws NotFoundException or UnauthorizedException if it is missing or not the user's.
    private Booking findOwnedBooking(int userId, String confirmationId) {
        Booking booking = bookingRepo.findByIdForUpdate(confirmationId)
                .orElseThrow(() -> new NotFoundException("Booking " + confirmationId + " not found"));
        if (booking.getUserId() != userId) {
            throw new UnauthorizedException("Booking does not belong to user " + userId);
        }
        return booking;
    }

    // Searches movies by title that are showing in the given city.
    public List<Movie> searchMovies(String title, int cityId) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Title must not be blank");
        }
        return movieRepo.findByTitleInCity(title.trim(), cityId);
    }

    // Returns the available seats for the given show.
    public List<Seat> getAvailableSeats(int showId) {
        findShow(showId);
        return seatRepo.findByShowIdAndStatus(showId, SeatStatus.AVAILABLE);
    }

    // Reserves the seats, prices the booking and creates it in CREATED status with a hold expiry.
    @Transactional
    public Booking createBooking(int userId, int showId, List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty() || seatIds.stream().distinct().count() != seatIds.size()) {
            throw new ValidationException("Seat ids must be non-empty and unique");
        }
        Show show = findShow(showId);
        if (show.isCancelled()) {
            throw new InvalidStateException("Show is cancelled");
        }
        if (show.hasStarted()) {
            throw new InvalidStateException("Show has already started");
        }
        List<Seat> seats = seatRepo.findByShowIdAndIdIn(showId, seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("One or more seats not found for show " + showId);
        }
        for (Seat seat : seats) {
            if (!seat.reserve()) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is not available");
            }
        }
        BigDecimal amount = pricingStrategyFactory.forTier(show.getPricingTier()).calculatePrice(show, seats);
        LocalDateTime now = LocalDateTime.now();
        Booking booking = new Booking(UUID.randomUUID().toString(), userId, show, seats, amount,
                now, now.plus(HOLD_DURATION));
        seatRepo.saveAll(seats);
        return bookingRepo.save(booking);
    }

    // Pays for a CREATED booking owned by the user and confirms it, or marks payment failed.
    @Transactional
    public Booking confirmBooking(int userId, String confirmationId, PaymentType paymentType) {
        Booking booking = findOwnedBooking(userId, confirmationId);
        if (booking.getBookingStatus() != BookingStatus.CREATED) {
            throw new InvalidStateException("Booking is " + booking.getBookingStatus() + ", not CREATED");
        }
        if (LocalDateTime.now().isAfter(booking.getExpiresAt())) {
            throw new InvalidStateException("Booking hold has expired");
        }
        Payment payment = paymentStrategyFactory.forPayment(paymentType).pay(booking);
        if (payment.getStatus() == PaymentStatus.FAILED) {
            booking.markPaymentFailed();
            booking.getSeats().forEach(Seat::release);
        } else {
            booking.confirm(payment);
            booking.getSeats().forEach(Seat::book);
        }
        seatRepo.saveAll(booking.getSeats());
        return bookingRepo.save(booking);
    }

    // Cancels the user's booking, releases its seats and applies the refund policy.
    @Transactional
    public boolean cancelBooking(int userId, String confirmationId) {
        Booking booking = findOwnedBooking(userId, confirmationId);
        if (booking.getShow().hasStarted()) {
            throw new InvalidStateException("Show has already started");
        }
        BigDecimal refund = refundPolicy.calculateRefund(booking, LocalDateTime.now());
        booking.cancel(refund);
        booking.getSeats().forEach(Seat::release);
        if (refund.signum() > 0) {
            booking.getPayment().markRefunded();
        }
        seatRepo.saveAll(booking.getSeats());
        bookingRepo.save(booking);
        return true;
    }
}
