package com.himanshu.movieTicketBookingSystem.integration;

import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends IntegrationTest {

    @Test
    void registrationCreatesACustomerAndRejectsDuplicatesAndInvalidInput() throws Exception {
        postAnonymously("/api/auth/register", "{\"username\":\"carol\",\"password\":\"secret789\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("carol"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        postAnonymously("/api/auth/register", "{\"username\":\"carol\",\"password\":\"secret789\"}")
                .andExpect(status().isConflict());
        postAnonymously("/api/auth/register", "{\"username\":\"\",\"password\":\"1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("username")));
    }

    @Test
    void apisNeedALoginAndTheAdminApisNeedTheAdminRole() throws Exception {
        mvc.perform(get("/api/cities")).andExpect(status().isUnauthorized());
        getAs(httpBasic("alice", "wrong"), "/api/cities").andExpect(status().isUnauthorized());

        getAs(ALICE, "/api/cities").andExpect(status().isOk());
        getAs(ALICE, "/api/admin/cities").andExpect(status().isForbidden());
        getAs(ADMIN, "/api/admin/cities").andExpect(status().isOk());
    }

    @Test
    void aCustomerCannotUseAnyAdminWriteEndpoint() throws Exception {
        postAs(ALICE, "/api/admin/cities", "{\"name\":\"Pune\"}").andExpect(status().isForbidden());
        postAs(ALICE, "/api/admin/discount-codes", "{}").andExpect(status().isForbidden());
        postAs(ALICE, "/api/admin/refund-policies", "{}").andExpect(status().isForbidden());
        postAs(ALICE, "/api/admin/shows/1/cancel").andExpect(status().isForbidden());
    }
}
