package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.DiscountCode;
import com.himanshu.movieTicketBookingSystem.enums.DiscountType;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.DiscountCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscountCodeServiceTest {

    private static final LocalDateTime FROM = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 2, 1, 0, 0);

    @Mock DiscountCodeRepository discountRepo;

    private DiscountCodeService service;

    @BeforeEach
    void setUp() {
        service = new DiscountCodeService(discountRepo);
    }

    @Test
    void createRejectsPercentAboveHundredAndAnEmptyValidityWindow() {
        assertThatThrownBy(() -> service.create("BIG", DiscountType.PERCENT, new BigDecimal("101"), FROM, TO, null, true))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.create("BAD", DiscountType.FLAT, BigDecimal.TEN, TO, FROM, null, true))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createStoresTheCodeUpperCaseTrimmedAndRejectsADuplicate() {
        when(discountRepo.findByCode("SAVE20")).thenReturn(Optional.empty(), Optional.of(mock()));
        when(discountRepo.save(any(DiscountCode.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscountCode created = service.create(" save20 ", DiscountType.PERCENT, BigDecimal.TEN, FROM, TO, null, true);

        assertThat(created.getCode()).isEqualTo("SAVE20");
        assertThatThrownBy(() -> service.create("Save20", DiscountType.PERCENT, BigDecimal.TEN, FROM, TO, null, true))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateCannotLowerMaxUsesBelowTheRedemptionsAlreadyMade() {
        DiscountCode code = new DiscountCode("SAVE20", DiscountType.PERCENT, BigDecimal.TEN, FROM, TO, 10, true);
        code.recordUse();
        code.recordUse();
        code.recordUse();
        when(discountRepo.findById(1)).thenReturn(Optional.of(code));

        assertThatThrownBy(() -> service.update(1, "SAVE20", DiscountType.PERCENT, BigDecimal.TEN, FROM, TO, 2, true))
                .isInstanceOf(ValidationException.class);
    }

    private static DiscountCode mock() {
        return new DiscountCode("SAVE20", DiscountType.PERCENT, BigDecimal.TEN, FROM, TO, null, true);
    }
}
