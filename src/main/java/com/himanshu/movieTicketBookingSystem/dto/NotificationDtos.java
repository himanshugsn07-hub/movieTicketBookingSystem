package com.himanshu.movieTicketBookingSystem.dto;

import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;

import java.time.LocalDateTime;

public final class NotificationDtos {

    private NotificationDtos() {
    }

    public record NotificationResponse(long id, String bookingId, NotificationType type, String message,
                                       NotificationStatus status, LocalDateTime createdAt, LocalDateTime sendAt,
                                       LocalDateTime sentAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getBookingId(), n.getType(), n.getMessage(), n.getStatus(),
                    n.getCreatedAt(), n.getSendAt(), n.getSentAt());
        }
    }
}
