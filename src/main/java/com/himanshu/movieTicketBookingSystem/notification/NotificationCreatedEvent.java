package com.himanshu.movieTicketBookingSystem.notification;

// Published when a notification that is due immediately has been saved.
public record NotificationCreatedEvent(long notificationId) {
}
