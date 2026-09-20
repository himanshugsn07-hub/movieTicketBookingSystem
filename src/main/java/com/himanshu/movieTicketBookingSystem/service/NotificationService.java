package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.notification.NotificationCreatedEvent;
import com.himanshu.movieTicketBookingSystem.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private ApplicationEventPublisher events;

    // Queues the booking-confirmed notice and, if the show is more than an hour away, a reminder for one hour before it.
    @Transactional
    public void bookingConfirmed(Booking b) {
        enqueue(b, NotificationType.CONFIRMATION, "Booking " + b.getConfirmationId() + " confirmed: " + describe(b)
                + ". Paid " + b.getPayableAmount() + ".", LocalDateTime.now());
        LocalDateTime start = b.getShow().getStartTime();
        if (start.isAfter(LocalDateTime.now().plusHours(1))) {
            enqueue(b, NotificationType.REMINDER, "Reminder: " + describe(b) + " starts in one hour.", start.minusHours(1));
        }
    }

    // Queues the payment-failed notice.
    @Transactional
    public void paymentFailed(Booking b) {
        enqueue(b, NotificationType.PAYMENT_FAILED, "Payment failed for booking " + b.getConfirmationId()
                + "; your seats were released. " + describe(b) + ".", LocalDateTime.now());
    }

    // Queues the cancellation notice with the refund amount and withdraws the booking's reminder.
    @Transactional
    public void bookingCancelled(Booking b, BigDecimal refund) {
        enqueue(b, NotificationType.CANCELLATION, "Booking " + b.getConfirmationId() + " cancelled. Refund: " + refund
                + ". " + describe(b) + ".", LocalDateTime.now());
        notificationRepo.cancelPendingReminders(b.getConfirmationId());
    }

    // Queues the notice sent when the admin cancels the show, and withdraws the booking's reminder.
    @Transactional
    public void showCancelled(Booking b, BigDecimal refund) {
        String outcome = refund.signum() > 0 ? "You are refunded " + refund + " in full." : "Your seat hold was released.";
        enqueue(b, NotificationType.CANCELLATION, "The show was cancelled by the theatre (booking "
                + b.getConfirmationId() + "). " + outcome + " " + describe(b) + ".", LocalDateTime.now());
        notificationRepo.cancelPendingReminders(b.getConfirmationId());
    }

    // Returns the user's notifications, newest first, without withdrawn ones.
    @Transactional(readOnly = true)
    public List<Notification> list(int userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ValidationException("page must be >= 0 and size between 1 and 100");
        }
        return notificationRepo.findByUserIdAndStatusNotOrderByCreatedAtDesc(userId, NotificationStatus.CANCELLED,
                PageRequest.of(page, size));
    }

    // Saves the notification in the caller's transaction; immediate ones are delivered asynchronously after commit.
    private void enqueue(Booking b, NotificationType type, String message, LocalDateTime sendAt) {
        Notification n = notificationRepo.save(new Notification(b.getUserId(), b.getConfirmationId(), type, message, sendAt));
        if (!sendAt.isAfter(LocalDateTime.now())) {
            events.publishEvent(new NotificationCreatedEvent(n.getId()));
        }
    }

    private String describe(Booking b) {
        String seats = b.getSeats().stream().map(Seat::getLabel).collect(Collectors.joining(", "));
        return b.getShow().getMovie().getTitle() + " at " + b.getShow().getStartTime() + ", seats " + seats;
    }
}
