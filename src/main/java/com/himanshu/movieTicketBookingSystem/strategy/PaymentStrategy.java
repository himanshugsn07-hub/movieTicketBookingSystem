package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Payment;

public interface PaymentStrategy {

    // Charges the booking amount and returns the resulting payment.
    Payment pay(Booking booking);
}
