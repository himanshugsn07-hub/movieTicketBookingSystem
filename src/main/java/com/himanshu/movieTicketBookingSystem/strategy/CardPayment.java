package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CardPayment implements PaymentStrategy {

    // Charges the booking amount via card and returns the payment.
    @Override
    public Payment pay(Booking booking) {
        return new Payment(UUID.randomUUID().toString(), booking, PaymentType.CARD, booking.getAmount(), PaymentStatus.SUCCESS);
    }
}
