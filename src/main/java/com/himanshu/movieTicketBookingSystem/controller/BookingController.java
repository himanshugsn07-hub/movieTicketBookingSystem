package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.dto.BookingDtos.*;
import com.himanshu.movieTicketBookingSystem.enums.BookingStatus;
import com.himanshu.movieTicketBookingSystem.security.AppUserDetails;
import com.himanshu.movieTicketBookingSystem.service.BookingQueryService;
import com.himanshu.movieTicketBookingSystem.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constants.Api.ROOT + "/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingQueryService bookingQueryService;

    public BookingController(BookingService bookingService, BookingQueryService bookingQueryService) {
        this.bookingService = bookingService;
        this.bookingQueryService = bookingQueryService;
    }

    // Lists the logged-in user's bookings, newest first, optionally by status.
    @GetMapping
    public List<BookingResponse> history(@AuthenticationPrincipal AppUserDetails user,
                                         @RequestParam(required = false) BookingStatus status,
                                         @RequestParam(defaultValue = Constants.Paging.DEFAULT_PAGE) int page,
                                         @RequestParam(defaultValue = Constants.Paging.DEFAULT_SIZE) int size) {
        return bookingQueryService.getBookingHistory(user.getId(), status, page, size).stream()
                .map(BookingResponse::from).toList();
    }

    // Returns one of the logged-in user's bookings.
    @GetMapping("/{confirmationId}")
    public BookingResponse getBooking(@AuthenticationPrincipal AppUserDetails user,
                                      @PathVariable String confirmationId) {
        return BookingResponse.from(bookingQueryService.getBooking(user.getId(), confirmationId));
    }

    // Holds the requested seats for the logged-in user.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createBooking(@AuthenticationPrincipal AppUserDetails user,
                                         @Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.createBooking(user.getId(), request.showId(), request.seatIds()));
    }

    // Pays for and confirms the user's held booking.
    @PostMapping("/{confirmationId}/confirm")
    public BookingResponse confirmBooking(@AuthenticationPrincipal AppUserDetails user,
                                          @PathVariable String confirmationId,
                                          @Valid @RequestBody ConfirmBookingRequest request) {
        return BookingResponse.from(bookingService.confirmBooking(user.getId(), confirmationId, request.paymentType(), request.discountCode()));
    }

    // Shows the refund the user would get by cancelling their confirmed booking right now.
    @GetMapping("/{confirmationId}/refund-preview")
    public RefundPreviewResponse refundPreview(@AuthenticationPrincipal AppUserDetails user,
                                               @PathVariable String confirmationId) {
        BookingQueryService.RefundPreview preview = bookingQueryService.previewRefund(user.getId(), confirmationId);
        return new RefundPreviewResponse(preview.refundAmount(), preview.policyName());
    }

    // Cancels the user's confirmed booking and refunds per policy.
    @PostMapping("/{confirmationId}/cancel")
    public CancelResponse cancelBooking(@AuthenticationPrincipal AppUserDetails user,
                                        @PathVariable String confirmationId) {
        bookingService.cancelBooking(user.getId(), confirmationId);
        return new CancelResponse(true);
    }
}
