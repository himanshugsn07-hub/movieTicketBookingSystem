package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class TimeBasedRefundPolicy implements RefundPolicy {

    private Duration fullRefundWindow = Duration.ofHours(24);

    // Returns the full amount if cancelled at least fullRefundWindow before the show, otherwise zero.
    @Override
    public BigDecimal calculateRefund(Booking booking, LocalDateTime now) {
        LocalDateTime cutoff = booking.getShow().getStartTime().minus(fullRefundWindow);
        return now.isAfter(cutoff) ? BigDecimal.ZERO : booking.getAmount();
    }
}
