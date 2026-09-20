package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.entity.Show;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.InvalidStateException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import com.himanshu.movieTicketBookingSystem.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefundPolicyServiceTest {

    @Mock RefundPolicyConfigRepository policyRepo;

    private RefundPolicyService service;

    @BeforeEach
    void setUp() {
        service = new RefundPolicyService(policyRepo);
    }

    static Stream<Arguments> invalidTiers() {
        return Stream.of(
                Arguments.of("no catch-all tier at 0 hours", List.of(new RefundTier(24, 100))),
                Arguments.of("duplicate hours", List.of(new RefundTier(0, 0), new RefundTier(0, 10))),
                Arguments.of("refund rises as the show gets closer", List.of(new RefundTier(24, 10), new RefundTier(0, 50))),
                Arguments.of("percent above 100", List.of(new RefundTier(0, 150))),
                Arguments.of("negative hours", List.of(new RefundTier(0, 0), new RefundTier(-1, 10))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTiers")
    void createRejectsInvalidTierSets(String name, List<RefundTier> tiers) {
        assertThatThrownBy(() -> service.create("Bad", tiers)).isInstanceOf(ValidationException.class);

        verify(policyRepo, never()).save(any());
    }

    @Test
    void createSavesAValidNonDefaultPolicyAndRejectsADuplicateName() {
        List<RefundTier> tiers = List.of(new RefundTier(48, 100), new RefundTier(12, 50), new RefundTier(0, 0));
        when(policyRepo.findByName("Flexible")).thenReturn(Optional.empty(), Optional.of(TestData.standardPolicy()));
        when(policyRepo.save(any(RefundPolicyConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        RefundPolicyConfig created = service.create("Flexible", tiers);

        assertThat(created.isDefaultPolicy()).isFalse();
        assertThatThrownBy(() -> service.create("Flexible", tiers)).isInstanceOf(ConflictException.class);
    }

    @Test
    void theDefaultPolicyCannotBeDeleted() {
        RefundPolicyConfig standard = TestData.standardPolicy();
        when(policyRepo.findById(0)).thenReturn(Optional.of(standard));

        assertThatThrownBy(() -> service.delete(0)).isInstanceOf(ConflictException.class);

        verify(policyRepo, never()).delete(any());
    }

    @Test
    void makeDefaultMovesTheDefaultFlagToTheChosenPolicy() {
        RefundPolicyConfig standard = TestData.standardPolicy();
        RefundPolicyConfig flexible = new RefundPolicyConfig("Flexible", List.of(new RefundTier(0, 0)), false);
        ReflectionTestUtils.setField(standard, "id", 1);
        ReflectionTestUtils.setField(flexible, "id", 2);
        when(policyRepo.findById(2)).thenReturn(Optional.of(flexible));
        when(policyRepo.findAll()).thenReturn(List.of(standard, flexible));

        service.makeDefault(2);

        assertThat(standard.isDefaultPolicy()).isFalse();
        assertThat(flexible.isDefaultPolicy()).isTrue();
    }

    @Test
    void resolveForPrefersTheShowsOwnPolicyThenTheDefaultAndFailsWithoutEither() {
        Show show = TestData.show(TestData.NOW.plusDays(1));
        RefundPolicyConfig own = new RefundPolicyConfig("Own", List.of(new RefundTier(0, 0)), false);

        when(policyRepo.findByDefaultPolicyTrue()).thenReturn(Optional.of(TestData.standardPolicy()), Optional.empty());

        assertThat(service.resolveFor(show).getName()).as("default").isEqualTo("Standard");
        assertThatThrownBy(() -> service.resolveFor(show)).as("nothing configured").isInstanceOf(InvalidStateException.class);

        show.assignRefundPolicy(own);
        assertThat(service.resolveFor(show)).as("show's own").isSameAs(own);
    }
}
