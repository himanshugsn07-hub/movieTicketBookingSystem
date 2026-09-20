package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import jakarta.persistence.*;

@Entity
public class Seat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "show_id")
    private Show show;

    @Enumerated(EnumType.STRING)
    private SeatStatus status;

    // Returns the seat id.
    public int getId() {
        return id;
    }

    // Returns true if the seat status is AVAILABLE.
    public boolean isAvailable() {
        return status == SeatStatus.AVAILABLE;
    }

    // Moves the seat from AVAILABLE to RESERVED; returns false if it was not available.
    public boolean reserve() {
        if (!isAvailable()) {
            return false;
        }
        status = SeatStatus.RESERVED;
        return true;
    }

    // Marks the seat as BOOKED.
    public void book() {
        status = SeatStatus.BOOKED;
    }

    // Marks the seat as AVAILABLE again.
    public void release() {
        status = SeatStatus.AVAILABLE;
    }
}
