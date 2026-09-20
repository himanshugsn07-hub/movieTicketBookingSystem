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
}
