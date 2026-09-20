package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import org.springframework.stereotype.Component;

@Component
public class PremiumPricing extends MultiplierPricing {

    public PremiumPricing() {
        super(Constants.Pricing.PREMIUM_MULTIPLIER);
    }
}
