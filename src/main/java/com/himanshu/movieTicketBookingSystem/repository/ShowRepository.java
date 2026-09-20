package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Show;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShowRepository extends JpaRepository<Show, Integer>, JpaSpecificationExecutor<Show> {

    // Finds a show under a write lock; used when cancelling it so no booking can be created meanwhile.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForUpdate(@Param("id") int id);

    // Finds a show under a shared lock; used when booking so a concurrent cancellation waits for it to finish.
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from Show s where s.id = :id")
    Optional<Show> findByIdForShare(@Param("id") int id);

    // Finds all shows on the given screen.
    List<Show> findByScreenId(int screenId);

    // Returns true if a non-cancelled show on the screen overlaps the given time range.
    @Query("select case when count(s) > 0 then true else false end from Show s "
            + "where s.screen.id = :screenId and s.cancelled = false and s.startTime < :end and s.endTime > :start")
    boolean existsOverlapping(@Param("screenId") int screenId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
