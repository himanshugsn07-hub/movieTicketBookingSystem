package com.himanshu.movieTicketBookingSystem.integration;

import com.jayway.jsonpath.JsonPath;
import com.himanshu.movieTicketBookingSystem.config.AdminSeeder;
import com.himanshu.movieTicketBookingSystem.config.RefundPolicySeeder;
import com.himanshu.movieTicketBookingSystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Base for integration tests: the full application (security, controllers, services, JPA) against the
 * dedicated test database. Every test starts from an empty database that holds only the seeded admin,
 * the default refund policy and two customers, alice and bob.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTest {

    protected static final RequestPostProcessor ADMIN = httpBasic("admin", "admin123");
    protected static final RequestPostProcessor ALICE = httpBasic("alice", "secret123");
    protected static final RequestPostProcessor BOB = httpBasic("bob", "secret456");

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private UserService userService;
    @Autowired private AdminSeeder adminSeeder;
    @Autowired private RefundPolicySeeder refundPolicySeeder;
    @Autowired @Qualifier("notificationExecutor") private ThreadPoolTaskExecutor notificationExecutor;

    protected record Catalog(int cityId, int theatreId, int screenId, String movieId, int showId) {
    }

    @BeforeEach
    void resetDatabase() {
        // deliveries started by the previous test must not write into the freshly emptied tables
        await().atMost(Duration.ofSeconds(10))
                .until(() -> notificationExecutor.getActiveCount() == 0 && notificationExecutor.getQueueSize() == 0);
        jdbc.execute("truncate table booking_seats, booking, payment, seat, show, screen, theatre, city, movie, users, "
                + "discount_code, refund_policy_tier, refund_policy, notification restart identity cascade");
        adminSeeder.run(new DefaultApplicationArguments());
        refundPolicySeeder.run(new DefaultApplicationArguments());
        userService.register("alice", "secret123");
        userService.register("bob", "secret456");
    }

    // ------------------------------------------------------------------ HTTP helpers

    protected ResultActions getAs(RequestPostProcessor user, String url, Object... uriVars) throws Exception {
        return mvc.perform(get(url, uriVars).with(user));
    }

    protected ResultActions postAs(RequestPostProcessor user, String url, String json) throws Exception {
        return mvc.perform(withBody(post(url), json).with(user));
    }

    protected ResultActions postAnonymously(String url, String json) throws Exception {
        return mvc.perform(withBody(post(url), json));
    }

    protected ResultActions postAs(RequestPostProcessor user, String url) throws Exception {
        return mvc.perform(post(url).with(user));
    }

    protected ResultActions putAs(RequestPostProcessor user, String url, String json) throws Exception {
        return mvc.perform(withBody(put(url), json).with(user));
    }

    protected ResultActions deleteAs(RequestPostProcessor user, String url) throws Exception {
        return mvc.perform(delete(url).with(user));
    }

    private MockHttpServletRequestBuilder withBody(MockHttpServletRequestBuilder request, String json) {
        return request.contentType(MediaType.APPLICATION_JSON).content(json);
    }

    protected static <T> T read(ResultActions result, String jsonPath) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), jsonPath);
    }

    // ------------------------------------------------------------------ fixtures (built through the admin API)

    protected static LocalDateTime inDays(int days) {
        return LocalDateTime.now().plusDays(days).withNano(0);
    }

    protected static LocalDateTime inHours(long hours) {
        return LocalDateTime.now().plusHours(hours).withNano(0);
    }

    // A city with a theatre, a screen of rows x columns seats, a movie and one show starting at the given time.
    protected Catalog createCatalog(int rows, int columns, LocalDateTime showStart) throws Exception {
        int cityId = read(postAs(ADMIN, "/api/admin/cities", "{\"name\":\"Pune\"}"), "$.id");
        int theatreId = read(postAs(ADMIN, "/api/admin/theatres",
                "{\"name\":\"PVR\",\"cityId\":%d}".formatted(cityId)), "$.id");
        int screenId = createScreen(theatreId, "Screen 1", rows, columns);
        postAs(ADMIN, "/api/admin/movies",
                "{\"id\":\"m1\",\"title\":\"Inception\",\"language\":\"EN\",\"durationMin\":120,\"genre\":\"SciFi\"}");
        int showId = createShow(screenId, "m1", showStart, 100);
        return new Catalog(cityId, theatreId, screenId, "m1", showId);
    }

    protected int createScreen(int theatreId, String name, int rows, int columns) throws Exception {
        return read(postAs(ADMIN, "/api/admin/screens",
                "{\"name\":\"%s\",\"theatreId\":%d,\"rows\":%d,\"columns\":%d}".formatted(name, theatreId, rows, columns)), "$.id");
    }

    protected int createShow(int screenId, String movieId, LocalDateTime start, int basePrice) throws Exception {
        return read(postAs(ADMIN, "/api/admin/shows",
                ("{\"screenId\":%d,\"movieId\":\"%s\",\"basePrice\":%d,\"pricingTier\":\"REGULAR\",\"startTime\":\"%s\"}")
                        .formatted(screenId, movieId, basePrice, start)), "$.id");
    }

    protected List<Integer> seatIds(int showId) {
        return jdbc.queryForList("select id from seat where show_id = ? order by id", Integer.class, showId);
    }

    // ------------------------------------------------------------------ booking helpers

    protected static String ids(List<Integer> seatIds) {
        return seatIds.toString();
    }

    protected ResultActions hold(RequestPostProcessor user, int showId, List<Integer> seatIds) throws Exception {
        return postAs(user, "/api/bookings", "{\"showId\":%d,\"seatIds\":%s}".formatted(showId, ids(seatIds)));
    }

    // Holds the seats and returns the confirmation id.
    protected String holdOk(RequestPostProcessor user, int showId, List<Integer> seatIds) throws Exception {
        return read(hold(user, showId, seatIds), "$.confirmationId");
    }

    protected ResultActions confirm(RequestPostProcessor user, String confirmationId, String discountCode) throws Exception {
        String code = discountCode == null ? "null" : "\"" + discountCode + "\"";
        return postAs(user, "/api/bookings/" + confirmationId + "/confirm",
                "{\"paymentType\":\"CARD\",\"discountCode\":%s}".formatted(code));
    }

    protected String bookingStatus(String confirmationId) {
        return jdbc.queryForObject("select booking_status from booking where confirmation_id = ?", String.class, confirmationId);
    }
}
