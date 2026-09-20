package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import jakarta.persistence.*;
import java.util.ArrayList;
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

    protected Show() {
    }

    public Show(Screen screen, Movie movie, BigDecimal basePrice, PricingTier pricingTier,
                LocalDateTime startTime, LocalDateTime endTime) {
        this.screen = screen;
        this.movie = movie;
        this.basePrice = basePrice;
        this.pricingTier = pricingTier;
        this.startTime = startTime;
        this.endTime = endTime;
        this.seats = new ArrayList<>();
    }

    // Returns the show id.
    public int getId() {
        return id;
    }

    // Returns the screen the show plays on.
    public Screen getScreen() {
        return screen;
    }

    // Returns the movie being shown.
    public Movie getMovie() {
        return movie;
    }

    // Returns the show's end time.
    public LocalDateTime getEndTime() {
        return endTime;
    }

    // Creates one AVAILABLE seat per row/column, labelled A1, A2, B1 and so on.
    public void generateSeats(int rows, int columns) {
        for (int r = 0; r < rows; r++) {
            for (int c = 1; c <= columns; c++) {
                seats.add(new Seat(this, (char) ('A' + r) + String.valueOf(c)));
            }
        }
    }

    // Changes the base price and pricing tier.
    public void reprice(BigDecimal basePrice, PricingTier pricingTier) {
        this.basePrice = basePrice;
        this.pricingTier = pricingTier;
    }
}
