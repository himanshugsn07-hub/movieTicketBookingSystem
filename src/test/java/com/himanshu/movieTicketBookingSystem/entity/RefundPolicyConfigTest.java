package com.himanshu.movieTicketBookingSystem.entity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyConfigTest {

    @Test
    void snapshotListsLongestNoticeFirstAndParsesBackToTheSameTiers() {
        RefundPolicyConfig policy = new RefundPolicyConfig("Flexible",
                List.of(new RefundTier(0, 0), new RefundTier(48, 100), new RefundTier(12, 50)), false);

        String snapshot = policy.toSnapshot();

        assertThat(snapshot).isEqualTo("48:100,12:50,0:0");
        assertThat(RefundTier.parseAll(snapshot))
                .containsExactly(new RefundTier(48, 100), new RefundTier(12, 50), new RefundTier(0, 0));
    }
}
