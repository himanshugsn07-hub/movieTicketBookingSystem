package com.himanshu.movieTicketBookingSystem.support;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/** Small builders for entities used across the unit tests, plus a fixed clock. */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 15, 10, 0);
    public static final Clock CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
    public static final BigDecimal SEAT_PRICE = new BigDecimal("100.00");

    private TestData() {
    }

    public static Movie movie() {
        return new Movie("m1", "Inception", "EN", 120, "SciFi");
    }

    // A REGULAR show at 100.00 per seat that starts at the given time and lasts two hours.
    public static Show show(LocalDateTime start) {
        return new Show(new Screen("S1", null, 2, 3), movie(), SEAT_PRICE, PricingTier.REGULAR, start, start.plusHours(2));
    }

    // The "Standard" policy: full refund 24h or more ahead, nothing after.
    public static RefundPolicyConfig standardPolicy() {
        return new RefundPolicyConfig("Standard", List.of(new RefundTier(24, 100), new RefundTier(0, 0)), true);
    }

    public static Seat seat(Show show, String label) {
        return new Seat(show, label);
    }

    // A CREATED booking (id "b1", user 7) for the seats, priced at 100.00 per seat, held until the given time.
    public static Booking booking(Show show, List<Seat> seats, LocalDateTime expiresAt) {
        return new Booking("b1", 7, show, seats, SEAT_PRICE.multiply(BigDecimal.valueOf(seats.size())),
                NOW, expiresAt, standardPolicy());
    }

    // A CREATED booking whose hold is still valid.
    public static Booking booking(Show show, List<Seat> seats) {
        return booking(show, seats, NOW.plusMinutes(5));
    }
}
