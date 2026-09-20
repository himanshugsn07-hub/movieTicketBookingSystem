package com.himanshu.movieTicketBookingSystem.integration;

import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.notification.NotificationDispatcher;
import com.himanshu.movieTicketBookingSystem.notification.NotificationSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/** Notifications are sent with no database transaction (row lock, connection) held open. */
@Import(NotificationDeliveryIT.BlockingSenderConfig.class)
class NotificationDeliveryIT extends IntegrationTest {

    // A provider that stays "busy" until the test lets it finish.
    static class BlockingSender implements NotificationSender {
        volatile CountDownLatch entered = new CountDownLatch(1);
        volatile CountDownLatch release = new CountDownLatch(1);

        void reset() {
            entered = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        @Override
        public void send(Notification notification) {
            entered.countDown();
            try {
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("test never released the sender");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @TestConfiguration
    static class BlockingSenderConfig {
        @Bean
        @Primary
        BlockingSender blockingSender() {
            return new BlockingSender();
        }
    }

    @Autowired BlockingSender sender;
    @Autowired NotificationDispatcher dispatcher;

    @BeforeEach
    void resetSender() {
        sender.reset();
    }

    private String status(String type) {
        return jdbc.queryForObject("select status from notification where type = ?", String.class, type);
    }

    @Test
    void aSlowProviderHoldsNoRowLockWhileItSendsAndTheOutcomeIsRecordedAfterwards() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        confirm(ALICE, id, null);
        assertThat(sender.entered.await(5, TimeUnit.SECONDS)).as("delivery started").isTrue();

        // mid-send: the row is marked SENDING and is NOT locked (NOWAIT would fail immediately if it were)
        assertThat(jdbc.queryForObject("select status from notification where type = 'CONFIRMATION' for update nowait", String.class))
                .isEqualTo("SENDING");

        sender.release.countDown();
        await().atMost(Duration.ofSeconds(5)).until(() -> "SENT".equals(status("CONFIRMATION")));
    }

    @Test
    void aNotificationClaimedByAWorkerThatDiedIsSentByTheSweepButAFreshClaimIsLeftAlone() {
        sender.release.countDown();
        String insert = "insert into notification(user_id, booking_id, type, message, status, attempts, created_at, send_at, claimed_at) "
                + "values (2, '%s', 'CONFIRMATION', 'hello', 'SENDING', 0, now(), now(), %s)";
        jdbc.update(insert.formatted("dead-worker", "now() - interval '10 minutes'"));
        jdbc.update(insert.formatted("busy-worker", "now()"));

        dispatcher.deliverDue();

        assertThat(jdbc.queryForObject("select status from notification where booking_id = 'dead-worker'", String.class)).isEqualTo("SENT");
        assertThat(jdbc.queryForObject("select status from notification where booking_id = 'busy-worker'", String.class)).isEqualTo("SENDING");
    }
}
