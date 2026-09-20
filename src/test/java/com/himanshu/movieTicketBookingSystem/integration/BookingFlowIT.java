package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingFlowIT extends IntegrationTest {

    @Test
    void customersBrowseCitiesTheatresAndFilterUpcomingShows() throws Exception {
        Catalog pune = createCatalog(1, 2, inDays(2));
        int mumbai = read(postAs(ADMIN, "/api/admin/cities", "{\"name\":\"Mumbai\"}"), "$.id");
        int inox = read(postAs(ADMIN, "/api/admin/theatres", "{\"name\":\"INOX\",\"cityId\":%d}".formatted(mumbai)), "$.id");
        postAs(ADMIN, "/api/admin/movies", "{\"id\":\"m2\",\"title\":\"Dune\",\"language\":\"EN\",\"durationMin\":150,\"genre\":\"SciFi\"}");
        createShow(createScreen(inox, "S1", 1, 2), "m2", inDays(3), 120);

        getAs(ALICE, "/api/cities").andExpect(jsonPath("$[*].name", contains("Mumbai", "Pune")));
        getAs(ALICE, "/api/cities/{id}/theatres", pune.cityId()).andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/cities/9999/theatres").andExpect(status().isNotFound());

        getAs(ALICE, "/api/shows").andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].movieTitle").value("Inception"));
        getAs(ALICE, "/api/shows?cityId={id}", mumbai).andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/shows?movieId=m2").andExpect(jsonPath("$[0].theatreName").value("INOX"));
        getAs(ALICE, "/api/shows?date={d}", inDays(2).toLocalDate()).andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/shows?size=1&page=1").andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/shows?size=500").andExpect(status().isBadRequest());
        getAs(ALICE, "/api/shows/{id}", pune.showId()).andExpect(jsonPath("$.availableSeats").value(2));
    }

    @Test
    void movieSearchIsCaseInsensitiveAndPerCity() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));

        getAs(ALICE, "/api/movies?title=incep&cityId={id}", c.cityId()).andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/movies?title=INCEPTION&cityId={id}", c.cityId()).andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/movies?title=incep&cityId=9999").andExpect(jsonPath("$", hasSize(0)));
        getAs(ALICE, "/api/movies?title=&cityId={id}", c.cityId()).andExpect(status().isBadRequest());
    }

    @Test
    void aBookingGoesFromHoldToConfirmedToCancelledAndFreesItsSeats() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(5));
        List<Integer> seats = seatIds(c.showId()).subList(0, 2);

        String id = read(hold(ALICE, c.showId(), seats).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.movieTitle").value("Inception")), "$.confirmationId");
        getAs(ALICE, "/api/shows/{id}/seats/available", c.showId()).andExpect(jsonPath("$", hasSize(2)));

        hold(BOB, c.showId(), seats).andExpect(status().isConflict());
        confirm(BOB, id, null).andExpect(status().isForbidden());

        confirm(ALICE, id, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"));
        confirm(ALICE, id, null).andExpect(status().isConflict());

        getAs(ALICE, "/api/bookings/{id}/refund-preview", id)
                .andExpect(jsonPath("$.refundAmount").value(200.00))
                .andExpect(jsonPath("$.policyName").value("Standard"));
        postAs(ALICE, "/api/bookings/" + id + "/cancel").andExpect(status().isOk()).andExpect(jsonPath("$.cancelled").value(true));

        getAs(ALICE, "/api/bookings/{id}", id).andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(200.00));
        getAs(ALICE, "/api/shows/{id}/seats/available", c.showId()).andExpect(jsonPath("$", hasSize(4)));
        postAs(ALICE, "/api/bookings/" + id + "/cancel").andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select status from payment", String.class)).isEqualTo("REFUNDED");
    }

    @Test
    void invalidBookingRequestsAreRejected() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        int seat = seatIds(c.showId()).get(0);

        postAs(ALICE, "/api/bookings", "{\"showId\":%d,\"seatIds\":[]}".formatted(c.showId())).andExpect(status().isBadRequest());
        postAs(ALICE, "/api/bookings", "{\"showId\":%d,\"seatIds\":[null]}".formatted(c.showId())).andExpect(status().isBadRequest());
        postAs(ALICE, "/api/bookings", "{\"showId\":%d,\"seatIds\":[%d,%d]}".formatted(c.showId(), seat, seat)).andExpect(status().isBadRequest());
        postAs(ALICE, "/api/bookings", "{\"showId\":null,\"seatIds\":[1]}").andExpect(status().isBadRequest());
        hold(ALICE, 9999, List.of(seat)).andExpect(status().isNotFound());
        hold(ALICE, c.showId(), List.of(987654)).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from booking", Integer.class)).isZero();
    }

    @Test
    void aShowThatHasAlreadyStartedCannotBeBookedOrConfirmed() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        jdbc.update("update show set start_time = now() - interval '1 minute' where id = ?", c.showId());

        hold(BOB, c.showId(), seatIds(c.showId()).subList(1, 2)).andExpect(status().isConflict());
        confirm(ALICE, id, null).andExpect(status().isConflict());
        assertThat(bookingStatus(id)).isEqualTo("CREATED");
    }

    @Test
    void historyShowsOnlyMyBookingsNewestFirstWithStatusFilterPagingAndOwnership() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(5));
        List<Integer> seats = seatIds(c.showId());
        String first = holdOk(ALICE, c.showId(), seats.subList(0, 1));
        confirm(ALICE, first, null).andExpect(status().isOk());
        postAs(ALICE, "/api/bookings/" + first + "/cancel").andExpect(status().isOk());
        Thread.sleep(20);
        String second = holdOk(ALICE, c.showId(), seats.subList(1, 2));

        getAs(ALICE, "/api/bookings").andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].confirmationId").value(second))
                .andExpect(jsonPath("$[1].confirmationId").value(first));
        getAs(ALICE, "/api/bookings?status=CANCELLED").andExpect(jsonPath("$", hasSize(1)));
        getAs(ALICE, "/api/bookings?size=1&page=1").andExpect(jsonPath("$[0].confirmationId").value(first));
        getAs(ALICE, "/api/bookings?size=0").andExpect(status().isBadRequest());
        getAs(ALICE, "/api/bookings?status=NOPE").andExpect(status().isBadRequest());

        getAs(BOB, "/api/bookings").andExpect(jsonPath("$", hasSize(0)));
        getAs(BOB, "/api/bookings/{id}", first).andExpect(status().isForbidden());
        getAs(ALICE, "/api/bookings/nope").andExpect(status().isNotFound());
    }
}
