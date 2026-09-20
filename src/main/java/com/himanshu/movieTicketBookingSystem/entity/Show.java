package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
public class Show {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private Screen screen;

    @ManyToOne
    private Movie movie;

    @OneToMany(mappedBy = "show", cascade = CascadeType.ALL)
    private List<Seat> seats;

    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    private PricingTier pricingTier;

    private boolean cancelled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Returns the base price of a single seat.
    public BigDecimal getBasePrice() {
        return basePrice;
    }

    // Returns the show's pricing tier.
    public PricingTier getPricingTier() {
        return pricingTier;
    }

    // Returns the show's start time.
    public LocalDateTime getStartTime() {
        return startTime;
    }

    // Returns true if the show's start time has passed.
    public boolean hasStarted() {
        return !LocalDateTime.now().isBefore(startTime);
    }

    // Returns true if the show has been cancelled.
    public boolean isCancelled() {
        return cancelled;
    }

    // Returns the seats of this show whose status is AVAILABLE.
    public List<Seat> getAvailableSeats() {
        return seats.stream().filter(Seat::isAvailable).toList();
    }
}
