package com.himanshu.movieTicketBookingSystem.dto;

import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class BrowseDtos {

    private BrowseDtos() {
    }

    public record ShowSummary(int id, String movieId, String movieTitle, int cityId, int theatreId,
                              String theatreName, int screenId, String screenName, LocalDateTime startTime,
                              LocalDateTime endTime, BigDecimal basePrice, PricingTier pricingTier,
                              boolean cancelled, long availableSeats) {
        public static ShowSummary from(Show s, long availableSeats) {
            return new ShowSummary(s.getId(), s.getMovie().getId(), s.getMovie().getTitle(),
                    s.getScreen().getTheatre().getCity().getId(), s.getScreen().getTheatre().getId(),
                    s.getScreen().getTheatre().getName(), s.getScreen().getId(), s.getScreen().getName(),
                    s.getStartTime(), s.getEndTime(), s.getBasePrice(), s.getPricingTier(), s.isCancelled(),
                    availableSeats);
        }
    }
}
