package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import org.springframework.stereotype.Component;

@Component
public class PricingStrategyFactory {

    // Returns the pricing strategy for the given tier.
    public PricingStrategy forTier(PricingTier pricingTier) {
        throw new UnsupportedOperationException("TODO");
    }
}
