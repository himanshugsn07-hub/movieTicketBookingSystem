package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class RefundPolicyService {

    private final RefundPolicyConfigRepository policyRepo;

    public RefundPolicyService(RefundPolicyConfigRepository policyRepo) {
        this.policyRepo = policyRepo;
    }

    // Creates a policy with the given tiers; the name must be unused.
    public RefundPolicyConfig create(String name, List<RefundTier> tiers) {
        validateTiers(tiers);
        if (policyRepo.findByName(name.trim()).isPresent()) {
            throw new ConflictException("Refund policy " + name.trim() + " already exists");
        }
        return policyRepo.save(new RefundPolicyConfig(name.trim(), tiers, false));
    }

    // Lists all refund policies.
    @Transactional(readOnly = true)
    public List<RefundPolicyConfig> list() {
        return policyRepo.findAll();
    }

    // Returns a policy by id, or throws NotFoundException.
    @Transactional(readOnly = true)
    public RefundPolicyConfig get(int id) {
        return find(id);
    }

    // Replaces a policy's name and tiers; bookings already made keep the tiers they were created with.
    public RefundPolicyConfig update(int id, String name, List<RefundTier> tiers) {
        RefundPolicyConfig policy = find(id);
        validateTiers(tiers);
        policyRepo.findByName(name.trim()).ifPresent(other -> {
            if (other.getId() != id) {
                throw new ConflictException("Refund policy " + name.trim() + " already exists");
            }
        });
        policy.update(name.trim(), tiers);
        return policy;
    }

    // Makes the policy the default, clearing the flag on the previous default.
    public RefundPolicyConfig makeDefault(int id) {
        RefundPolicyConfig policy = find(id);
        policyRepo.findAll().forEach(p -> p.setDefaultPolicy(p.getId() == id));
        return policy;
    }

    // Deletes a policy; the default policy and policies assigned to shows cannot be deleted.
    public void delete(int id) {
        RefundPolicyConfig policy = find(id);
        if (policy.isDefaultPolicy()) {
            throw new ConflictException("The default refund policy cannot be deleted; make another policy the default first");
        }
        policyRepo.delete(policy);
        policyRepo.flush();
    }

    // Returns the show's own refund policy, or the default one; throws InvalidStateException if none is configured.
    @Transactional(readOnly = true)
    public RefundPolicyConfig resolveFor(Show show) {
        if (show.getRefundPolicy() != null) {
            return show.getRefundPolicy();
        }
        return policyRepo.findByDefaultPolicyTrue()
                .orElseThrow(() -> new InvalidStateException("No refund policy is configured"));
    }

    private RefundPolicyConfig find(int id) {
        return policyRepo.findById(id).orElseThrow(() -> NotFoundException.of("Refund policy", id));
    }

    // Requires unique hours, a catch-all tier at 0 hours, and refunds that never rise as the show gets closer.
    private void validateTiers(List<RefundTier> tiers) {
        Set<Integer> hours = new HashSet<>();
        for (RefundTier t : tiers) {
            if (t.hoursBeforeShow() < 0 || t.refundPercent() < 0 || t.refundPercent() > 100) {
                throw new ValidationException("Tier hours must be >= 0 and refund percent between 0 and 100");
            }
            if (!hours.add(t.hoursBeforeShow())) {
                throw new ValidationException("Duplicate tier for " + t.hoursBeforeShow() + " hours");
            }
        }
        if (!hours.contains(0)) {
            throw new ValidationException("A tier with hoursBeforeShow = 0 is required so every cancellation is covered");
        }
        List<RefundTier> sorted = tiers.stream()
                .sorted(Comparator.comparingInt(RefundTier::hoursBeforeShow).reversed()).toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).refundPercent() > sorted.get(i - 1).refundPercent()) {
                throw new ValidationException("Refund percent must not increase as the show gets closer");
            }
        }
    }
}
