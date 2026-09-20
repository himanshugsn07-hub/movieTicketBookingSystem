package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookingExpiryService {

    @Autowired
    private BookingRepository bookingRepo;

    // Expires CREATED bookings past their hold time and releases their seats.
    public void releaseExpiredBookings() {
    }
}
