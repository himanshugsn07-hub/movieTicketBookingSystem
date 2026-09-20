package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
public class DiscountCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiscountType type;

    @Column(name = "discount_value", nullable = false)
    private BigDecimal value;

    private LocalDateTime validFrom;
    private LocalDateTime validTo;

    // null means unlimited redemptions
    private Integer maxUses;

    private int usedCount;
    private boolean active;

    protected DiscountCode() {
    }

    public DiscountCode(String code, DiscountType type, BigDecimal value, LocalDateTime validFrom,
                        LocalDateTime validTo, Integer maxUses, boolean active) {
        this.code = code;
        this.type = type;
        this.value = value;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.maxUses = maxUses;
        this.active = active;
    }

    // Returns the code id.
    public int getId() {
        return id;
    }

    // Returns the code text (upper case).
    public String getCode() {
        return code;
    }

    // Returns whether the value is a percentage or a flat amount.
    public DiscountType getType() {
        return type;
    }

    // Returns the percentage or flat amount.
    public BigDecimal getValue() {
        return value;
    }

    // Returns the start of the validity window.
    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    // Returns the end of the validity window.
    public LocalDateTime getValidTo() {
        return validTo;
    }

    // Returns the redemption limit, or null if unlimited.
    public Integer getMaxUses() {
        return maxUses;
    }

    // Returns how many times the code has been redeemed.
    public int getUsedCount() {
        return usedCount;
    }

    // Returns true if the code is switched on.
    public boolean isActive() {
        return active;
    }

    // Returns true if the code is active, inside its validity window and not fully used.
    public boolean isRedeemable(LocalDateTime now) {
        return active
                && !now.isBefore(validFrom)
                && !now.isAfter(validTo)
                && (maxUses == null || usedCount < maxUses);
    }

    // Returns the discount for the amount: percentage (rounded half-up) or flat, never more than the amount.
    public BigDecimal calculateDiscount(BigDecimal amount) {
        BigDecimal discount = type == DiscountType.PERCENT
                ? amount.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
        return discount.min(amount);
    }

    // Counts one redemption.
    public void recordUse() {
        usedCount++;
    }

    // Replaces the code's settings.
    public void update(String code, DiscountType type, BigDecimal value, LocalDateTime validFrom,
                       LocalDateTime validTo, Integer maxUses, boolean active) {
        this.code = code;
        this.type = type;
        this.value = value;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.maxUses = maxUses;
        this.active = active;
    }
}
