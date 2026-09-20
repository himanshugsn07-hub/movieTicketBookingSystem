package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {

    // Finds bookings with status CREATED and expiresAt before the given time.
    List<Booking> findExpiredBookings(LocalDateTime now);
}
