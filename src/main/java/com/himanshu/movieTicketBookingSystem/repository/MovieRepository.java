package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {

    // Finds movies matching the title that are showing in the given city.
    @Query("select distinct s.movie from Show s where lower(s.movie.title) like lower(concat('%', :title, '%')) and s.screen.theatre.city.id = :cityId")
    List<Movie> findByTitleInCity(@Param("title") String title, @Param("cityId") int cityId);
}
