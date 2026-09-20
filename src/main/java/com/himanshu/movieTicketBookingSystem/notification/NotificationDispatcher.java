package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);

    private final NotificationRepository notificationRepo;
    private final BookingRepository bookingRepo;
    private final NotificationSender sender;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public NotificationDispatcher(NotificationRepository notificationRepo, BookingRepository bookingRepo, NotificationSender sender,
                                  TransactionTemplate transactionTemplate,
            Clock clock) {
        this.notificationRepo = notificationRepo;
        this.bookingRepo = bookingRepo;
        this.sender = sender;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    // Delivers a new notification on the notification thread pool once the booking transaction has committed.
    @Async(Constants.Notifications.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(NotificationCreatedEvent event) {
        deliver(event.notificationId());
    }

    // Every 15 seconds delivers due notifications: scheduled reminders and any retries that are still pending.
    @Scheduled(fixedDelay = Constants.Scheduling.NOTIFICATION_SWEEP_MS)
    public void deliverDue() {
        for (Long id : notificationRepo.findDueIds(LocalDateTime.now(clock), PageRequest.of(0, Constants.Notifications.DUE_BATCH_SIZE))) {
            try {
                deliver(id);
            } catch (RuntimeException e) {
                log.warn("Delivery of notification {} failed unexpectedly", id, e);
            }
        }
    }

    // Locks the notification, skips it if already handled or not yet due, drops stale reminders, then sends it.
    private void deliver(long id) {
        transactionTemplate.executeWithoutResult(status -> {
            Notification n = notificationRepo.findByIdForUpdate(id).orElse(null);
            if (n == null || n.getStatus() != NotificationStatus.PENDING || n.getSendAt().isAfter(LocalDateTime.now(clock))) {
                return;
            }
            if (n.getType() == NotificationType.REMINDER && !bookingStillActive(n.getBookingId())) {
                n.cancel();
                return;
            }
            try {
                sender.send(n);
                n.markSent(LocalDateTime.now(clock));
            } catch (RuntimeException e) {
                n.recordFailure(Constants.Notifications.MAX_DELIVERY_ATTEMPTS);
                log.warn("Sending notification {} failed (attempt {})", id, n.getAttempts(), e);
            }
        });
    }

    private boolean bookingStillActive(String bookingId) {
        return bookingRepo.findById(bookingId)
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED && !b.getShow().isCancelled())
                .isPresent();
    }
}
