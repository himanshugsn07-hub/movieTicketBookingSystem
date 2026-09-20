package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCancelShowIT extends IntegrationTest {

    @Test
    void cancellingAShowRefundsPaidBookingsInFullEvenInsideTheRefundWindowAndReleasesEverything() throws Exception {
        // the show starts in 2 hours, so the Standard policy alone would refund nothing
        Catalog c = createCatalog(1, 6, inHours(2));
        postAs(ADMIN, "/api/admin/discount-codes", "{\"code\":\"FLAT50\",\"type\":\"FLAT\",\"value\":50,\"validFrom\":\"%s\",\"validTo\":\"%s\"}"
                .formatted(inDays(-1), inDays(5))).andExpect(status().isCreated());
        List<Integer> seats = seatIds(c.showId());

        String discounted = holdOk(ALICE, c.showId(), seats.subList(0, 2));
        confirm(ALICE, discounted, "FLAT50").andExpect(status().isOk());       // paid 150
        String bobs = holdOk(BOB, c.showId(), seats.subList(2, 3));
        confirm(BOB, bobs, null).andExpect(status().isOk());                    // paid 100
        String held = holdOk(ALICE, c.showId(), seats.subList(3, 4));           // still only a hold
        getAs(ALICE, "/api/bookings/{id}/refund-preview", discounted).andExpect(jsonPath("$.refundAmount").value(0.0));

        postAs(ADMIN, "/api/admin/shows/" + c.showId() + "/cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundedBookings").value(2))
                .andExpect(jsonPath("$.expiredBookings").value(1))
                .andExpect(jsonPath("$.totalRefunded").value(250.00));

        getAs(ALICE, "/api/bookings/{id}", discounted)
                .andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.refundAmount").value(150.00));
        getAs(BOB, "/api/bookings/{id}", bobs).andExpect(jsonPath("$.refundAmount").value(100.00));
        assertThat(bookingStatus(held)).isEqualTo("EXPIRED");
        assertThat(jdbc.queryForList("select distinct status from payment", String.class)).containsExactly("REFUNDED");
        assertThat(jdbc.queryForList("select distinct status from seat where show_id = ?", String.class, c.showId()))
                .containsExactly("AVAILABLE");

        // customers can no longer find or book it
        getAs(ALICE, "/api/shows").andExpect(jsonPath("$", hasSize(0)));
        getAs(ALICE, "/api/shows/{id}", c.showId()).andExpect(jsonPath("$.cancelled").value(true));
        getAs(ALICE, "/api/shows/{id}/seats/available", c.showId()).andExpect(jsonPath("$", hasSize(0)));
        hold(ALICE, c.showId(), seats.subList(0, 1)).andExpect(status().isConflict());
        postAs(ALICE, "/api/bookings/" + discounted + "/cancel").andExpect(status().isConflict());

        // and both affected customers are told
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(jdbc.queryForList("select distinct status from notification where type = 'CANCELLATION'", String.class))
                        .containsExactly("SENT"));
        assertThat(jdbc.queryForObject("select count(*) from notification where type = 'CANCELLATION'", Integer.class)).isEqualTo(3);
    }

    @Test
    void aShowThatIsMissingAlreadyCancelledOrStartedCannotBeCancelled() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));

        postAs(ADMIN, "/api/admin/shows/9999/cancel").andExpect(status().isNotFound());

        postAs(ADMIN, "/api/admin/shows/" + c.showId() + "/cancel").andExpect(status().isOk());
        postAs(ADMIN, "/api/admin/shows/" + c.showId() + "/cancel").andExpect(status().isConflict());

        int other = createShow(c.screenId(), "m1", inDays(4), 100);
        jdbc.update("update show set start_time = now() - interval '1 minute' where id = ?", other);
        postAs(ADMIN, "/api/admin/shows/" + other + "/cancel").andExpect(status().isConflict());
    }
}
