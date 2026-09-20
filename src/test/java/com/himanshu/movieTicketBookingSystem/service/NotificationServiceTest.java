package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Notification;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.NotificationType;
import com.himanshu.movieTicketBookingSystem.notification.NotificationCreatedEvent;
import com.himanshu.movieTicketBookingSystem.repository.NotificationRepository;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.himanshu.movieTicketBookingSystem.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository notificationRepo;
    @Mock ApplicationEventPublisher events;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepo, events, TestData.CLOCK);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Booking bookingForShowAt(LocalDateTime showStart) {
        Show show = TestData.show(showStart);
        return TestData.booking(show, List.of(new Seat(show, "A1")));
    }

    private List<Notification> savedNotifications(int expected) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    @Test
    void confirmationIsSentNowAndAReminderIsScheduledOneHourBeforeAFarShow() {
        LocalDateTime start = NOW.plusHours(5);

        service.bookingConfirmed(bookingForShowAt(start));

        List<Notification> saved = savedNotifications(2);
        assertThat(saved).extracting(Notification::getType).containsExactly(NotificationType.CONFIRMATION, NotificationType.REMINDER);
        assertThat(saved.get(0).getSendAt()).isEqualTo(NOW);
        assertThat(saved.get(1).getSendAt()).isEqualTo(start.minusHours(1));
        // only the immediate one is handed to the async delivery; the reminder waits for the scheduled sweep
        verify(events, times(1)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void noReminderIsScheduledWhenTheShowStartsWithinAnHour() {
        service.bookingConfirmed(bookingForShowAt(NOW.plusMinutes(30)));

        assertThat(savedNotifications(1)).extracting(Notification::getType).containsExactly(NotificationType.CONFIRMATION);
    }

    @Test
    void cancellingWithdrawsTheBookingsPendingReminder() {
        Booking booking = bookingForShowAt(NOW.plusDays(2));

        service.bookingCancelled(booking, new BigDecimal("50.00"));

        assertThat(savedNotifications(1)).extracting(Notification::getType).containsExactly(NotificationType.CANCELLATION);
        verify(notificationRepo).cancelPendingReminders("b1");
    }
}
