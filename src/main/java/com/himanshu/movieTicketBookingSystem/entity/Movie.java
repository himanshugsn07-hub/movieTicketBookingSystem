package com.himanshu.movieTicketBookingSystem.entity;

import jakarta.persistence.*;

@Entity
public class Movie {
    @Id
    private String id;

    private String title;
    private String language;
    private int durationMin;
    private String genre;
}
