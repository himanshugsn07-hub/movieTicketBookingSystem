package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Theatre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    @ManyToOne
    private City city;

    @OneToMany(mappedBy = "theatre")
    private List<Screen> screens;

    protected Theatre() {
    }

    public Theatre(String name, City city) {
        this.name = name;
        this.city = city;
    }

    // Returns the theatre id.
    public int getId() {
        return id;
    }

    // Returns the theatre name.
    public String getName() {
        return name;
    }

    // Returns the city the theatre is in.
    public City getCity() {
        return city;
    }

    // Updates the theatre's name and city.
    public void update(String name, City city) {
        this.name = name;
        this.city = city;
    }
}
