package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {

    // Finds movies matching the title that are showing in the given city.
    List<Movie> findByTitleInCity(String title, int cityId);
}
