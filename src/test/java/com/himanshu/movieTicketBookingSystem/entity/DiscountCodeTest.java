package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class DiscountCodeTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 15, 10, 0);

    private DiscountCode code(Integer maxUses, boolean active) {
        return new DiscountCode("SAVE", DiscountType.PERCENT, BigDecimal.TEN, NOW.minusDays(1), NOW.plusDays(1), maxUses, active);
    }

    @Test
    void isRedeemableOnlyWhenActiveInWindowAndNotFullyUsed() {
        assertThat(code(null, true).isRedeemable(NOW)).isTrue();
        assertThat(code(null, false).isRedeemable(NOW)).as("inactive").isFalse();
        assertThat(code(null, true).isRedeemable(NOW.minusDays(2))).as("before window").isFalse();
        assertThat(code(null, true).isRedeemable(NOW.plusDays(2))).as("after window").isFalse();

        DiscountCode limited = code(1, true);
        assertThat(limited.isRedeemable(NOW)).isTrue();
        limited.recordUse();
        assertThat(limited.isRedeemable(NOW)).as("single use spent").isFalse();
    }

    @Test
    void percentDiscountIsRoundedHalfUp() {
        DiscountCode twelvePointFive = new DiscountCode("P", DiscountType.PERCENT, new BigDecimal("12.5"),
                NOW, NOW.plusDays(1), null, true);

        assertThat(twelvePointFive.calculateDiscount(new BigDecimal("99.99"))).isEqualByComparingTo("12.50");
        assertThat(twelvePointFive.calculateDiscount(new BigDecimal("0.04"))).isEqualByComparingTo("0.01");
    }

    @Test
    void flatDiscountNeverExceedsTheAmount() {
        DiscountCode flat = new DiscountCode("F", DiscountType.FLAT, new BigDecimal("50"), NOW, NOW.plusDays(1), null, true);

        assertThat(flat.calculateDiscount(new BigDecimal("200.00"))).isEqualByComparingTo("50.00");
        assertThat(flat.calculateDiscount(new BigDecimal("30.00"))).isEqualByComparingTo("30.00");
    }
}
