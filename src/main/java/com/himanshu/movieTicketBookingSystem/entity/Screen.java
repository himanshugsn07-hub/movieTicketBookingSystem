package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;
import java.util.List;

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

    @OneToMany(mappedBy = "screen")
    private List<Show> shows;
}
