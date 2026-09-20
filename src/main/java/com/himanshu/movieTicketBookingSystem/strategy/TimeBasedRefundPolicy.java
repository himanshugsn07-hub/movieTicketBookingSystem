package com.himanshu.movieTicketBookingSystem.strategy;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class TimeBasedRefundPolicy implements RefundPolicy {

    private Duration fullRefundWindow = Duration.ofHours(24);

    // Returns the full amount if cancelled within the refund window, otherwise a reduced or zero refund.
    @Override
    public BigDecimal calculateRefund(Booking booking, LocalDateTime now) {
        throw new UnsupportedOperationException("TODO");
    }
}
