package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import jakarta.persistence.*;
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

    // Moves booking CREATED -> CONFIRMED and attaches the payment.
    public void confirm(Payment payment) {
    }

    // Moves booking CREATED -> PAYMENT_FAILED.
    public void markPaymentFailed() {
    }

    // Moves booking CREATED -> EXPIRED.
    public void expire() {
    }

    // Moves booking CONFIRMED -> CANCELLED and records the refund amount.
    public void cancel(BigDecimal refundAmount) {
    }

    // Throws InvalidStateException if the current status is not the expected one.
    private void requireStatus(BookingStatus expected) {
    }
}
