package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Finds bookings with status CREATED and expiresAt before the given time.
    @Query("select b from Booking b where b.bookingStatus = com.himanshu.movieTicketBookingSystem.enums.BookingStatus.CREATED and b.expiresAt < :now order by b.expiresAt, b.confirmationId")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    // Finds ids of CREATED bookings whose hold ended before the given time, oldest first (ids only, no seats loaded).
    @Query("select b.confirmationId from Booking b where b.bookingStatus = com.himanshu.movieTicketBookingSystem.enums.BookingStatus.CREATED and b.expiresAt < :now order by b.expiresAt, b.confirmationId")
    List<String> findExpiredBookingIds(@Param("now") LocalDateTime now);

    // Same as findExpiredBookingIds but only for one show.
    @Query("select b.confirmationId from Booking b where b.bookingStatus = com.himanshu.movieTicketBookingSystem.enums.BookingStatus.CREATED and b.expiresAt < :now and b.show.id = :showId order by b.expiresAt, b.confirmationId")
    List<String> findExpiredBookingIdsByShowId(@Param("now") LocalDateTime now, @Param("showId") int showId);

    // Finds the user's bookings, newest first.
    List<Booking> findByUserIdOrderByCreatedAtDesc(int userId, Pageable pageable);

    // Finds the user's bookings with the given status, newest first.
    List<Booking> findByUserIdAndBookingStatusOrderByCreatedAtDesc(int userId, BookingStatus status, Pageable pageable);

    // Finds the ids of the show's bookings that have one of the given statuses, in id order.
    @Query("select b.confirmationId from Booking b where b.show.id = :showId and b.bookingStatus in :statuses order by b.confirmationId")
    List<String> findIdsByShowIdAndStatuses(@Param("showId") int showId, @Param("statuses") Collection<BookingStatus> statuses);

    // Finds a booking by id under a pessimistic write lock so concurrent changes are serialized.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.confirmationId = :confirmationId")
    Optional<Booking> findByIdForUpdate(@Param("confirmationId") String confirmationId);
}
