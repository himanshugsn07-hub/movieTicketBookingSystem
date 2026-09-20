package com.himanshu.movieTicketBookingSystem.exception;

public class NotFoundException extends BookingSystemException {

    public NotFoundException(String message) {
        super(message);
    }

    // Builds the standard "<entity> <id> not found" exception.
    public static NotFoundException of(String entity, Object id) {
        return new NotFoundException(entity + " " + id + " not found");
    }
}
