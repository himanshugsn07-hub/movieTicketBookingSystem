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

    protected Movie() {
    }

    public Movie(String id, String title, String language, int durationMin, String genre) {
        this.id = id;
        this.title = title;
        this.language = language;
        this.durationMin = durationMin;
        this.genre = genre;
    }

    // Returns the movie id.
    public String getId() {
        return id;
    }

    // Returns the movie title.
    public String getTitle() {
        return title;
    }

    // Returns the movie language.
    public String getLanguage() {
        return language;
    }

    // Returns the running time in minutes.
    public int getDurationMin() {
        return durationMin;
    }

    // Returns the movie genre.
    public String getGenre() {
        return genre;
    }

    // Updates the movie's details.
    public void update(String title, String language, int durationMin, String genre) {
        this.title = title;
        this.language = language;
        this.durationMin = durationMin;
        this.genre = genre;
    }
}
