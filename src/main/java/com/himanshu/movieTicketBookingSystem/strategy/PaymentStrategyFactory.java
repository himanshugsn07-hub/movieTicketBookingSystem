package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    // Returns the payment strategy for the given payment type.
    public PaymentStrategy forPayment(PaymentType paymentType) {
        throw new UnsupportedOperationException("TODO");
    }
}
