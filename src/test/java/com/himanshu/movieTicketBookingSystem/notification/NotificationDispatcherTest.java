package com.himanshu.movieTicketBookingSystem.notification;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.NotificationStatus;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.NotificationRepository;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.himanshu.movieTicketBookingSystem.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock NotificationRepository notificationRepo;
    @Mock BookingRepository bookingRepo;
    @Mock NotificationSender sender;

    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
    private NotificationDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(notificationRepo, bookingRepo, sender,
                new TransactionTemplate(txManager), TestData.CLOCK);
    }

    private Notification notification(NotificationType type, LocalDateTime sendAt) {
        Notification n = new Notification(7, "b1", type, "hello", NOW.minusMinutes(1), sendAt);
        when(notificationRepo.findByIdForUpdate(1)).thenReturn(Optional.of(n));
        return n;
    }

    private void deliver() {
        dispatcher.onCreated(new NotificationCreatedEvent(1));
    }

    @Test
    void deliversADueNotificationAndMarksItSent() {
        Notification n = notification(NotificationType.CONFIRMATION, NOW);

        deliver();

        verify(sender).send(n);
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(n.getSentAt()).isEqualTo(NOW);
    }

    @Test
    void doesNotSendANotificationThatIsNotYetDueOrAlreadyHandled() {
        Notification future = notification(NotificationType.REMINDER, NOW.plusHours(1));
        deliver();

        Notification sent = notification(NotificationType.CONFIRMATION, NOW);
        sent.markSent(NOW);
        deliver();

        verify(sender, never()).send(any());
        assertThat(future.getStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void aFailingSendIsRetriedAndGivenUpOnAfterThreeAttempts() {
        Notification n = notification(NotificationType.CONFIRMATION, NOW);
        doThrow(new IllegalStateException("provider down")).when(sender).send(n);

        deliver();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(n.getAttempts()).isEqualTo(1);

        deliver();
        deliver();
        assertThat(n.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(n.getAttempts()).isEqualTo(3);
    }

    @Test
    void aReminderIsSentOnlyWhileTheBookingIsStillConfirmedForAnActiveShow() {
        Show show = TestData.show(NOW.plusHours(1));
        Booking booking = TestData.booking(show, List.of(new Seat(show, "A1")));
        when(bookingRepo.findById("b1")).thenReturn(Optional.of(booking));
        Notification reminder = notification(NotificationType.REMINDER, NOW);

        deliver();
        assertThat(reminder.getStatus()).as("booking still CREATED").isEqualTo(NotificationStatus.CANCELLED);
        verify(sender, never()).send(any());

        booking.confirm(new Payment("p1", PaymentType.CARD, TestData.SEAT_PRICE, PaymentStatus.SUCCESS));
        Notification second = notification(NotificationType.REMINDER, NOW);
        deliver();
        assertThat(second.getStatus()).as("booking CONFIRMED").isEqualTo(NotificationStatus.SENT);
        verify(sender).send(second);
    }

    @Test
    void theProviderIsCalledOutsideAnyTransaction() {
        Notification n = notification(NotificationType.CONFIRMATION, NOW);

        deliver();

        // claim committed, then the send, then the outcome committed: no lock or connection is held while sending
        InOrder order = inOrder(txManager, sender);
        order.verify(txManager).commit(any());
        order.verify(sender).send(n);
        order.verify(txManager).commit(any());
    }

    @Test
    void aClaimLeftByADeadWorkerIsTakenOverOnlyAfterTheLeaseExpires() {
        Notification abandoned = notification(NotificationType.CONFIRMATION, NOW.minusHours(1));
        abandoned.claim(NOW.minusMinutes(10));
        deliver();
        verify(sender).send(abandoned);
        assertThat(abandoned.getStatus()).isEqualTo(NotificationStatus.SENT);

        Notification inFlight = notification(NotificationType.CONFIRMATION, NOW.minusHours(1));
        inFlight.claim(NOW.minusSeconds(30));
        deliver();
        verify(sender, never()).send(inFlight);
        assertThat(inFlight.getStatus()).isEqualTo(NotificationStatus.SENDING);
    }
}
