package com.himanshu.movieTicketBookingSystem.dto;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentStatus;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.enums.SeatStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CreateBookingRequest(@NotNull Integer showId, @NotEmpty List<@NotNull Integer> seatIds) {
    }

    public record ConfirmBookingRequest(@NotNull PaymentType paymentType, String discountCode) {
    }

    public record CancelResponse(boolean cancelled) {
    }

    public record MovieResponse(String id, String title, String language, int durationMin, String genre) {
        public static MovieResponse from(Movie m) {
            return new MovieResponse(m.getId(), m.getTitle(), m.getLanguage(), m.getDurationMin(), m.getGenre());
        }
    }

    public record SeatResponse(int id, String label, SeatStatus status) {
        public static SeatResponse from(Seat s) {
            return new SeatResponse(s.getId(), s.getLabel(), s.getStatus());
        }
    }

    public record BookingResponse(String confirmationId, int showId, String movieTitle, LocalDateTime showStartTime,
                                  List<Integer> seatIds, BookingStatus status,
                                  BigDecimal amount, String discountCode, BigDecimal discountAmount,
                                  BigDecimal payableAmount, BigDecimal refundAmount, LocalDateTime createdAt,
                                  LocalDateTime expiresAt, PaymentStatus paymentStatus) {
        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getConfirmationId(), b.getShow().getId(),
                    b.getShow().getMovie().getTitle(), b.getShow().getStartTime(),
                    b.getSeats().stream().map(Seat::getId).toList(), b.getBookingStatus(), b.getAmount(),
                    b.getDiscountCode(), b.getDiscountAmount(), b.getPayableAmount(), b.getRefundAmount(), b.getCreatedAt(), b.getExpiresAt(),
                    b.getPayment() == null ? null : b.getPayment().getStatus());
        }
    }
}
