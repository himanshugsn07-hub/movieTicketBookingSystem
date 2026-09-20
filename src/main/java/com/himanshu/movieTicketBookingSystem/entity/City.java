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
}
