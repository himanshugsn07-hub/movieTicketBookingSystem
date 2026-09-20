package com.himanshu.movieTicketBookingSystem.config;

import com.himanshu.movieTicketBookingSystem.entity.RefundPolicyConfig;
import com.himanshu.movieTicketBookingSystem.entity.RefundTier;
import com.himanshu.movieTicketBookingSystem.repository.RefundPolicyConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefundPolicySeeder implements ApplicationRunner {

    @Autowired
    private RefundPolicyConfigRepository policyRepo;

    // Creates the default "Standard" policy (full refund 24h or more before the show, none after) if none exist.
    @Override
    public void run(ApplicationArguments args) {
        if (policyRepo.count() == 0) {
            policyRepo.save(new RefundPolicyConfig("Standard",
                    List.of(new RefundTier(24, 100), new RefundTier(0, 0)), true));
        }
    }
}
