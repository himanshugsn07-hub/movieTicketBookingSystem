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
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Finds bookings with status CREATED and expiresAt before the given time.
    @Query("select b from Booking b where b.bookingStatus = com.himanshu.movieTicketBookingSystem.enums.BookingStatus.CREATED and b.expiresAt < :now order by b.expiresAt, b.confirmationId")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);

    // Finds the user's bookings, newest first.
    List<Booking> findByUserIdOrderByCreatedAtDesc(int userId, Pageable pageable);

    // Finds the user's bookings with the given status, newest first.
    List<Booking> findByUserIdAndBookingStatusOrderByCreatedAtDesc(int userId, BookingStatus status, Pageable pageable);

    // Finds a booking by id under a pessimistic write lock so concurrent changes are serialized.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.confirmationId = :confirmationId")
    Optional<Booking> findByIdForUpdate(@Param("confirmationId") String confirmationId);
}
