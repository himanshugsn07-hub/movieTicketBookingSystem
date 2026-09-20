package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;

@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final int MAX_ATTEMPTS = 3;

    @Autowired
    private NotificationRepository notificationRepo;

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private NotificationSender sender;

    @Autowired
    private PlatformTransactionManager txManager;

    // Delivers a new notification on the notification thread pool once the booking transaction has committed.
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCreated(NotificationCreatedEvent event) {
        deliver(event.notificationId());
    }

    // Every 15 seconds delivers due notifications: scheduled reminders and any retries that are still pending.
    @Scheduled(fixedDelay = 15_000)
    public void deliverDue() {
        for (Long id : notificationRepo.findDueIds(LocalDateTime.now(), PageRequest.of(0, 50))) {
            try {
                deliver(id);
            } catch (RuntimeException e) {
                log.warn("Delivery of notification {} failed unexpectedly", id, e);
            }
        }
    }

    // Locks the notification, skips it if already handled or not yet due, drops stale reminders, then sends it.
    private void deliver(long id) {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Notification n = notificationRepo.findByIdForUpdate(id).orElse(null);
            if (n == null || n.getStatus() != NotificationStatus.PENDING || n.getSendAt().isAfter(LocalDateTime.now())) {
                return;
            }
            if (n.getType() == NotificationType.REMINDER && !bookingStillActive(n.getBookingId())) {
                n.cancel();
                return;
            }
            try {
                sender.send(n);
                n.markSent(LocalDateTime.now());
            } catch (RuntimeException e) {
                n.recordFailure(MAX_ATTEMPTS);
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
