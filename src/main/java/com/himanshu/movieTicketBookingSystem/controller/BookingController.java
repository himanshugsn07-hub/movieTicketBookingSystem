package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.*;
import com.himanshu.movieTicketBookingSystem.security.AppUserDetails;
import com.himanshu.movieTicketBookingSystem.service.BookingService;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    // Lists the logged-in user's bookings, newest first, optionally by status.
    @GetMapping("/bookings")
    public List<BookingResponse> history(@AuthenticationPrincipal AppUserDetails user,
                                         @RequestParam(required = false) BookingStatus status,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return bookingService.getBookingHistory(user.getId(), status, page, size).stream()
                .map(BookingResponse::from).toList();
    }

    // Returns one of the logged-in user's bookings.
    @GetMapping("/bookings/{confirmationId}")
    public BookingResponse getBooking(@AuthenticationPrincipal AppUserDetails user,
                                      @PathVariable String confirmationId) {
        return BookingResponse.from(bookingService.getBooking(user.getId(), confirmationId));
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
        return BookingResponse.from(bookingService.confirmBooking(user.getId(), confirmationId, request.paymentType(), request.discountCode()));
    }

    // Cancels the user's confirmed booking and refunds per policy.
    @PostMapping("/bookings/{confirmationId}/cancel")
    public CancelResponse cancelBooking(@AuthenticationPrincipal AppUserDetails user,
                                        @PathVariable String confirmationId) {
        return new CancelResponse(bookingService.cancelBooking(user.getId(), confirmationId));
    }
}
