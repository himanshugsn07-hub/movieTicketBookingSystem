package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscountAndRefundPolicyIT extends IntegrationTest {

    private int createCode(String code, String type, int value, LocalDateTime from, LocalDateTime to, String extra) throws Exception {
        return read(postAs(ADMIN, "/api/admin/discount-codes",
                ("{\"code\":\"%s\",\"type\":\"%s\",\"value\":%d,\"validFrom\":\"%s\",\"validTo\":\"%s\"%s}")
                        .formatted(code, type, value, from, to, extra)).andExpect(status().isCreated()), "$.id");
    }

    private int createPolicy(String name, String tiersJson) throws Exception {
        return read(postAs(ADMIN, "/api/admin/refund-policies",
                "{\"name\":\"%s\",\"tiers\":%s}".formatted(name, tiersJson)).andExpect(status().isCreated()), "$.id");
    }

    // ------------------------------------------------------------------ discount codes

    @Test
    void aDiscountCodeReducesThePaymentAndTheRefundFollowsWhatWasPaid() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(5));
        createCode("SAVE20", "PERCENT", 20, inDays(-1), inDays(30), "");
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 2));

        confirm(ALICE, id, "save20").andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.discountCode").value("SAVE20"))
                .andExpect(jsonPath("$.discountAmount").value(40.00))
                .andExpect(jsonPath("$.payableAmount").value(160.00));
        assertThat(jdbc.queryForObject("select amount from payment", java.math.BigDecimal.class)).isEqualByComparingTo("160.00");

        postAs(ALICE, "/api/bookings/" + id + "/cancel").andExpect(status().isOk());
        getAs(ALICE, "/api/bookings/{id}", id).andExpect(jsonPath("$.refundAmount").value(160.00));
    }

    @Test
    void unknownExpiredOrInactiveCodesAreRejectedAndTheBookingCanStillBeConfirmed() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(5));
        createCode("OLD", "PERCENT", 10, inDays(-10), inDays(-5), "");
        createCode("OFF", "FLAT", 10, inDays(-1), inDays(5), ",\"active\":false");
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));

        for (String code : List.of("NOPE", "OLD", "OFF")) {
            confirm(ALICE, id, code).andExpect(status().isBadRequest());
        }
        assertThat(bookingStatus(id)).isEqualTo("CREATED");

        confirm(ALICE, id, null).andExpect(status().isOk()).andExpect(jsonPath("$.payableAmount").value(100.00));
    }

    @Test
    void aSingleUseCodeCanBeRedeemedOnlyOnce() throws Exception {
        Catalog c = createCatalog(1, 4, inDays(5));
        createCode("ONCE", "FLAT", 10, inDays(-1), inDays(5), ",\"maxUses\":1");
        List<Integer> seats = seatIds(c.showId());
        String alice = holdOk(ALICE, c.showId(), seats.subList(0, 1));
        String bob = holdOk(BOB, c.showId(), seats.subList(1, 2));

        confirm(ALICE, alice, "ONCE").andExpect(status().isOk());
        confirm(BOB, bob, "ONCE").andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("select used_count from discount_code", Integer.class)).isEqualTo(1);
    }

    @Test
    void discountCodeAdminRejectsDuplicatesAndOutOfRangeValues() throws Exception {
        createCode("SAVE", "PERCENT", 10, inDays(-1), inDays(5), "");

        postAs(ADMIN, "/api/admin/discount-codes", "{\"code\":\"save\",\"type\":\"FLAT\",\"value\":5,\"validFrom\":\"%s\",\"validTo\":\"%s\"}"
                .formatted(inDays(-1), inDays(5))).andExpect(status().isConflict());
        postAs(ADMIN, "/api/admin/discount-codes", "{\"code\":\"BIG\",\"type\":\"PERCENT\",\"value\":150,\"validFrom\":\"%s\",\"validTo\":\"%s\"}"
                .formatted(inDays(-1), inDays(5))).andExpect(status().isBadRequest());
        postAs(ADMIN, "/api/admin/discount-codes", "{\"code\":\"BAD\",\"type\":\"FLAT\",\"value\":5,\"validFrom\":\"%s\",\"validTo\":\"%s\"}"
                .formatted(inDays(5), inDays(-1))).andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ refund policies

    @Test
    void theSeededStandardPolicyRefundsInFullOnlyWhenCancellingAtLeastADayAhead() throws Exception {
        Catalog c = createCatalog(1, 4, inHours(30));
        int lateScreen = createScreen(c.theatreId(), "Screen 2", 1, 2);
        int lateShow = createShow(lateScreen, "m1", inHours(10), 100);
        String early = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));
        String late = holdOk(ALICE, lateShow, seatIds(lateShow).subList(0, 1));
        confirm(ALICE, early, null);
        confirm(ALICE, late, null);

        getAs(ALICE, "/api/bookings/{id}/refund-preview", early).andExpect(jsonPath("$.refundAmount").value(100.00));
        getAs(ALICE, "/api/bookings/{id}/refund-preview", late).andExpect(jsonPath("$.refundAmount").value(0.0));
    }

    @Test
    void tieredPoliciesDriveTheRefundAndBookingsKeepTheirSnapshotWhenThePolicyChanges() throws Exception {
        Catalog c = createCatalog(1, 4, inHours(20));
        int flexible = createPolicy("Flexible", "[{\"hoursBeforeShow\":48,\"refundPercent\":100},{\"hoursBeforeShow\":12,\"refundPercent\":50},{\"hoursBeforeShow\":0,\"refundPercent\":0}]");
        postAs(ADMIN, "/api/admin/refund-policies/" + flexible + "/default").andExpect(status().isOk());
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));
        confirm(ALICE, id, null);

        getAs(ALICE, "/api/bookings/{id}/refund-preview", id)
                .andExpect(jsonPath("$.refundAmount").value(50.00)).andExpect(jsonPath("$.policyName").value("Flexible"));

        putAs(ADMIN, "/api/admin/refund-policies/" + flexible, "{\"name\":\"Flexible\",\"tiers\":[{\"hoursBeforeShow\":0,\"refundPercent\":0}]}")
                .andExpect(status().isOk());

        getAs(ALICE, "/api/bookings/{id}/refund-preview", id).andExpect(jsonPath("$.refundAmount").value(50.00));
        postAs(ALICE, "/api/bookings/" + id + "/cancel").andExpect(status().isOk());
        getAs(ALICE, "/api/bookings/{id}", id).andExpect(jsonPath("$.refundAmount").value(50.00));
    }

    @Test
    void aShowSpecificPolicyOverridesTheDefault() throws Exception {
        Catalog c = createCatalog(1, 4, inHours(30));
        int standard = jdbc.queryForObject("select id from refund_policy where name = 'Standard'", Integer.class);
        int strict = createPolicy("Strict", "[{\"hoursBeforeShow\":0,\"refundPercent\":0}]");
        postAs(ADMIN, "/api/admin/refund-policies/" + strict + "/default").andExpect(status().isOk());

        putAs(ADMIN, "/api/admin/shows/" + c.showId() + "/refund-policy", "{\"refundPolicyId\":%d}".formatted(standard))
                .andExpect(status().isOk()).andExpect(jsonPath("$.refundPolicyId").value(standard));
        String id = holdOk(ALICE, c.showId(), seatIds(c.showId()).subList(0, 1));
        confirm(ALICE, id, null);

        getAs(ALICE, "/api/bookings/{id}/refund-preview", id)
                .andExpect(jsonPath("$.policyName").value("Standard")).andExpect(jsonPath("$.refundAmount").value(100.00));
    }

    @Test
    void refundPolicyAdminEnforcesItsRules() throws Exception {
        int standard = jdbc.queryForObject("select id from refund_policy where name = 'Standard'", Integer.class);

        String[] invalidTiers = {
                "[{\"hoursBeforeShow\":24,\"refundPercent\":100}]",
                "[{\"hoursBeforeShow\":0,\"refundPercent\":0},{\"hoursBeforeShow\":0,\"refundPercent\":10}]",
                "[{\"hoursBeforeShow\":0,\"refundPercent\":50},{\"hoursBeforeShow\":24,\"refundPercent\":10}]",
                "[{\"hoursBeforeShow\":0,\"refundPercent\":150}]",
                "[]"};
        for (String tiers : invalidTiers) {
            postAs(ADMIN, "/api/admin/refund-policies", "{\"name\":\"Bad\",\"tiers\":%s}".formatted(tiers)).andExpect(status().isBadRequest());
        }
        postAs(ADMIN, "/api/admin/refund-policies", "{\"name\":\"Standard\",\"tiers\":[{\"hoursBeforeShow\":0,\"refundPercent\":0}]}")
                .andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/admin/refund-policies/" + standard).andExpect(status().isConflict());
    }
}
