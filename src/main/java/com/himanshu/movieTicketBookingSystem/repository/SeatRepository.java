package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Integer> {

    // Finds the show's seats with the given ids, ordered by id, under a pessimistic write lock.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Seat> findByShowIdAndIdIn(int showId, List<Integer> seatIds);

    // Finds the show's seats that have the given status.
    List<Seat> findByShowIdAndStatus(int showId, SeatStatus status);
}
