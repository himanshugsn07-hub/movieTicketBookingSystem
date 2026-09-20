package com.himanshu.movieTicketBookingSystem.config;

import com.himanshu.movieTicketBookingSystem.entity.User;
import com.himanshu.movieTicketBookingSystem.enums.Role;
import com.himanshu.movieTicketBookingSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    // Creates the configured admin account on startup if it does not exist yet.
    @Override
    public void run(ApplicationArguments args) {
        if (userRepo.findByUsername(adminUsername).isEmpty()) {
            userRepo.save(new User(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
        }
    }
}
