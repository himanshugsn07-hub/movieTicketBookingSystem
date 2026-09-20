package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBasedRefundPolicyTest {

    private static final LocalDateTime SHOW_START = TestData.NOW.plusDays(10);

    private final TimeBasedRefundPolicy policy = new TimeBasedRefundPolicy();
    private final Show show = TestData.show(SHOW_START);

    // A booking of one seat (100.00) that snapshotted a 48h:100, 12h:50, 0h:0 policy.
    private Booking flexibleBooking() {
        Booking booking = new Booking("b1", 7, show, List.of(new Seat(show, "A1")), TestData.SEAT_PRICE, TestData.NOW,
                TestData.NOW.plusMinutes(5), new RefundPolicyConfig("Flexible",
                List.of(new RefundTier(48, 100), new RefundTier(12, 50), new RefundTier(0, 0)), false));
        return booking;
    }

    private BigDecimal refundHoursBefore(Booking booking, long hours, long minutes) {
        return policy.calculateRefund(booking, SHOW_START.minusHours(hours).minusMinutes(minutes));
    }

    @Test
    void picksTheTierWithTheLargestHoursNotExceedingTheNotice() {
        Booking booking = flexibleBooking();

        assertThat(refundHoursBefore(booking, 100, 0)).isEqualByComparingTo("100.00");
        assertThat(refundHoursBefore(booking, 48, 0)).as("exactly on the boundary").isEqualByComparingTo("100.00");
        assertThat(refundHoursBefore(booking, 47, 59)).isEqualByComparingTo("50.00");
        assertThat(refundHoursBefore(booking, 12, 0)).isEqualByComparingTo("50.00");
        assertThat(refundHoursBefore(booking, 11, 59)).isEqualByComparingTo("0.00");
    }

    @Test
    void refundsAPercentageOfWhatWasActuallyPaidAfterDiscount() {
        Booking booking = flexibleBooking();
        booking.applyDiscount("SAVE20", new BigDecimal("20.00"));

        // paid 80.00, 50% tier
        assertThat(refundHoursBefore(booking, 24, 0)).isEqualByComparingTo("40.00");
    }

    @Test
    void roundsTheRefundHalfUpToTwoDecimals() {
        Booking booking = flexibleBooking();
        booking.applyDiscount("ODD", new BigDecimal("66.67"));

        // paid 33.33 x 50% = 16.665
        assertThat(refundHoursBefore(booking, 24, 0)).isEqualByComparingTo("16.67");
    }

    @Test
    void bookingsWithoutASnapshotUseTheLegacyFullRefundBeforeTwentyFourHours() {
        Booking booking = flexibleBooking();
        ReflectionTestUtils.setField(booking, "refundTiers", null);

        assertThat(refundHoursBefore(booking, 24, 0)).isEqualByComparingTo("100.00");
        assertThat(refundHoursBefore(booking, 23, 59)).isEqualByComparingTo("0.00");
    }
}
