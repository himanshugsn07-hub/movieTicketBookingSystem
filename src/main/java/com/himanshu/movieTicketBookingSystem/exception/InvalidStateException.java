package com.himanshu.movieTicketBookingSystem.exception;

// Thrown for wrong status, started show or expired hold.
public class InvalidStateException extends BookingSystemException {

    public InvalidStateException(String message) {
        super(message);
    }
}
