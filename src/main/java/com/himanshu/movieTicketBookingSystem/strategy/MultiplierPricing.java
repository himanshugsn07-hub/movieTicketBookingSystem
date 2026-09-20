package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Base for pricing tiers that charge the regular price times a fixed multiplier. */
public abstract class MultiplierPricing implements PricingStrategy {

    private final BigDecimal multiplier;

    protected MultiplierPricing(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    // Returns basePrice x seat count x multiplier, rounded half-up to 2 decimals.
    @Override
    public BigDecimal calculatePrice(Show show, List<Seat> seats) {
        return show.getBasePrice()
                .multiply(BigDecimal.valueOf(seats.size()))
                .multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
