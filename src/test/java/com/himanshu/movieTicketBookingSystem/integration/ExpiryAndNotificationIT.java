package com.himanshu.movieTicketBookingSystem.integration;

import com.himanshu.movieTicketBookingSystem.notification.NotificationDispatcher;
import com.himanshu.movieTicketBookingSystem.service.BookingExpiryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpiryAndNotificationIT extends IntegrationTest {

    @Autowired BookingExpiryService expiryService;
    @Autowired NotificationDispatcher dispatcher;

    private void lapseHold(String confirmationId) {
        jdbc.update("update booking set expires_at = now() - interval '1 minute' where confirmation_id = ?", confirmationId);
    }

    private String notificationStatus(String bookingId, String type) {
        List<String> statuses = jdbc.queryForList("select status from notification where booking_id = ? and type = ?",
                String.class, bookingId, type);
        return statuses.isEmpty() ? null : statuses.get(0);
    }

    // ------------------------------------------------------------------ hold expiry

    @Test
    void aLapsedHoldFreesItsSeatForTheNextBookingImmediately() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        List<Integer> seat = seatIds(c.showId()).subList(0, 1);
        String alice = holdOk(ALICE, c.showId(), seat);

        hold(BOB, c.showId(), seat).andExpect(status().isConflict());        // still held
        lapseHold(alice);
        hold(BOB, c.showId(), seat).andExpect(status().isCreated());         // no wait for the 30s sweep

        assertThat(bookingStatus(alice)).isEqualTo("EXPIRED");
    }

    @Test
    void theScheduledSweepExpiresLapsedHoldsAndLeavesLiveOnesAlone() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(2));
        List<Integer> seats = seatIds(c.showId());
        String lapsed = holdOk(ALICE, c.showId(), seats.subList(0, 1));
        String live = holdOk(BOB, c.showId(), seats.subList(1, 2));
        lapseHold(lapsed);

        confirm(ALICE, lapsed, null).andExpect(status().isConflict());       // rejected even before the sweep runs
        expiryService.releaseExpiredBookings();

        assertThat(bookingStatus(lapsed)).isEqualTo("EXPIRED");
        assertThat(bookingStatus(live)).isEqualTo("CREATED");
        assertThat(jdbc.queryForObject("select status from seat where id = ?", String.class, seats.get(0))).isEqualTo("AVAILABLE");
        assertThat(jdbc.queryForObject("select status from seat where id = ?", String.class, seats.get(1))).isEqualTo("RESERVED");
    }

    // ------------------------------------------------------------------ notifications

    @Test
    void confirmingQueuesAConfirmationAndAReminderThatTheSchedulerDeliversWhenDue() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        confirm(ALICE, id, null).andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).until(() -> "SENT".equals(notificationStatus(id, "CONFIRMATION")));
        assertThat(notificationStatus(id, "REMINDER")).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("""
                select count(*) from notification n join booking b on b.confirmation_id = n.booking_id
                join show s on s.id = b.show_id
                where n.type = 'REMINDER' and n.send_at = s.start_time - interval '1 hour'""", Integer.class)).isEqualTo(1);

        dispatcher.deliverDue();
        assertThat(notificationStatus(id, "REMINDER")).as("not due yet").isEqualTo("PENDING");

        jdbc.update("update notification set send_at = now() - interval '1 minute' where type = 'REMINDER'");
        dispatcher.deliverDue();
        assertThat(notificationStatus(id, "REMINDER")).isEqualTo("SENT");

        getAs(ALICE, "/api/notifications").andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].type", containsInAnyOrder("CONFIRMATION", "REMINDER")));
        getAs(BOB, "/api/notifications").andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aShowStartingWithinAnHourGetsNoReminder() throws Exception {
        Catalog c = createCatalog(1, 2, inHours(0).plusMinutes(30));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        confirm(ALICE, id, null).andExpect(status().isOk());

        await().atMost(Duration.ofSeconds(5)).until(() -> "SENT".equals(notificationStatus(id, "CONFIRMATION")));
        assertThat(notificationStatus(id, "REMINDER")).isNull();
    }

    @Test
    void cancellingABookingWithdrawsItsReminderAndQueuesACancellationNotice() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));
        confirm(ALICE, id, null).andExpect(status().isOk());

        postAs(ALICE, "/api/bookings/" + id + "/cancel").andExpect(status().isOk());

        assertThat(notificationStatus(id, "REMINDER")).isEqualTo("CANCELLED");
        await().atMost(Duration.ofSeconds(5)).until(() -> "SENT".equals(notificationStatus(id, "CANCELLATION")));
        getAs(ALICE, "/api/notifications").andExpect(jsonPath("$[*].type", containsInAnyOrder("CONFIRMATION", "CANCELLATION")));
    }

    @Test
    void aRejectedConfirmationQueuesNoNotification() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        confirm(ALICE, id, "NOPE").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select count(*) from notification", Integer.class)).isZero();
    }

    @Test
    void notificationPagingIsValidated() throws Exception {
        getAs(ALICE, "/api/notifications?size=0").andExpect(status().isBadRequest());
        getAs(ALICE, "/api/notifications?page=-1").andExpect(status().isBadRequest());
    }
}
