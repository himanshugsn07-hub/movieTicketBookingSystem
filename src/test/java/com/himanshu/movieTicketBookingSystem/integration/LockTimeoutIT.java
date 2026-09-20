package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** A request that cannot get a row lock in time (1s in the test profile) must fail fast with a retryable 503. */
class LockTimeoutIT extends IntegrationTest {

    @Autowired DataSource dataSource;

    // Opens a transaction on its own connection that keeps the row locked until it is rolled back.
    private Connection lockRow(String selectForUpdate) throws Exception {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute(selectForUpdate);
        }
        return connection;
    }

    @Test
    void holdingASeatThatIsLockedElsewhereTimesOutWith503AndWorksOnceTheLockIsFree() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        List<Integer> seat = seatIds(c.showId()).subList(0, 1);

        try (Connection blocker = lockRow("select id from seat where id = " + seat.get(0) + " for update")) {
            hold(ALICE, c.showId(), seat)
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(header().string("Retry-After", "1"))
                    .andExpect(jsonPath("$.status").value(503));
            blocker.rollback();
        }

        assertThat(jdbc.queryForObject("select count(*) from booking", Integer.class)).as("nothing half-created").isZero();
        hold(ALICE, c.showId(), seat).andExpect(status().isCreated());
    }

    @Test
    void confirmingABookingThatIsLockedElsewhereTimesOutWith503AndSucceedsAfterwards() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        try (Connection blocker = lockRow("select confirmation_id from booking where confirmation_id = '" + id + "' for update")) {
            confirm(ALICE, id, null).andExpect(status().isServiceUnavailable());
            blocker.rollback();
        }

        assertThat(bookingStatus(id)).isEqualTo("CREATED");
        confirm(ALICE, id, null).andExpect(status().isOk());
    }
}
