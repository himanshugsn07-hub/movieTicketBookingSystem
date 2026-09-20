package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Payment {
    @Id
    private String id;

    @OneToOne(mappedBy = "payment")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    protected Payment() {
    }

    public Payment(String id, Booking booking, PaymentType paymentType, BigDecimal amount, PaymentStatus status) {
        this.id = id;
        this.booking = booking;
        this.paymentType = paymentType;
        this.amount = amount;
        this.status = status;
    }

    // Returns the payment status.
    public PaymentStatus getStatus() {
        return status;
    }

    // Sets the payment status to REFUNDED.
    public void markRefunded() {
        status = PaymentStatus.REFUNDED;
    }
}
