package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;

import java.math.BigDecimal;
import java.util.List;

public interface PricingStrategy {

    // Calculates the total price for the seats in the show.
    BigDecimal calculatePrice(Show show, List<Seat> seats);
}
