package com.himanshu.movieTicketBookingSystem.exception;

// Thrown when the request conflicts with current state.
public class ConflictException extends BookingSystemException {

    public ConflictException(String message) {
        super(message);
    }
}
