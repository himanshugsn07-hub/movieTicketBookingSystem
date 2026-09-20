package com.himanshu.movieTicketBookingSystem.dto;

import com.himanshu.movieTicketBookingSystem.entity.City;
import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.entity.Theatre;
import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DiscountCodeRequest(@NotBlank String code, @NotNull DiscountType type,
                                      @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal value,
                                      @NotNull LocalDateTime validFrom, @NotNull LocalDateTime validTo,
                                      @Min(1) Integer maxUses, Boolean active) {
    }

    public record DiscountCodeResponse(int id, String code, DiscountType type, BigDecimal value,
                                       LocalDateTime validFrom, LocalDateTime validTo, Integer maxUses,
                                       int usedCount, boolean active) {
        public static DiscountCodeResponse from(DiscountCode d) {
            return new DiscountCodeResponse(d.getId(), d.getCode(), d.getType(), d.getValue(), d.getValidFrom(),
                    d.getValidTo(), d.getMaxUses(), d.getUsedCount(), d.isActive());
        }
    }

    public record RefundTierRequest(@NotNull @Min(0) Integer hoursBeforeShow,
                                    @NotNull @Min(0) @Max(100) Integer refundPercent) {
    }

    public record RefundPolicyRequest(@NotBlank String name, @NotEmpty @Valid List<RefundTierRequest> tiers) {
    }

    public record RefundPolicyResponse(int id, String name, boolean defaultPolicy, List<RefundTierRequest> tiers) {
        public static RefundPolicyResponse from(RefundPolicyConfig p) {
            return new RefundPolicyResponse(p.getId(), p.getName(), p.isDefaultPolicy(),
                    p.getTiers().stream()
                            .sorted(Comparator.comparingInt(RefundTier::hoursBeforeShow).reversed())
                            .map(t -> new RefundTierRequest(t.hoursBeforeShow(), t.refundPercent())).toList());
        }
    }

    public record ShowRefundPolicyRequest(Integer refundPolicyId) {
    }

    public record CityRequest(@NotBlank String name) {
    }

    public record CityResponse(int id, String name) {
        public static CityResponse from(City c) {
            return new CityResponse(c.getId(), c.getName());
        }
    }

    public record TheatreRequest(@NotBlank String name, @NotNull Integer cityId) {
    }

    public record TheatreResponse(int id, String name, int cityId) {
        public static TheatreResponse from(Theatre t) {
            return new TheatreResponse(t.getId(), t.getName(), t.getCity().getId());
        }
    }

    public record ScreenRequest(@NotBlank String name, @NotNull Integer theatreId,
                                @NotNull @Min(1) @Max(26) Integer rows, @NotNull @Min(1) @Max(50) Integer columns) {
    }

    public record ScreenResponse(int id, String name, int theatreId, int rows, int columns) {
        public static ScreenResponse from(Screen s) {
            return new ScreenResponse(s.getId(), s.getName(), s.getTheatre().getId(), s.getRows(), s.getColumns());
        }
    }

    public record MovieRequest(String id, @NotBlank String title, @NotBlank String language,
                               @NotNull @Min(1) Integer durationMin, @NotBlank String genre) {
    }

    public record ShowRequest(@NotNull Integer screenId, @NotBlank String movieId,
                              @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal basePrice,
                              @NotNull PricingTier pricingTier, @NotNull @Future LocalDateTime startTime) {
    }

    public record PricingRequest(@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal basePrice,
                                 @NotNull PricingTier pricingTier) {
    }

    public record ShowResponse(int id, int screenId, String movieId, BigDecimal basePrice, PricingTier pricingTier,
                               boolean cancelled, LocalDateTime startTime, LocalDateTime endTime,
                               Integer refundPolicyId) {
        public static ShowResponse from(Show s) {
            return new ShowResponse(s.getId(), s.getScreen().getId(), s.getMovie().getId(), s.getBasePrice(),
                    s.getPricingTier(), s.isCancelled(), s.getStartTime(), s.getEndTime(),
                    s.getRefundPolicy() == null ? null : s.getRefundPolicy().getId());
        }
    }
}
