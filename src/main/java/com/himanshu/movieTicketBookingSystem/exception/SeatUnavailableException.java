package com.himanshu.movieTicketBookingSystem.exception;

// Thrown when a seat is already taken or held.
public class SeatUnavailableException extends ConflictException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
