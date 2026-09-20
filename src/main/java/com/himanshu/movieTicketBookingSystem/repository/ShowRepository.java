package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Integer>, JpaSpecificationExecutor<Show> {

    // Finds all shows on the given screen.
    List<Show> findByScreenId(int screenId);

    // Returns true if a non-cancelled show on the screen overlaps the given time range.
    @Query("select case when count(s) > 0 then true else false end from Show s "
            + "where s.screen.id = :screenId and s.cancelled = false and s.startTime < :end and s.endTime > :start")
    boolean existsOverlapping(@Param("screenId") int screenId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
