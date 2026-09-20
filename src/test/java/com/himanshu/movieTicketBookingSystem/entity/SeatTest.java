package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeatTest {

    private final Seat seat = new Seat(TestData.show(TestData.NOW.plusDays(1)), "A1");

    @Test
    void onlyOneReserveSucceedsUntilTheSeatIsReleased() {
        assertThat(seat.reserve()).isTrue();
        assertThat(seat.reserve()).as("already reserved").isFalse();

        seat.release();

        assertThat(seat.isAvailable()).isTrue();
        assertThat(seat.reserve()).isTrue();
    }

    @Test
    void bookedSeatCannotBeReserved() {
        seat.reserve();
        seat.book();

        assertThat(seat.getStatus()).isEqualTo(SeatStatus.BOOKED);
        assertThat(seat.reserve()).isFalse();
    }
}
