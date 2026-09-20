package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminCatalogIT extends IntegrationTest {

    @Test
    void aShowGetsOneLabelledSeatPerCellOfTheScreenLayout() throws Exception {
        Catalog c = createCatalog(2, 3, inDays(2));

        getAs(ALICE, "/api/shows/{id}/seats", c.showId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].label").value("A1"))
                .andExpect(jsonPath("$[5].label").value("B3"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
        getAs(ADMIN, "/api/admin/screens?theatreId={id}", c.theatreId())
                .andExpect(jsonPath("$[0].rows").value(2))
                .andExpect(jsonPath("$[0].columns").value(3));
    }

    @Test
    void showSchedulingRejectsOverlapsPastTimesAndUnknownReferences() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));
        String body = "{\"screenId\":%d,\"movieId\":\"m1\",\"basePrice\":100,\"pricingTier\":\"REGULAR\",\"startTime\":\"%s\"}";

        postAs(ADMIN, "/api/admin/shows", body.formatted(c.screenId(), inDays(2).plusMinutes(30))).andExpect(status().isConflict());
        postAs(ADMIN, "/api/admin/shows", body.formatted(c.screenId(), inDays(-1))).andExpect(status().isBadRequest());
        postAs(ADMIN, "/api/admin/shows", body.formatted(9999, inDays(5))).andExpect(status().isNotFound());
        postAs(ADMIN, "/api/admin/shows", body.formatted(c.screenId(), inDays(2)).replace("m1", "nope")).andExpect(status().isNotFound());

        // a show that starts after the first one ends (2 hours) is fine
        postAs(ADMIN, "/api/admin/shows", body.formatted(c.screenId(), inDays(2).plusHours(3))).andExpect(status().isCreated());
    }

    @Test
    void catalogItemsThatAreInUseCannotBeDeleted() throws Exception {
        Catalog c = createCatalog(1, 2, inDays(2));

        deleteAs(ADMIN, "/api/admin/cities/" + c.cityId()).andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/admin/theatres/" + c.theatreId()).andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/admin/screens/" + c.screenId()).andExpect(status().isConflict());
        deleteAs(ADMIN, "/api/admin/movies/" + c.movieId()).andExpect(status().isConflict());

        int emptyCity = read(postAs(ADMIN, "/api/admin/cities", "{\"name\":\"Delhi\"}"), "$.id");
        deleteAs(ADMIN, "/api/admin/cities/" + emptyCity).andExpect(status().isNoContent());
        getAs(ADMIN, "/api/admin/cities").andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void invalidAdminInputIsRejectedWithAClearMessage() throws Exception {
        postAs(ADMIN, "/api/admin/cities", "{\"name\":\"  \"}").andExpect(status().isBadRequest());
        postAs(ADMIN, "/api/admin/cities", "{not json").andExpect(status().isBadRequest());
        postAs(ADMIN, "/api/admin/screens", "{\"name\":\"\",\"theatreId\":1,\"rows\":0,\"columns\":99}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("rows")))
                .andExpect(jsonPath("$.message").value(containsString("columns")));
        postAs(ADMIN, "/api/admin/theatres", "{\"name\":\"PVR\",\"cityId\":9999}").andExpect(status().isNotFound());
    }

    @Test
    void repricingAShowChangesWhatNewBookingsPay() throws Exception {
        Catalog c = createCatalog(1, 3, inDays(2));

        putAs(ADMIN, "/api/admin/shows/" + c.showId() + "/pricing", "{\"basePrice\":250,\"pricingTier\":\"PREMIUM\"}")
                .andExpect(status().isOk());

        // 250 x 2 seats x 1.5 (premium)
        hold(ALICE, c.showId(), seatIds(c.showId()).subList(0, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(750.00));
        assertThat(jdbc.queryForObject("select pricing_tier from show where id = ?", String.class, c.showId())).isEqualTo("PREMIUM");
    }
}
