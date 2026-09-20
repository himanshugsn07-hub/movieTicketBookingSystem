package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.entity.Screen;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.enums.PricingTier;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import com.himanshu.movieTicketBookingSystem.repository.ScreenRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.himanshu.movieTicketBookingSystem.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminShowServiceTest {

    @Mock ShowRepository showRepo;
    @Mock ScreenRepository screenRepo;
    @Mock BookingRepository bookingRepo;
    @Mock NotificationService notificationService;
    @Mock MovieRepository movieRepo;
    @Mock RefundPolicyConfigRepository refundPolicyRepo;

    private AdminShowService service;

    @BeforeEach
    void setUp() {
        service = new AdminShowService(showRepo, screenRepo, bookingRepo, notificationService, movieRepo,
                refundPolicyRepo, TestData.CLOCK);
    }

    // ---------------------------------------------------------------- createShow

    private final Screen screen = new Screen("S1", null, 2, 3);
    private final Movie movie = new Movie("m1", "Inception", "EN", 120, "SciFi");

    private void screenAndMovieExist() {
        when(screenRepo.findByIdForUpdate(1)).thenReturn(Optional.of(screen));
        when(movieRepo.findById("m1")).thenReturn(Optional.of(movie));
    }

    @Test
    void createShowRejectsAStartTimeThatIsNotInTheFuture() {
        screenAndMovieExist();

        assertThatThrownBy(() -> service.createShow(1, "m1", BigDecimal.TEN, PricingTier.REGULAR, NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createShowRejectsAnOverlapWithAnotherShowOnTheScreen() {
        screenAndMovieExist();
        LocalDateTime start = NOW.plusDays(1);
        when(showRepo.existsOverlapping(1, start, start.plusMinutes(120))).thenReturn(true);

        assertThatThrownBy(() -> service.createShow(1, "m1", BigDecimal.TEN, PricingTier.REGULAR, start))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void createShowEndsAfterTheMovieRunsAndGeneratesOneSeatPerLayoutCell() {
        screenAndMovieExist();
        LocalDateTime start = NOW.plusDays(1);
        when(showRepo.save(any(Show.class))).thenAnswer(inv -> inv.getArgument(0));

        Show show = service.createShow(1, "m1", BigDecimal.TEN, PricingTier.PREMIUM, start);

        assertThat(show.getEndTime()).isEqualTo(start.plusMinutes(120));
        assertThat((List<Seat>) ReflectionTestUtils.getField(show, "seats")).hasSize(6);
    }

    @Test
    void createShowFailsForAnUnknownScreenOrMovie() {
        when(screenRepo.findByIdForUpdate(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createShow(9, "m1", BigDecimal.TEN, PricingTier.REGULAR, NOW.plusDays(1)))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- cancelShow

    private Payment payment() {
        return new Payment("p1", PaymentType.CARD, TestData.SEAT_PRICE, PaymentStatus.SUCCESS);
    }

    @Test
    void cancelShowRefundsConfirmedBookingsInFullExpiresHoldsAndFreesSeats() {
        Show show = TestData.show(NOW.plusDays(2));
        when(showRepo.findByIdForUpdate(5)).thenReturn(Optional.of(show));

        Seat paidSeat = new Seat(show, "A1");
        paidSeat.reserve();
        paidSeat.book();
        Booking paid = new Booking("paid", 1, show, List.of(paidSeat), TestData.SEAT_PRICE, NOW, NOW.plusMinutes(5),
                TestData.standardPolicy());
        paid.applyDiscount("SAVE10", new BigDecimal("10.00"));
        paid.confirm(payment());

        Seat heldSeat = new Seat(show, "A2");
        heldSeat.reserve();
        Booking held = new Booking("held", 2, show, List.of(heldSeat), TestData.SEAT_PRICE, NOW, NOW.plusMinutes(5),
                TestData.standardPolicy());

        when(bookingRepo.findIdsByShowIdAndStatuses(5, List.of(BookingStatus.CREATED, BookingStatus.CONFIRMED)))
                .thenReturn(List.of("held", "paid"));
        when(bookingRepo.findByIdForUpdate("held")).thenReturn(Optional.of(held));
        when(bookingRepo.findByIdForUpdate("paid")).thenReturn(Optional.of(paid));

        AdminShowService.CancellationResult result = service.cancelShow(5);

        assertThat(result.refundedBookings()).isEqualTo(1);
        assertThat(result.expiredBookings()).isEqualTo(1);
        assertThat(result.totalRefunded()).isEqualByComparingTo("90.00");
        assertThat(show.isCancelled()).isTrue();

        assertThat(paid.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(paid.getRefundAmount()).isEqualByComparingTo("90.00");
        assertThat(paid.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paidSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(held.getBookingStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);

        ArgumentCaptor<BigDecimal> refunds = ArgumentCaptor.forClass(BigDecimal.class);
        verify(notificationService).showCancelled(eq(paid), refunds.capture());
        assertThat(refunds.getValue()).isEqualByComparingTo("90.00");
        verify(notificationService).showCancelled(eq(held), eq(BigDecimal.ZERO));
    }

    @Test
    void cancelShowRejectsAMissingAlreadyCancelledOrStartedShow() {
        Show cancelled = TestData.show(NOW.plusDays(2));
        cancelled.cancel();
        when(showRepo.findByIdForUpdate(1)).thenReturn(Optional.empty());
        when(showRepo.findByIdForUpdate(2)).thenReturn(Optional.of(cancelled));
        when(showRepo.findByIdForUpdate(3)).thenReturn(Optional.of(TestData.show(NOW.minusMinutes(1))));

        assertThatThrownBy(() -> service.cancelShow(1)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.cancelShow(2)).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> service.cancelShow(3)).isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(bookingRepo, notificationService);
    }
}
