package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    protected City() {
    }

    public City(String name) {
        this.name = name;
    }

    // Returns the city id.
    public int getId() {
        return id;
    }

    // Returns the city name.
    public String getName() {
        return name;
    }

    // Changes the city name.
    public void rename(String name) {
        this.name = name;
    }
}
