package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.entity.Notification;

public interface NotificationSender {

    // Delivers the notification over some channel; throws if delivery fails so it can be retried.
    void send(Notification notification);
}
