package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real parallel requests against the real database: the guarantees that stop double allocation. */
class ConcurrencyIT extends IntegrationTest {

    private static final int THREADS = 12;

    // Starts all tasks together behind a latch and returns their results in task order.
    private <T> List<T> race(int tasks, IntFunction<Callable<T>> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                Callable<T> work = task.apply(i);
                futures.add(pool.submit(() -> {
                    start.await();
                    return work.call();
                }));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static long count(List<Integer> statuses, int status) {
        return statuses.stream().filter(s -> s == status).count();
    }

    private int holdStatus(RequestPostProcessor user, int showId, List<Integer> seats) throws Exception {
        return hold(user, showId, seats).andReturn().getResponse().getStatus();
    }

    private int confirmStatus(RequestPostProcessor user, String id, String code) throws Exception {
        return confirm(user, id, code).andReturn().getResponse().getStatus();
    }

    private int cancelStatus(RequestPostProcessor user, String id) throws Exception {
        return postAs(user, "/api/bookings/" + id + "/cancel").andReturn().getResponse().getStatus();
    }

    @Test
    void whenManyUsersRaceForTheSameSeatExactlyOneGetsIt() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        List<Integer> seat = seatIds(c.showId()).subList(0, 1);

        List<Integer> statuses = race(THREADS, i -> () -> holdStatus(i % 2 == 0 ? ALICE : BOB, c.showId(), seat));

        assertThat(count(statuses, 201)).isEqualTo(1);
        assertThat(count(statuses, 409)).isEqualTo(THREADS - 1);
        assertThat(jdbc.queryForObject("select count(*) from booking where booking_status = 'CREATED'", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from seat where id = ?", String.class, seat.get(0))).isEqualTo("RESERVED");
    }

    @Test
    void overlappingSeatSetsNeitherDeadlockNorDoubleAllocateTheSharedSeat() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(2));
        List<Integer> seats = seatIds(c.showId());
        List<Integer> first = List.of(seats.get(0), seats.get(1));
        List<Integer> second = List.of(seats.get(2), seats.get(1));   // listed in the opposite order on purpose

        List<Integer> statuses = race(THREADS, i -> () -> holdStatus(ALICE, c.showId(), i % 2 == 0 ? first : second));

        assertThat(count(statuses, 201)).isEqualTo(1);
        assertThat(statuses).containsOnly(201, 409);
        assertThat(jdbc.queryForObject("select count(*) from booking", Integer.class)).isEqualTo(1);
    }

    @Test
    void parallelConfirmationsOfOneBookingChargeOnlyOnce() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        List<Integer> statuses = race(THREADS, i -> () -> confirmStatus(ALICE, id, null));

        assertThat(count(statuses, 200)).isEqualTo(1);
        assertThat(count(statuses, 409)).isEqualTo(THREADS - 1);
        assertThat(jdbc.queryForObject("select count(*) from payment", Integer.class)).isEqualTo(1);
        assertThat(bookingStatus(id)).isEqualTo("CONFIRMED");
    }

    @Test
    void parallelCancellationsOfOneBookingRefundOnlyOnce() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));
        confirm(ALICE, id, null).andExpect(status().isOk());

        List<Integer> statuses = race(THREADS, i -> () -> cancelStatus(ALICE, id));

        assertThat(count(statuses, 200)).isEqualTo(1);
        assertThat(count(statuses, 409)).isEqualTo(THREADS - 1);
        assertThat(jdbc.queryForList("select status from payment", String.class)).containsExactly("REFUNDED");
        assertThat(jdbc.queryForObject("select count(*) from notification where type = 'CANCELLATION'", Integer.class)).isEqualTo(1);
    }

    @Test
    void aSingleUseDiscountCodeIsRedeemedOnceEvenWhenBookingsAreConfirmedInParallel() throws Exception {
        Catalog c = createCatalog(1, 8, inDays(5));
        postAs(ADMIN, "/api/admin/discount-codes", "{\"code\":\"ONCE\",\"type\":\"FLAT\",\"value\":10,\"validFrom\":\"%s\",\"validTo\":\"%s\",\"maxUses\":1}"
                .formatted(inDays(-1), inDays(5))).andExpect(status().isCreated());
        List<Integer> seats = seatIds(c.showId());
        List<String> bookings = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            bookings.add(holdOk(ALICE, c.showId(), seats.subList(i, i + 1)));
        }

        List<Integer> statuses = race(bookings.size(), i -> () -> confirmStatus(ALICE, bookings.get(i), "ONCE"));

        assertThat(count(statuses, 200)).isEqualTo(1);
        assertThat(count(statuses, 400)).isEqualTo(bookings.size() - 1);
        assertThat(jdbc.queryForObject("select used_count from discount_code", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from booking where discount_code = 'ONCE'", Integer.class)).isEqualTo(1);
    }

    @Test
    void cancellingAShowWhileBookingsArriveLeavesNoLiveBookingOrHeldSeat() throws Exception {
        Catalog c = createCatalog(1, 10, inDays(2));
        for (int round = 0; round < 3; round++) {
            int show = round == 0 ? c.showId() : createShow(c.screenId(), "m1", inDays(3 + round), 100);
            List<Integer> seats = seatIds(show);

            // one task per seat, plus the admin cancelling the show in the middle of them
            race(seats.size() + 1, i -> () -> i == 0
                    ? postAs(ADMIN, "/api/admin/shows/" + show + "/cancel").andReturn().getResponse().getStatus()
                    : holdStatus(ALICE, show, seats.subList(i - 1, i)));

            assertThat(jdbc.queryForObject("select count(*) from booking where show_id = ? and booking_status in ('CREATED','CONFIRMED')",
                    Integer.class, show)).as("live bookings, round %d", round).isZero();
            assertThat(jdbc.queryForObject("select count(*) from seat where show_id = ? and status <> 'AVAILABLE'",
                    Integer.class, show)).as("held seats, round %d", round).isZero();
        }
    }
}
