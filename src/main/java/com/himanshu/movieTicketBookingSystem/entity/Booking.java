package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import jakarta.persistence.*;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Booking {
    @Id
    private String confirmationId;

    private int userId;

    @ManyToOne
    private Show show;

    @ManyToMany
    private List<Seat> seats;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    private BigDecimal amount;
    private BigDecimal refundAmount;
    private LocalDateTime expiresAt;

    @OneToOne(cascade = CascadeType.ALL)
    private Payment payment;

    protected Booking() {
    }

    public Booking(String confirmationId, int userId, Show show, List<Seat> seats, BigDecimal amount,
                   LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.confirmationId = confirmationId;
        this.userId = userId;
        this.show = show;
        this.seats = seats;
        this.amount = amount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.bookingStatus = BookingStatus.CREATED;
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
