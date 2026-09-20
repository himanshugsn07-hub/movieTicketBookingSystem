package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.constants.Queries;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, String> {

    // Finds movies matching the title that are showing in the given city.
    @Query(Queries.Movie.BY_TITLE_IN_CITY)
    List<Movie> findByTitleInCity(@Param("title") String title, @Param("cityId") int cityId);
}
