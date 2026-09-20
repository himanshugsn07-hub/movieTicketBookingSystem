package com.himanshu.movieTicketBookingSystem.config;

import com.himanshu.movieTicketBookingSystem.entity.User;
import com.himanshu.movieTicketBookingSystem.enums.Role;
import com.himanshu.movieTicketBookingSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminSeeder(UserRepository userRepo, PasswordEncoder passwordEncoder,
                       @Value("${app.admin.username}") String adminUsername,
                       @Value("${app.admin.password}") String adminPassword) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    // Creates the configured admin account on startup if it does not exist yet.
    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.findByUsername(adminUsername).isEmpty()) {
            userRepo.save(new User(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
        }
    }
}
