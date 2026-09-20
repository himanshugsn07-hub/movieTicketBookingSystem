package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.*;
import com.himanshu.movieTicketBookingSystem.security.AppUserDetails;
import com.himanshu.movieTicketBookingSystem.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // Searches movies by title in a city.
    @GetMapping("/movies")
    public List<MovieResponse> searchMovies(@RequestParam String title, @RequestParam int cityId) {
        return bookingService.searchMovies(title, cityId).stream().map(MovieResponse::from).toList();
    }

    // Lists the available seats of a show.
    @GetMapping("/shows/{showId}/seats/available")
    public List<SeatResponse> availableSeats(@PathVariable int showId) {
        return bookingService.getAvailableSeats(showId).stream().map(SeatResponse::from).toList();
    }

    // Holds the requested seats for the logged-in user.
    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal AppUserDetails user,
                                         @Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.createBooking(user.getId(), request.showId(), request.seatIds()));
    }

    // Pays for and confirms the user's held booking.
    @PostMapping("/bookings/{confirmationId}/confirm")
    public BookingResponse confirmBooking(@AuthenticationPrincipal AppUserDetails user,
                                          @PathVariable String confirmationId,
                                          @Valid @RequestBody ConfirmBookingRequest request) {
        return BookingResponse.from(bookingService.confirmBooking(user.getId(), confirmationId, request.paymentType()));
    }

    // Cancels the user's confirmed booking and refunds per policy.
    @PostMapping("/bookings/{confirmationId}/cancel")
    public CancelResponse cancelBooking(@AuthenticationPrincipal AppUserDetails user,
                                        @PathVariable String confirmationId) {
        return new CancelResponse(bookingService.cancelBooking(user.getId(), confirmationId));
    }
}
