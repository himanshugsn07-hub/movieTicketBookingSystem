package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.User;
import com.himanshu.movieTicketBookingSystem.enums.Role;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // Registers a new CUSTOMER account with a hashed password; the username must be unused.
    public User register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < Constants.Security.MIN_PASSWORD_LENGTH) {
            throw new ValidationException("Username is required and password must be at least " + Constants.Security.MIN_PASSWORD_LENGTH + " characters");
        }
        if (userRepo.findByUsername(username.trim()).isPresent()) {
            throw new ConflictException("Username already taken");
        }
        return userRepo.save(new User(username.trim(), passwordEncoder.encode(password), Role.CUSTOMER));
    }
}
