package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.constants.Queries;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    // Finds the show's seats with the given ids, ordered by id, under a pessimistic write lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(Queries.Seat.BY_SHOW_AND_IDS)
    List<Seat> findByShowIdAndIdIn(@Param("showId") int showId, @Param("seatIds") List<Integer> seatIds);

    // Finds all seats of the show ordered by id.
    List<Seat> findByShowIdOrderById(int showId);

    // Counts the show's seats that have the given status.
    long countByShowIdAndStatus(int showId, SeatStatus status);

    // Finds the show's seats that have the given status.
    List<Seat> findByShowIdAndStatus(int showId, SeatStatus status);
}
