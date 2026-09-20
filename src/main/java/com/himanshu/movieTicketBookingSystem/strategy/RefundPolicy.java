package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface RefundPolicy {

    // Calculates the refund amount for cancelling the booking at the given time.
    BigDecimal calculateRefund(Booking booking, LocalDateTime now);
}
