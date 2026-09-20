package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import com.himanshu.movieTicketBookingSystem.entity.Payment;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.SeatUnavailableException;
import com.himanshu.movieTicketBookingSystem.exception.UnauthorizedException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.DiscountCodeRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import com.himanshu.movieTicketBookingSystem.strategy.PaymentStrategy;
import com.himanshu.movieTicketBookingSystem.strategy.PaymentStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.PricingStrategy;
import com.himanshu.movieTicketBookingSystem.strategy.PricingStrategyFactory;
import com.himanshu.movieTicketBookingSystem.strategy.RefundPolicy;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.himanshu.movieTicketBookingSystem.support.TestData.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final int SHOW_ID = 5;
    private static final int USER_ID = 7;

    @Mock BookingRepository bookingRepo;
    @Mock ShowRepository showRepo;
    @Mock SeatRepository seatRepo;
    @Mock DiscountCodeRepository discountCodeRepo;
    @Mock RefundPolicyService refundPolicyService;
    @Mock NotificationService notificationService;
    @Mock BookingExpiryService expiryService;
    @Mock PaymentStrategyFactory paymentFactory;
    @Mock PricingStrategyFactory pricingFactory;
    @Mock RefundPolicy refundPolicy;
    @Mock PaymentStrategy paymentStrategy;
    @Mock PricingStrategy pricingStrategy;

    private BookingService service;
    private Show show;
    private Seat seat1;
    private Seat seat2;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepo, showRepo, seatRepo, discountCodeRepo, refundPolicyService,
                notificationService, expiryService, paymentFactory, pricingFactory, refundPolicy,
                new TransactionTemplate(mock(PlatformTransactionManager.class)), TestData.CLOCK);
        show = TestData.show(NOW.plusDays(2));
        seat1 = new Seat(show, "A1");
        seat2 = new Seat(show, "A2");
    }

    // ---------------------------------------------------------------- createBooking

    static Stream<Arguments> invalidSeatIds() {
        return Stream.of(
                Arguments.of("null list", null),
                Arguments.of("empty list", List.of()),
                Arguments.of("null element", Arrays.asList(1, null)),
                Arguments.of("duplicate ids", List.of(1, 1)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidSeatIds")
    void createRejectsInvalidSeatIdsBeforeTouchingAnything(String name, List<Integer> seatIds) {
        assertThatThrownBy(() -> service.createBooking(USER_ID, SHOW_ID, seatIds)).isInstanceOf(ValidationException.class);

        verifyNoInteractions(showRepo, seatRepo, expiryService);
    }

    @Test
    void createReservesSeatsPricesThemAndHoldsTheBookingForFiveMinutes() {
        when(showRepo.findByIdForShare(SHOW_ID)).thenReturn(Optional.of(show));
        when(refundPolicyService.resolveFor(show)).thenReturn(TestData.standardPolicy());
        when(seatRepo.findByShowIdAndIdIn(SHOW_ID, List.of(11, 12))).thenReturn(List.of(seat1, seat2));
        when(pricingFactory.forTier(show.getPricingTier())).thenReturn(pricingStrategy);
        when(pricingStrategy.calculatePrice(show, List.of(seat1, seat2))).thenReturn(new BigDecimal("200.00"));
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = service.createBooking(USER_ID, SHOW_ID, List.of(11, 12));

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CREATED);
        assertThat(booking.getUserId()).isEqualTo(USER_ID);
        assertThat(booking.getAmount()).isEqualByComparingTo("200.00");
        assertThat(booking.getCreatedAt()).isEqualTo(NOW);
        assertThat(booking.getExpiresAt()).isEqualTo(NOW.plusMinutes(5));
        assertThat(booking.getRefundPolicyName()).isEqualTo("Standard");
        assertThat(List.of(seat1, seat2)).extracting(Seat::getStatus).containsOnly(SeatStatus.RESERVED);
        verify(expiryService).releaseExpiredBookings(SHOW_ID);
    }

    @Test
    void createStillWorksWhenTheExpirySweepFails() {
        doThrow(new RuntimeException("db down")).when(expiryService).releaseExpiredBookings(anyInt());
        when(showRepo.findByIdForShare(SHOW_ID)).thenReturn(Optional.of(show));
        when(refundPolicyService.resolveFor(show)).thenReturn(TestData.standardPolicy());
        when(seatRepo.findByShowIdAndIdIn(SHOW_ID, List.of(11))).thenReturn(List.of(seat1));
        when(pricingFactory.forTier(show.getPricingTier())).thenReturn(pricingStrategy);
        when(pricingStrategy.calculatePrice(any(), any())).thenReturn(TestData.SEAT_PRICE);
        when(bookingRepo.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.createBooking(USER_ID, SHOW_ID, List.of(11)).getBookingStatus()).isEqualTo(BookingStatus.CREATED);
    }

    @Test
    void createFailsForAMissingShow() {
        when(showRepo.findByIdForShare(SHOW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBooking(USER_ID, SHOW_ID, List.of(11))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createFailsForACancelledOrAlreadyStartedShow() {
        Show cancelled = TestData.show(NOW.plusDays(2));
        cancelled.cancel();
        Show started = TestData.show(NOW.minusMinutes(1));

        when(showRepo.findByIdForShare(1)).thenReturn(Optional.of(cancelled));
        when(showRepo.findByIdForShare(2)).thenReturn(Optional.of(started));

        assertThatThrownBy(() -> service.createBooking(USER_ID, 1, List.of(11))).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> service.createBooking(USER_ID, 2, List.of(11))).isInstanceOf(InvalidStateException.class);
        verifyNoInteractions(seatRepo);
    }

    @Test
    void createFailsWhenASeatIsAlreadyTaken() {
        seat2.reserve();
        when(showRepo.findByIdForShare(SHOW_ID)).thenReturn(Optional.of(show));
        when(seatRepo.findByShowIdAndIdIn(SHOW_ID, List.of(11, 12))).thenReturn(List.of(seat1, seat2));

        assertThatThrownBy(() -> service.createBooking(USER_ID, SHOW_ID, List.of(11, 12)))
                .isInstanceOf(SeatUnavailableException.class);
        verify(bookingRepo, never()).save(any());
    }

    @Test
    void createFailsWhenARequestedSeatDoesNotBelongToTheShow() {
        when(showRepo.findByIdForShare(SHOW_ID)).thenReturn(Optional.of(show));
        when(seatRepo.findByShowIdAndIdIn(SHOW_ID, List.of(11, 99))).thenReturn(List.of(seat1));

        assertThatThrownBy(() -> service.createBooking(USER_ID, SHOW_ID, List.of(11, 99))).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- confirmBooking

    private Booking heldBooking() {
        seat1.reserve();
        Booking booking = TestData.booking(show, List.of(seat1));
        when(bookingRepo.findByIdForUpdate("b1")).thenReturn(Optional.of(booking));
        return booking;
    }

    private void paymentReturns(Booking booking, PaymentStatus status) {
        when(paymentFactory.forPayment(PaymentType.CARD)).thenReturn(paymentStrategy);
        when(paymentStrategy.pay(booking)).thenReturn(new Payment("p1", PaymentType.CARD, booking.getPayableAmount(), status));
    }

    private DiscountCode percentCode(String code, boolean active, LocalDateTime validTo) {
        return new DiscountCode(code, DiscountType.PERCENT, new BigDecimal("20"), NOW.minusDays(1), validTo, null, active);
    }

    @Test
    void confirmFailsForAnUnknownBookingOrSomeoneElsesBooking() {
        when(bookingRepo.findByIdForUpdate("missing")).thenReturn(Optional.empty());
        heldBooking();

        assertThatThrownBy(() -> service.confirmBooking(USER_ID, "missing", PaymentType.CARD, null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.confirmBooking(USER_ID + 1, "b1", PaymentType.CARD, null))
                .isInstanceOf(UnauthorizedException.class);
        verifyNoInteractions(paymentFactory);
    }

    @Test
    void confirmFailsWhenTheBookingIsNotCreatedTheHoldHasLapsedOrTheShowStarted() {
        Booking confirmed = TestData.booking(show, List.of(seat1));
        confirmed.confirm(new Payment("p0", PaymentType.CARD, TestData.SEAT_PRICE, PaymentStatus.SUCCESS));
        when(bookingRepo.findByIdForUpdate("confirmed")).thenReturn(Optional.of(confirmed));

        Booking lapsed = TestData.booking(show, List.of(seat1), NOW.minusSeconds(1));
        when(bookingRepo.findByIdForUpdate("lapsed")).thenReturn(Optional.of(lapsed));

        Booking startedShow = TestData.booking(TestData.show(NOW.minusMinutes(5)), List.of(seat1));
        when(bookingRepo.findByIdForUpdate("started")).thenReturn(Optional.of(startedShow));

        for (String id : List.of("confirmed", "lapsed", "started")) {
            assertThatThrownBy(() -> service.confirmBooking(USER_ID, id, PaymentType.CARD, null))
                    .as(id).isInstanceOf(InvalidStateException.class);
        }
        verifyNoInteractions(paymentFactory);
    }

    @Test
    void confirmChargesThePayableAmountBooksTheSeatsAndNotifies() {
        Booking booking = heldBooking();
        paymentReturns(booking, PaymentStatus.SUCCESS);

        Booking result = service.confirmBooking(USER_ID, "b1", PaymentType.CARD, null);

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(seat1.getStatus()).isEqualTo(SeatStatus.BOOKED);
        verify(notificationService).bookingConfirmed(booking);
    }

    @Test
    void confirmAppliesADiscountCodeAndCountsItsUseOnlyOnSuccess() {
        Booking booking = heldBooking();
        DiscountCode code = percentCode("SAVE20", true, NOW.plusDays(1));
        when(discountCodeRepo.findByCodeForUpdate("SAVE20")).thenReturn(Optional.of(code));
        paymentReturns(booking, PaymentStatus.SUCCESS);

        service.confirmBooking(USER_ID, "b1", PaymentType.CARD, " save20 ");

        assertThat(booking.getDiscountAmount()).isEqualByComparingTo("20.00");
        assertThat(booking.getPayableAmount()).isEqualByComparingTo("80.00");
        assertThat(code.getUsedCount()).isEqualTo(1);
    }

    @Test
    void confirmRejectsUnknownOrUnusableDiscountCodesBeforeCharging() {
        heldBooking();
        when(discountCodeRepo.findByCodeForUpdate("NOPE")).thenReturn(Optional.empty());
        when(discountCodeRepo.findByCodeForUpdate("OLD")).thenReturn(Optional.of(percentCode("OLD", true, NOW.minusDays(1))));
        when(discountCodeRepo.findByCodeForUpdate("OFF")).thenReturn(Optional.of(percentCode("OFF", false, NOW.plusDays(1))));

        for (String code : List.of("NOPE", "OLD", "OFF")) {
            assertThatThrownBy(() -> service.confirmBooking(USER_ID, "b1", PaymentType.CARD, code))
                    .as(code).isInstanceOf(ValidationException.class);
        }
        verifyNoInteractions(paymentFactory);
    }

    @Test
    void failedPaymentReleasesTheSeatsAndDoesNotConsumeTheDiscount() {
        Booking booking = heldBooking();
        DiscountCode code = percentCode("SAVE20", true, NOW.plusDays(1));
        when(discountCodeRepo.findByCodeForUpdate("SAVE20")).thenReturn(Optional.of(code));
        paymentReturns(booking, PaymentStatus.FAILED);

        Booking result = service.confirmBooking(USER_ID, "b1", PaymentType.CARD, "SAVE20");

        assertThat(result.getBookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(result.getDiscountCode()).isNull();
        assertThat(result.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(seat1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(code.getUsedCount()).isZero();
        verify(notificationService).paymentFailed(booking);
        verify(notificationService, never()).bookingConfirmed(any());
    }

    // ---------------------------------------------------------------- cancelBooking

    private Booking confirmedBooking(Show forShow) {
        seat1.reserve();
        seat1.book();
        Booking booking = TestData.booking(forShow, List.of(seat1));
        booking.confirm(new Payment("p1", PaymentType.CARD, TestData.SEAT_PRICE, PaymentStatus.SUCCESS));
        when(bookingRepo.findByIdForUpdate("b1")).thenReturn(Optional.of(booking));
        return booking;
    }

    @Test
    void cancelAppliesTheRefundPolicyMarksThePaymentRefundedAndFreesTheSeats() {
        Booking booking = confirmedBooking(show);
        when(refundPolicy.calculateRefund(booking, NOW)).thenReturn(new BigDecimal("50.00"));

        service.cancelBooking(USER_ID, "b1");

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundAmount()).isEqualByComparingTo("50.00");
        assertThat(booking.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(seat1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        verify(notificationService).bookingCancelled(eq(booking), eq(new BigDecimal("50.00")));
    }

    @Test
    void cancelWithNoRefundLeavesThePaymentAsItWas() {
        Booking booking = confirmedBooking(show);
        when(refundPolicy.calculateRefund(booking, NOW)).thenReturn(BigDecimal.ZERO);

        service.cancelBooking(USER_ID, "b1");

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void cancelFailsForAStartedShowOrABookingThatIsNotConfirmed() {
        confirmedBooking(TestData.show(NOW.minusMinutes(5)));
        assertThatThrownBy(() -> service.cancelBooking(USER_ID, "b1")).isInstanceOf(InvalidStateException.class);

        Booking held = TestData.booking(show, List.of(seat2));
        when(bookingRepo.findByIdForUpdate("held")).thenReturn(Optional.of(held));
        when(refundPolicy.calculateRefund(held, NOW)).thenReturn(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.cancelBooking(USER_ID, "held")).isInstanceOf(InvalidStateException.class);
    }
}
