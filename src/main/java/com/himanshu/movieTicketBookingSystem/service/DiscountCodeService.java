package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.NotFoundException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.DiscountCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DiscountCodeService {

    private final DiscountCodeRepository discountRepo;

    public DiscountCodeService(DiscountCodeRepository discountRepo) {
        this.discountRepo = discountRepo;
    }

    // Creates a discount code; the text is stored upper-case and must be unused.
    public DiscountCode create(String code, DiscountType type, BigDecimal value, LocalDateTime validFrom,
                               LocalDateTime validTo, Integer maxUses, boolean active) {
        String normalized = normalize(code);
        validate(type, value, validFrom, validTo, maxUses, 0);
        if (discountRepo.findByCode(normalized).isPresent()) {
            throw new ConflictException("Discount code " + normalized + " already exists");
        }
        return discountRepo.save(new DiscountCode(normalized, type, value, validFrom, validTo, maxUses, active));
    }

    // Lists all discount codes.
    @Transactional(readOnly = true)
    public List<DiscountCode> list() {
        return discountRepo.findAll();
    }

    // Returns a discount code by id, or throws NotFoundException.
    @Transactional(readOnly = true)
    public DiscountCode get(int id) {
        return find(id);
    }

    // Updates a code; maxUses may not drop below the number of redemptions already made.
    public DiscountCode update(int id, String code, DiscountType type, BigDecimal value, LocalDateTime validFrom,
                               LocalDateTime validTo, Integer maxUses, boolean active) {
        DiscountCode existing = find(id);
        String normalized = normalize(code);
        validate(type, value, validFrom, validTo, maxUses, existing.getUsedCount());
        discountRepo.findByCode(normalized).ifPresent(other -> {
            if (other.getId() != id) {
                throw new ConflictException("Discount code " + normalized + " already exists");
            }
        });
        existing.update(normalized, type, value, validFrom, validTo, maxUses, active);
        return existing;
    }

    // Deletes a code; past bookings keep the code text they used.
    public void delete(int id) {
        discountRepo.delete(find(id));
    }

    private DiscountCode find(int id) {
        return discountRepo.findById(id).orElseThrow(() -> NotFoundException.of("Discount code", id));
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }

    private void validate(DiscountType type, BigDecimal value, LocalDateTime validFrom, LocalDateTime validTo,
                          Integer maxUses, int usedCount) {
        if (type == DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("Percentage discount cannot exceed 100");
        }
        if (!validTo.isAfter(validFrom)) {
            throw new ValidationException("validTo must be after validFrom");
        }
        if (maxUses != null && maxUses < usedCount) {
            throw new ValidationException("maxUses cannot be below the " + usedCount + " redemptions already made");
        }
    }
}
