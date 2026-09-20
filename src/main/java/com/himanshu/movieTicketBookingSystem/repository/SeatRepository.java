package com.himanshu.movieTicketBookingSystem.repository;

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
    @Query("select s from Seat s where s.show.id = :showId and s.id in :seatIds order by s.id")
    List<Seat> findByShowIdAndIdIn(@Param("showId") int showId, @Param("seatIds") List<Integer> seatIds);

    // Finds the show's seats that have the given status.
    List<Seat> findByShowIdAndStatus(int showId, SeatStatus status);
}
