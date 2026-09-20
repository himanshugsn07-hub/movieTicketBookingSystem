package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.UnauthorizedException;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.strategy.RefundPolicy;
import com.himanshu.movieTicketBookingSystem.util.PageRequests;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class BookingQueryService {

    private final BookingRepository bookingRepo;
    private final RefundPolicy refundPolicy;
    private final Clock clock;

    public BookingQueryService(BookingRepository bookingRepo, RefundPolicy refundPolicy, Clock clock) {
        this.bookingRepo = bookingRepo;
        this.refundPolicy = refundPolicy;
        this.clock = clock;
    }

    // Returns the user's bookings, newest first, optionally filtered by status.
    public List<Booking> getBookingHistory(int userId, BookingStatus status, int page, int size) {
        Pageable pageable = PageRequests.of(page, size);
        return status == null
                ? bookingRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : bookingRepo.findByUserIdAndBookingStatusOrderByCreatedAtDesc(userId, status, pageable);
    }

    // Returns one of the user's bookings, or throws NotFoundException or UnauthorizedException.
    public Booking getBooking(int userId, String confirmationId) {
        Booking booking = bookingRepo.findById(confirmationId)
                .orElseThrow(() -> NotFoundException.of("Booking", confirmationId));
        if (booking.getUserId() != userId) {
            throw new UnauthorizedException(Constants.Messages.NOT_BOOKING_OWNER);
        }
        return booking;
    }

    public record RefundPreview(BigDecimal refundAmount, String policyName) {
    }

    // Returns the refund the user would get, and under which policy, if they cancelled their CONFIRMED booking right now.
    public RefundPreview previewRefund(int userId, String confirmationId) {
        Booking booking = getBooking(userId, confirmationId);
        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidStateException("Only CONFIRMED bookings can be cancelled");
        }
        return new RefundPreview(refundPolicy.calculateRefund(booking, LocalDateTime.now(clock)), booking.getRefundPolicyName());
    }
}
