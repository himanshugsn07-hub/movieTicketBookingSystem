package com.himanshu.movieTicketBookingSystem.exception;

// Thrown when the user is not the booking's owner.
public class UnauthorizedException extends BookingSystemException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
