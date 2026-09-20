package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.SeatUnavailableException;
import com.himanshu.movieTicketBookingSystem.exception.UnauthorizedException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.DiscountCodeRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.strategy.PaymentStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.PricingStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.RefundPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepo;
    private final ShowRepository showRepo;
    private final SeatRepository seatRepo;
    private final DiscountCodeRepository discountCodeRepo;
    private final RefundPolicyService refundPolicyService;
    private final NotificationService notificationService;
    private final BookingExpiryService bookingExpiryService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final RefundPolicy refundPolicy;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public BookingService(BookingRepository bookingRepo, ShowRepository showRepo, SeatRepository seatRepo,
                          DiscountCodeRepository discountCodeRepo, RefundPolicyService refundPolicyService,
                          NotificationService notificationService, BookingExpiryService bookingExpiryService,
                          PaymentStrategyFactory paymentStrategyFactory, PricingStrategyFactory pricingStrategyFactory,
                          RefundPolicy refundPolicy, TransactionTemplate transactionTemplate,
            Clock clock) {
        this.bookingRepo = bookingRepo;
        this.showRepo = showRepo;
        this.seatRepo = seatRepo;
        this.discountCodeRepo = discountCodeRepo;
        this.refundPolicyService = refundPolicyService;
        this.notificationService = notificationService;
        this.bookingExpiryService = bookingExpiryService;
        this.paymentStrategyFactory = paymentStrategyFactory;
        this.pricingStrategyFactory = pricingStrategyFactory;
        this.refundPolicy = refundPolicy;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    // Frees lapsed holds on the show, then reserves the seats, prices the booking and creates it in CREATED status with a hold expiry.
    public Booking createBooking(int userId, int showId, List<Integer> seatIds) {
        validateSeatIds(seatIds);
        releaseExpiredHolds(showId);
        return transactionTemplate.execute(status -> reserveAndCreate(userId, showId, seatIds));
    }

    // Pays for a CREATED booking owned by the user and confirms it, or marks payment failed.
    @Transactional
    public Booking confirmBooking(int userId, String confirmationId, PaymentType paymentType, String discountCode) {
        Booking booking = findOwnedBookingForUpdate(userId, confirmationId);
        requireConfirmable(booking);
        DiscountCode discount = applyDiscount(booking, discountCode);
        Payment payment = paymentStrategyFactory.forPayment(paymentType).pay(booking);
        if (payment.getStatus() == PaymentStatus.FAILED) {
            failPayment(booking);
        } else {
            completePayment(booking, payment, discount);
        }
        return booking;
    }

    // Cancels the user's booking, releases its seats and applies the refund policy.
    @Transactional
    public void cancelBooking(int userId, String confirmationId) {
        Booking booking = findOwnedBookingForUpdate(userId, confirmationId);
        if (booking.getShow().hasStarted(LocalDateTime.now(clock))) {
            throw new InvalidStateException(Constants.Messages.SHOW_STARTED);
        }
        BigDecimal refund = refundPolicy.calculateRefund(booking, LocalDateTime.now(clock));
        booking.cancel(refund);
        booking.getSeats().forEach(Seat::release);
        if (refund.signum() > 0) {
            booking.getPayment().markRefunded();
        }
        notificationService.bookingCancelled(booking, refund);
    }

    private void validateSeatIds(List<Integer> seatIds) {
        if (seatIds == null || seatIds.isEmpty() || seatIds.stream().anyMatch(Objects::isNull)
                || seatIds.stream().distinct().count() != seatIds.size()) {
            throw new ValidationException("Seat ids must be non-empty, non-null and unique");
        }
    }

    // Runs before the booking transaction opens, so the sweep never needs a second connection while holding one.
    private void releaseExpiredHolds(int showId) {
        try {
            bookingExpiryService.releaseExpiredBookings(showId);
        } catch (RuntimeException e) {
            log.warn("Could not release expired holds for show {} before booking", showId, e);
        }
    }

    // Reserves the seats under row locks and saves the CREATED booking; runs inside one transaction.
    private Booking reserveAndCreate(int userId, int showId, List<Integer> seatIds) {
        Show show = lockBookableShow(showId);
        List<Seat> seats = reserveSeats(showId, seatIds);
        BigDecimal amount = pricingStrategyFactory.forTier(show.getPricingTier()).calculatePrice(show, seats);
        LocalDateTime now = LocalDateTime.now(clock);
        Booking booking = new Booking(UUID.randomUUID().toString(), userId, show, seats, amount,
                now, now.plus(Constants.BookingRules.HOLD_DURATION), refundPolicyService.resolveFor(show));
        return bookingRepo.save(booking);
    }

    // Takes a shared lock on the show, so a concurrent cancellation waits for this booking, and checks it can be booked.
    private Show lockBookableShow(int showId) {
        Show show = showRepo.findByIdForShare(showId).orElseThrow(() -> NotFoundException.of("Show", showId));
        if (show.isCancelled()) {
            throw new InvalidStateException("Show is cancelled");
        }
        if (show.hasStarted(LocalDateTime.now(clock))) {
            throw new InvalidStateException(Constants.Messages.SHOW_STARTED);
        }
        return show;
    }

    // Locks the requested seats in id order and reserves each one.
    private List<Seat> reserveSeats(int showId, List<Integer> seatIds) {
        List<Seat> seats = seatRepo.findByShowIdAndIdIn(showId, seatIds);
        if (seats.size() != seatIds.size()) {
            throw new NotFoundException("One or more seats not found for show " + showId);
        }
        for (Seat seat : seats) {
            if (!seat.reserve()) {
                throw new SeatUnavailableException("Seat " + seat.getId() + " is not available");
            }
        }
        return seats;
    }

    // Loads the booking under a write lock; throws NotFoundException if missing or UnauthorizedException if not the user's.
    private Booking findOwnedBookingForUpdate(int userId, String confirmationId) {
        Booking booking = bookingRepo.findByIdForUpdate(confirmationId)
                .orElseThrow(() -> NotFoundException.of("Booking", confirmationId));
        if (booking.getUserId() != userId) {
            throw new UnauthorizedException(Constants.Messages.NOT_BOOKING_OWNER);
        }
        return booking;
    }

    // Requires a CREATED booking whose hold has not lapsed and whose show is still on and not started.
    private void requireConfirmable(Booking booking) {
        if (booking.getBookingStatus() != BookingStatus.CREATED) {
            throw new InvalidStateException("Booking is " + booking.getBookingStatus() + ", not CREATED");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.isAfter(booking.getExpiresAt())) {
            throw new InvalidStateException("Booking hold has expired");
        }
        Show show = booking.getShow();
        if (show.isCancelled() || show.hasStarted(now)) {
            throw new InvalidStateException("Show has been cancelled or has already started");
        }
    }

    // Locks and validates the discount code, if one was given, and applies it to the booking; returns null if none.
    private DiscountCode applyDiscount(Booking booking, String discountCode) {
        if (discountCode == null || discountCode.isBlank()) {
            return null;
        }
        DiscountCode discount = discountCodeRepo.findByCodeForUpdate(discountCode.trim().toUpperCase())
                .orElseThrow(() -> new ValidationException("Invalid discount code"));
        if (!discount.isRedeemable(LocalDateTime.now(clock))) {
            throw new ValidationException("Discount code is inactive, expired or fully used");
        }
        booking.applyDiscount(discount.getCode(), discount.calculateDiscount(booking.getAmount()));
        return discount;
    }

    private void failPayment(Booking booking) {
        booking.markPaymentFailed();
        booking.clearDiscount();
        booking.getSeats().forEach(Seat::release);
        notificationService.paymentFailed(booking);
    }

    private void completePayment(Booking booking, Payment payment, DiscountCode discount) {
        booking.confirm(payment);
        if (discount != null) {
            discount.recordUse();
        }
        booking.getSeats().forEach(Seat::book);
        notificationService.bookingConfirmed(booking);
    }
}
