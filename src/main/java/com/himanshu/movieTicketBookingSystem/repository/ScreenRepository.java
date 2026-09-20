package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.constants.Queries;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenRepository extends JpaRepository<Screen, Integer> {

    // Finds all screens of the given theatre.
    List<Screen> findByTheatreId(int theatreId);

    // Finds a screen under a write lock so show scheduling on it is serialized.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(Queries.Screen.BY_ID)
    Optional<Screen> findByIdForUpdate(@Param("id") int id);
}
