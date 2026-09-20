package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundPolicyConfigRepository extends JpaRepository<RefundPolicyConfig, Integer> {

    // Finds a policy by its unique name.
    Optional<RefundPolicyConfig> findByName(String name);

    // Finds the default policy.
    Optional<RefundPolicyConfig> findByDefaultPolicyTrue();
}
