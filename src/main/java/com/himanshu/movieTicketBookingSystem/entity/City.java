package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @OneToMany(mappedBy = "city")
    private List<Theatre> theatres;

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
