package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.entity.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    // Optional artificial delay to simulate a slow email/SMS provider.
    @Value("${app.notification.simulated-delay-ms:0}")
    private long simulatedDelayMs;

    // Simulates delivery by logging the message after the optional delay.
    @Override
    public void send(Notification n) {
        if (simulatedDelayMs > 0) {
            try {
                Thread.sleep(simulatedDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while sending", e);
            }
        }
        log.info("[NOTIFY] user={} booking={} type={} message={}", n.getUserId(), n.getBookingId(), n.getType(), n.getMessage());
    }
}
