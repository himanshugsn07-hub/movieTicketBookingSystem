package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.DiscountCodeRequest;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.DiscountCodeResponse;
import com.himanshu.movieTicketBookingSystem.service.DiscountCodeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(Constants.Api.ADMIN + "/discount-codes")
public class AdminDiscountController {

    private final DiscountCodeService discountService;

    public AdminDiscountController(DiscountCodeService discountService) {
        this.discountService = discountService;
    }

    // Creates a discount code.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DiscountCodeResponse create(@Valid @RequestBody DiscountCodeRequest r) {
        return DiscountCodeResponse.from(discountService.create(r.code(), r.type(), r.value(), r.validFrom(),
                r.validTo(), r.maxUses(), r.active() == null || r.active()));
    }

    // Lists all discount codes.
    @GetMapping
    public List<DiscountCodeResponse> list() {
        return discountService.list().stream().map(DiscountCodeResponse::from).toList();
    }

    // Returns one discount code.
    @GetMapping("/{id}")
    public DiscountCodeResponse get(@PathVariable int id) {
        return DiscountCodeResponse.from(discountService.get(id));
    }

    // Updates a discount code.
    @PutMapping("/{id}")
    public DiscountCodeResponse update(@PathVariable int id, @Valid @RequestBody DiscountCodeRequest r) {
        return DiscountCodeResponse.from(discountService.update(id, r.code(), r.type(), r.value(), r.validFrom(),
                r.validTo(), r.maxUses(), r.active() == null || r.active()));
    }

    // Deletes a discount code.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        discountService.delete(id);
    }
}
