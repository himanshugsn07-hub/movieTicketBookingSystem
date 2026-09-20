package com.himanshu.movieTicketBookingSystem.exception;

// Thrown when a show, seat or booking is not found.
public class NotFoundException extends BookingSystemException {

    public NotFoundException(String message) {
        super(message);
    }
}
