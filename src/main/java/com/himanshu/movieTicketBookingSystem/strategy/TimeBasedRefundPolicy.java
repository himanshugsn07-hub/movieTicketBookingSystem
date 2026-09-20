package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class TimeBasedRefundPolicy implements RefundPolicy {

    // Used for bookings created before refund policies were configurable: full refund 24h or more ahead.
    private static final String LEGACY_TIERS = "24:100,0:0";

    // Applies the booking's snapshotted refund tiers to the amount paid, based on the notice given before the show.
    @Override
    public BigDecimal calculateRefund(Booking booking, LocalDateTime now) {
        String snapshot = booking.getRefundTiers() != null ? booking.getRefundTiers() : LEGACY_TIERS;
        long minutesBeforeShow = Duration.between(now, booking.getShow().getStartTime()).toMinutes();
        int percent = RefundTier.parseAll(snapshot).stream()
                .filter(t -> t.hoursBeforeShow() * 60L <= minutesBeforeShow)
                .max(Comparator.comparingInt(RefundTier::hoursBeforeShow))
                .map(RefundTier::refundPercent)
                .orElse(0);
        return booking.getPayableAmount().multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
