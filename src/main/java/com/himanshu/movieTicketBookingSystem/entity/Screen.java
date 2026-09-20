package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
public class Screen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToOne
    private Theatre theatre;

    private int rows;
    private int columns;

    protected Screen() {
    }

    public Screen(String name, Theatre theatre, int rows, int columns) {
        this.name = name;
        this.theatre = theatre;
        this.rows = rows;
        this.columns = columns;
    }

    // Returns the screen id.
    public int getId() {
        return id;
    }

    // Returns the screen name.
    public String getName() {
        return name;
    }

    // Returns the theatre the screen belongs to.
    public Theatre getTheatre() {
        return theatre;
    }

    // Returns the number of seat rows.
    public int getRows() {
        return rows;
    }

    // Returns the number of seat columns.
    public int getColumns() {
        return columns;
    }

    // Updates the screen's name, theatre and seat layout; only affects shows created afterwards.
    public void update(String name, Theatre theatre, int rows, int columns) {
        this.name = name;
        this.theatre = theatre;
        this.rows = rows;
        this.columns = columns;
    }
}
