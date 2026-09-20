package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PremiumPricing implements PricingStrategy {

    // Returns basePrice x seat count x 1.5.
    @Override
    public BigDecimal calculatePrice(Show show, List<Seat> seats) {
        throw new UnsupportedOperationException("TODO");
    }
}
