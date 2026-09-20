package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private int userId;
    private String bookingId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private int attempts;
    private LocalDateTime createdAt;

    // when the notification becomes due for delivery (now for immediate ones, show start - 1h for reminders)
    private LocalDateTime sendAt;
    private LocalDateTime sentAt;

    protected Notification() {
    }

    public Notification(int userId, String bookingId, NotificationType type, String message, LocalDateTime sendAt) {
        this.userId = userId;
        this.bookingId = bookingId;
        this.type = type;
        this.message = message;
        this.sendAt = sendAt;
        this.createdAt = LocalDateTime.now();
        this.status = NotificationStatus.PENDING;
    }

    // Returns the notification id.
    public long getId() {
        return id;
    }

    // Returns the id of the user to notify.
    public int getUserId() {
        return userId;
    }

    // Returns the related booking's confirmation id.
    public String getBookingId() {
        return bookingId;
    }

    // Returns the notification type.
    public NotificationType getType() {
        return type;
    }

    // Returns the message text.
    public String getMessage() {
        return message;
    }

    // Returns the delivery status.
    public NotificationStatus getStatus() {
        return status;
    }

    // Returns how many delivery attempts have failed so far.
    public int getAttempts() {
        return attempts;
    }

    // Returns when the notification was created.
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Returns when the notification is due for delivery.
    public LocalDateTime getSendAt() {
        return sendAt;
    }

    // Returns when the notification was delivered, or null.
    public LocalDateTime getSentAt() {
        return sentAt;
    }

    // Marks the notification as delivered.
    public void markSent(LocalDateTime when) {
        this.status = NotificationStatus.SENT;
        this.sentAt = when;
    }

    // Counts a failed attempt and gives up (FAILED) once maxAttempts is reached.
    public void recordFailure(int maxAttempts) {
        this.attempts++;
        if (attempts >= maxAttempts) {
            this.status = NotificationStatus.FAILED;
        }
    }

    // Withdraws a notification that has not been sent.
    public void cancel() {
        this.status = NotificationStatus.CANCELLED;
    }
}
