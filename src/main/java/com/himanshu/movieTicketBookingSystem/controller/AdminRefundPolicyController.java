package com.himanshu.movieTicketBookingSystem.controller;

import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.RefundPolicyRequest;
import com.himanshu.movieTicketBookingSystem.dto.AdminDtos.RefundPolicyResponse;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.service.RefundPolicyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/refund-policies")
public class AdminRefundPolicyController {

    @Autowired
    private RefundPolicyService policyService;

    // Creates a refund policy.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RefundPolicyResponse create(@Valid @RequestBody RefundPolicyRequest r) {
        return RefundPolicyResponse.from(policyService.create(r.name(), toTiers(r)));
    }

    // Lists all refund policies.
    @GetMapping
    public List<RefundPolicyResponse> list() {
        return policyService.list().stream().map(RefundPolicyResponse::from).toList();
    }

    // Returns one refund policy.
    @GetMapping("/{id}")
    public RefundPolicyResponse get(@PathVariable int id) {
        return RefundPolicyResponse.from(policyService.get(id));
    }

    // Updates a refund policy; existing bookings keep their snapshot.
    @PutMapping("/{id}")
    public RefundPolicyResponse update(@PathVariable int id, @Valid @RequestBody RefundPolicyRequest r) {
        return RefundPolicyResponse.from(policyService.update(id, r.name(), toTiers(r)));
    }

    // Makes the policy the default for shows without their own.
    @PostMapping("/{id}/default")
    public RefundPolicyResponse makeDefault(@PathVariable int id) {
        return RefundPolicyResponse.from(policyService.makeDefault(id));
    }

    // Deletes a refund policy.
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable int id) {
        policyService.delete(id);
    }

    private List<RefundTier> toTiers(RefundPolicyRequest r) {
        return r.tiers().stream().map(t -> new RefundTier(t.hoursBeforeShow(), t.refundPercent())).toList();
    }
}
