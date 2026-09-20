package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    @Autowired
    private CardPayment cardPayment;

    @Autowired
    private UpiPayment upiPayment;

    // Returns the payment strategy for the given payment type.
    public PaymentStrategy forPayment(PaymentType paymentType) {
        return switch (paymentType) {
            case CARD -> cardPayment;
            case UPI -> upiPayment;
        };
    }
}
