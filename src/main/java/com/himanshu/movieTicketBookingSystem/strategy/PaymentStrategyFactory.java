package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import org.springframework.stereotype.Component;

@Component
public class PaymentStrategyFactory {

    private final CardPayment cardPayment;
    private final UpiPayment upiPayment;

    public PaymentStrategyFactory(CardPayment cardPayment, UpiPayment upiPayment) {
        this.cardPayment = cardPayment;
        this.upiPayment = upiPayment;
    }

    // Returns the payment strategy for the given payment type.
    public PaymentStrategy forPayment(PaymentType paymentType) {
        return switch (paymentType) {
            case CARD -> cardPayment;
            case UPI -> upiPayment;
        };
    }
}
