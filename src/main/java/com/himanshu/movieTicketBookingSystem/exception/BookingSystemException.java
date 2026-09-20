package com.himanshu.movieTicketBookingSystem.exception;

// Base unchecked exception for all booking system errors.
public class BookingSystemException extends RuntimeException {

    public BookingSystemException(String message) {
        super(message);
    }
}
