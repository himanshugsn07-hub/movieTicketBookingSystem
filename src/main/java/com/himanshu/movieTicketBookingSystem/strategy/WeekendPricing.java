package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class WeekendPricing implements PricingStrategy {

    private static final BigDecimal MULTIPLIER = new BigDecimal("1.5");

    // Returns basePrice x seat count x 1.5.
    @Override
    public BigDecimal calculatePrice(Show show, List<Seat> seats) {
        return show.getBasePrice()
                .multiply(BigDecimal.valueOf(seats.size()))
                .multiply(MULTIPLIER)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
