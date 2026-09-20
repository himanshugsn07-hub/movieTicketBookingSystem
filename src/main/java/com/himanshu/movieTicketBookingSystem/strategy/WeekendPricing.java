package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import org.springframework.stereotype.Component;

@Component
public class WeekendPricing extends MultiplierPricing {

    public WeekendPricing() {
        super(Constants.Pricing.WEEKEND_MULTIPLIER);
    }
}
