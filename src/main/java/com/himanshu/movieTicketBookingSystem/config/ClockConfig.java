package com.himanshu.movieTicketBookingSystem.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    // The single time source for the application; tests can replace it with a fixed or adjustable clock.
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
