package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.*;
import com.himanshu.movieTicketBookingSystem.service.AdminShowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constants.Api.ADMIN + "/shows")
public class AdminShowController {

    private final AdminShowService showService;

    public AdminShowController(AdminShowService showService) {
        this.showService = showService;
    }

    // Schedules a show and generates its seats.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponse createShow(@Valid @RequestBody ShowRequest r) {
        return ShowResponse.from(showService.createShow(r.screenId(), r.movieId(), r.basePrice(), r.pricingTier(), r.startTime()));
    }

    // Lists shows, optionally by screen.
    @GetMapping
    public List<ShowResponse> listShows(@RequestParam(required = false) Integer screenId) {
        return showService.listShows(screenId).stream().map(ShowResponse::from).toList();
    }

    // Cancels a show: refunds confirmed bookings in full, expires held ones and frees the seats.
    @PostMapping("/{id}/cancel")
    public ShowCancellationResponse cancelShow(@PathVariable int id) {
        AdminShowService.CancellationResult r = showService.cancelShow(id);
        return new ShowCancellationResponse(r.showId(), r.refundedBookings(), r.expiredBookings(), r.totalRefunded());
    }

    // Assigns a refund policy to a show, or clears it to use the default.
    @PutMapping("/{id}/refund-policy")
    public ShowResponse assignRefundPolicy(@PathVariable int id, @RequestBody ShowRefundPolicyRequest r) {
        return ShowResponse.from(showService.assignRefundPolicy(id, r.refundPolicyId()));
    }

    // Changes a show's base price and pricing tier.
    @PutMapping("/{id}/pricing")
    public ShowResponse updatePricing(@PathVariable int id, @Valid @RequestBody PricingRequest r) {
        return ShowResponse.from(showService.updatePricing(id, r.basePrice(), r.pricingTier()));
    }
}
