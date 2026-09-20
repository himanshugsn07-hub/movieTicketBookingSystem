package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Finds bookings with status CREATED and expiresAt before the given time.
    @Query("select b from Booking b where b.bookingStatus = com.himanshu.movieTicketBookingSystem.enums.BookingStatus.CREATED and b.expiresAt < :now")
    List<Booking> findExpiredBookings(@Param("now") LocalDateTime now);
}
