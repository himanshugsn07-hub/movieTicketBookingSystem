package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
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

    // Every 15 seconds delivers due notifications: scheduled reminders, pending retries, and claims whose worker died.
    @Scheduled(fixedDelay = Constants.Scheduling.NOTIFICATION_SWEEP_MS)
    public void deliverDue() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime staleBefore = now.minus(Constants.Notifications.CLAIM_LEASE);
        for (Long id : notificationRepo.findDueIds(now, staleBefore, PageRequest.of(0, Constants.Notifications.DUE_BATCH_SIZE))) {
            try {
                deliver(id);
            } catch (RuntimeException e) {
                log.warn("Delivery of notification {} failed unexpectedly", id, e);
            }
        }
    }

    // Claims the notification in a short transaction, sends it with no transaction open (so no row lock or database
    // connection is held while the provider is called), then records the outcome in a second short transaction.
    private void deliver(long id) {
        Notification claimed = transactionTemplate.execute(status -> claim(id));
        if (claimed == null) {
            return;
        }
        boolean sent = send(claimed);
        transactionTemplate.executeWithoutResult(status -> recordOutcome(id, sent));
    }

    // Locks the notification and takes it for sending, unless it is already handled, not due, or a reminder that went stale.
    private Notification claim(long id) {
        Notification n = notificationRepo.findByIdForUpdate(id).orElse(null);
        LocalDateTime now = LocalDateTime.now(clock);
        if (n == null || !n.isClaimable(now, now.minus(Constants.Notifications.CLAIM_LEASE))) {
            return null;
        }
        if (n.getType() == NotificationType.REMINDER && !bookingStillActive(n.getBookingId())) {
            n.cancel();
            return null;
        }
        n.claim(now);
        return n;
    }

    private boolean send(Notification n) {
        try {
            sender.send(n);
            return true;
        } catch (RuntimeException e) {
            log.warn("Sending notification {} failed", n.getId(), e);
            return false;
        }
    }

    private void recordOutcome(long id, boolean sent) {
        notificationRepo.findByIdForUpdate(id).ifPresent(n -> {
            if (sent) {
                n.markSent(LocalDateTime.now(clock));
            } else {
                n.recordFailure(Constants.Notifications.MAX_DELIVERY_ATTEMPTS);
            }
        });
    }

    private boolean bookingStillActive(String bookingId) {
        return bookingRepo.findById(bookingId)
                .filter(b -> b.getBookingStatus() == BookingStatus.CONFIRMED && !b.getShow().isCancelled())
                .isPresent();
    }
}
