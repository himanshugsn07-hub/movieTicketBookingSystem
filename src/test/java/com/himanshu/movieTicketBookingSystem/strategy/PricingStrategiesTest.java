package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategiesTest {

    private final Show show = TestData.show(TestData.NOW.plusDays(1));
    private final List<Seat> twoSeats = List.of(new Seat(show, "A1"), new Seat(show, "A2"));

    @Test
    void regularPricingIsBasePriceTimesSeatCount() {
        assertThat(new RegularPricing().calculatePrice(show, twoSeats)).isEqualByComparingTo("200.00");
    }

    @Test
    void premiumAndWeekendPricingAddFiftyPercent() {
        assertThat(new PremiumPricing().calculatePrice(show, twoSeats)).isEqualByComparingTo("300.00");
        assertThat(new WeekendPricing().calculatePrice(show, twoSeats)).isEqualByComparingTo("300.00");
    }

    @Test
    void multiplierPricingRoundsHalfUpToTwoDecimals() {
        show.reprice(new BigDecimal("33.33"), show.getPricingTier());

        // 33.33 x 1.5 = 49.995
        assertThat(new PremiumPricing().calculatePrice(show, List.of(new Seat(show, "A1")))).isEqualByComparingTo("50.00");
    }
}
