package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PricingStrategyFactory {

    @Autowired
    private RegularPricing regularPricing;

    @Autowired
    private PremiumPricing premiumPricing;

    @Autowired
    private WeekendPricing weekendPricing;

    // Returns the pricing strategy for the given tier.
    public PricingStrategy forTier(PricingTier pricingTier) {
        return switch (pricingTier) {
            case REGULAR -> regularPricing;
            case PREMIUM -> premiumPricing;
            case WEEKEND -> weekendPricing;
        };
    }
}
