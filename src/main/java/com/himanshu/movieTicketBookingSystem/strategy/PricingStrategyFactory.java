package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import org.springframework.stereotype.Component;

@Component
public class PricingStrategyFactory {

    private final RegularPricing regularPricing;
    private final PremiumPricing premiumPricing;
    private final WeekendPricing weekendPricing;

    public PricingStrategyFactory(RegularPricing regularPricing, PremiumPricing premiumPricing, WeekendPricing weekendPricing) {
        this.regularPricing = regularPricing;
        this.premiumPricing = premiumPricing;
        this.weekendPricing = weekendPricing;
    }

    // Returns the pricing strategy for the given tier.
    public PricingStrategy forTier(PricingTier pricingTier) {
        return switch (pricingTier) {
            case REGULAR -> regularPricing;
            case PREMIUM -> premiumPricing;
            case WEEKEND -> weekendPricing;
        };
    }
}
