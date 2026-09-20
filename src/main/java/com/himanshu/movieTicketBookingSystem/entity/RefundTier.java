package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.Embeddable;

import java.util.Arrays;
import java.util.List;

// One step of a refund policy: cancelling at least hoursBeforeShow hours before the show refunds refundPercent.
@Embeddable
public record RefundTier(int hoursBeforeShow, int refundPercent) {

    // Parses a snapshot such as "24:100,6:50,0:0" back into tiers.
    public static List<RefundTier> parseAll(String snapshot) {
        return Arrays.stream(snapshot.split(","))
                .map(part -> part.split(":"))
                .map(p -> new RefundTier(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim())))
                .toList();
    }
}
