package com.himanshu.movieTicketBookingSystem.exception;

// Thrown for bad input.
public class ValidationException extends BookingSystemException {

    public ValidationException(String message) {
        super(message);
    }
}
