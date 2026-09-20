package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.Booking;
import com.himanshu.movieTicketBookingSystem.entity.Movie;
import com.himanshu.movieTicketBookingSystem.entity.Seat;
import com.himanshu.movieTicketBookingSystem.enums.PaymentType;
import com.himanshu.movieTicketBookingSystem.repository.BookingRepository;
import com.himanshu.movieTicketBookingSystem.repository.MovieRepository;
import com.himanshu.movieTicketBookingSystem.repository.SeatRepository;
import com.himanshu.movieTicketBookingSystem.repository.ShowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class BookingService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    @Autowired
    private BookingRepository bookingRepo;

    @Autowired
    private MovieRepository movieRepo;

    @Autowired
    private ShowRepository showRepo;

    @Autowired
    private SeatRepository seatRepo;

    // TODO: paymentStrategyFactory, pricingStrategyFactory and refundPolicy are added in the strategies step.

    // Searches movies by title that are showing in the given city.
    public List<Movie> searchMovies(String title, int cityId) {
        throw new UnsupportedOperationException("TODO");
    }

    // Returns the available seats for the given show.
    public List<Seat> getAvailableSeats(int showId) {
        throw new UnsupportedOperationException("TODO");
    }

    // Reserves the seats, prices the booking and creates it in CREATED status with a hold expiry.
    public Booking createBooking(int userId, int showId, List<Integer> seatIds) {
        throw new UnsupportedOperationException("TODO");
    }

    // Pays for a CREATED booking owned by the user and confirms it, or marks payment failed.
    public Booking confirmBooking(int userId, String confirmationId, PaymentType paymentType) {
        throw new UnsupportedOperationException("TODO");
    }

    // Cancels the user's booking, releases its seats and applies the refund policy.
    public boolean cancelBooking(int userId, String confirmationId) {
        throw new UnsupportedOperationException("TODO");
    }
}
