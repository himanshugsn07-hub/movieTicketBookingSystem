package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private final Show show = TestData.show(TestData.NOW.plusDays(2));
    private final Booking booking = TestData.booking(show, List.of(new Seat(show, "A1"), new Seat(show, "A2")));
    private final Payment payment = new Payment("p1", PaymentType.CARD, new BigDecimal("200.00"), PaymentStatus.SUCCESS);

    @Test
    void createdBookingCanBeConfirmedThenCancelledWithARefund() {
        booking.confirm(payment);
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.getPayment()).isSameAs(payment);

        booking.cancel(new BigDecimal("150.00"));
        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.getRefundAmount()).isEqualByComparingTo("150.00");
    }

    @Test
    void createdBookingCanExpireOrFailPayment() {
        Booking other = TestData.booking(show, List.of(new Seat(show, "A3")));

        booking.expire();
        other.markPaymentFailed();

        assertThat(booking.getBookingStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(other.getBookingStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
    }

    @Test
    void transitionsFromTheWrongStatusAreRejected() {
        assertThatThrownBy(() -> booking.cancel(BigDecimal.TEN)).isInstanceOf(InvalidStateException.class);

        booking.confirm(payment);
        assertThatThrownBy(() -> booking.confirm(payment)).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(booking::expire).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(booking::markPaymentFailed).isInstanceOf(InvalidStateException.class);
        assertThatThrownBy(() -> booking.applyDiscount("X", BigDecimal.ONE)).isInstanceOf(InvalidStateException.class);
    }

    @Test
    void discountReducesThePayableAmountAndCanBeCleared() {
        assertThat(booking.getPayableAmount()).isEqualByComparingTo("200.00");

        booking.applyDiscount("SAVE20", new BigDecimal("40.00"));
        assertThat(booking.getPayableAmount()).isEqualByComparingTo("160.00");
        assertThat(booking.getDiscountCode()).isEqualTo("SAVE20");

        booking.clearDiscount();
        assertThat(booking.getPayableAmount()).isEqualByComparingTo("200.00");
        assertThat(booking.getDiscountCode()).isNull();
    }
}
