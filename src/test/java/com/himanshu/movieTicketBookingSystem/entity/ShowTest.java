package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ShowTest {

    @Test
    void hasStartedIsTrueFromTheStartInstantOn() {
        LocalDateTime start = TestData.NOW;
        Show show = TestData.show(start);

        assertThat(show.hasStarted(start.minusSeconds(1))).isFalse();
        assertThat(show.hasStarted(start)).isTrue();
        assertThat(show.hasStarted(start.plusMinutes(1))).isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void generateSeatsCreatesOneLabelledSeatPerRowAndColumn() {
        Show show = TestData.show(TestData.NOW.plusDays(1));

        show.generateSeats(2, 3);

        List<Seat> seats = (List<Seat>) ReflectionTestUtils.getField(show, "seats");
        assertThat(seats).extracting(Seat::getLabel).containsExactly("A1", "A2", "A3", "B1", "B2", "B3");
        assertThat(seats).allMatch(Seat::isAvailable);
    }
}
