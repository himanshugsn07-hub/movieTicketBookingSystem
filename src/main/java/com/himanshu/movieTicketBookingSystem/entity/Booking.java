package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import jakarta.persistence.*;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(indexes = @Index(name = "idx_booking_status_expires", columnList = "booking_status, expires_at"))
public class Booking {
    @Id
    private String confirmationId;

    private int userId;

    @ManyToOne
    private Show show;

    @ManyToMany(fetch = FetchType.EAGER)
    private List<Seat> seats;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    private BigDecimal amount;
    private BigDecimal refundAmount;

    // Refund policy captured when the booking was created, so later policy edits do not affect it.
    private String refundPolicyName;
    private String refundTiers;

    private String discountCode;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private LocalDateTime expiresAt;

    @OneToOne(cascade = CascadeType.ALL)
    private Payment payment;

    protected Booking() {
    }

    public Booking(String confirmationId, int userId, Show show, List<Seat> seats, BigDecimal amount,
                   LocalDateTime createdAt, LocalDateTime expiresAt, RefundPolicyConfig refundPolicy) {
        this.confirmationId = confirmationId;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.amount = amount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.bookingStatus = BookingStatus.CREATED;
        this.refundPolicyName = refundPolicy.getName();
        this.refundTiers = refundPolicy.toSnapshot();
    }

    // Returns the name of the refund policy snapshotted on this booking.
    public String getRefundPolicyName() {
        return refundPolicyName;
    }

    // Returns the snapshotted refund tiers such as "24:100,0:0", or null for bookings that predate policies.
    public String getRefundTiers() {
        return refundTiers;
    }

    // Returns the discount code used, or null.
    public String getDiscountCode() {
        return discountCode;
    }

    // Returns the discount applied to the amount (zero if none).
    public BigDecimal getDiscountAmount() {
        return discountAmount == null ? BigDecimal.ZERO : discountAmount;
    }

    // Returns the amount to pay: the price minus any discount.
    public BigDecimal getPayableAmount() {
        return amount.subtract(getDiscountAmount());
    }

    // Applies a discount to a CREATED booking.
    public void applyDiscount(String code, BigDecimal discountAmount) {
        requireStatus(BookingStatus.CREATED);
        this.discountCode = code;
        this.discountAmount = discountAmount;
    }

    // Removes any applied discount.
    public void clearDiscount() {
        this.discountCode = null;
        this.discountAmount = BigDecimal.ZERO;
    }

    // Returns the refund amount, or null if not cancelled.
    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    // Returns when the booking was created.
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Returns the booking's confirmation id.
    public String getConfirmationId() {
        return confirmationId;
    }

    // Returns the current booking status.
    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

    // Returns the id of the user who owns this booking.
    public int getUserId() {
        return userId;
    }

    // Returns the seats held by this booking.
    public List<Seat> getSeats() {
        return seats;
    }

    // Returns the time the hold on the seats ends.
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    // Returns the payment attached to this booking, or null before confirmation.
    public Payment getPayment() {
        return payment;
    }

    // Returns the booking amount.
    public BigDecimal getAmount() {
        return amount;
    }

    // Returns the show this booking is for.
    public Show getShow() {
        return show;
    }

    // Moves booking CREATED -> CONFIRMED and attaches the payment.
    public void confirm(Payment payment) {
        requireStatus(BookingStatus.CREATED);
        this.payment = payment;
        this.bookingStatus = BookingStatus.CONFIRMED;
    }

    // Moves booking CREATED -> PAYMENT_FAILED.
    public void markPaymentFailed() {
        requireStatus(BookingStatus.CREATED);
        this.bookingStatus = BookingStatus.PAYMENT_FAILED;
    }

    // Moves booking CREATED -> EXPIRED.
    public void expire() {
        requireStatus(BookingStatus.CREATED);
        this.bookingStatus = BookingStatus.EXPIRED;
    }

    // Moves booking CONFIRMED -> CANCELLED and records the refund amount.
    public void cancel(BigDecimal refundAmount) {
        requireStatus(BookingStatus.CONFIRMED);
        this.refundAmount = refundAmount;
        this.bookingStatus = BookingStatus.CANCELLED;
    }

    // Throws InvalidStateException if the current status is not the expected one.
    private void requireStatus(BookingStatus expected) {
        if (bookingStatus != expected) {
            throw new InvalidStateException("Booking must be " + expected + " but was " + bookingStatus);
        }
    }
}
