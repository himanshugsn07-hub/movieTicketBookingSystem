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

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "show_id")
    private List<Seat> seats;

    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    private PricingTier pricingTier;

    private boolean cancelled;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // Returns true if the show's start time has passed.
    public boolean hasStarted() {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns true if the show has been cancelled.
    public boolean isCancelled() {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns the seats of this show whose status is AVAILABLE.
    public List<Seat> getAvailableSeats() {
        throw new UnsupportedOperationException("TODO");
    }
}
