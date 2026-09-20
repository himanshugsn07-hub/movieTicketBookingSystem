package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class CardPayment implements PaymentStrategy {

    // Charges the booking amount via card and returns the payment.
    @Override
    public Payment pay(Booking booking) {
        throw new UnsupportedOperationException("TODO");
    }
}
