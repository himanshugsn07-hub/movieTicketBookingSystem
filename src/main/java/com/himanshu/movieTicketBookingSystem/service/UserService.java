package com.himanshu.movieTicketBookingSystem.service;

import com.himanshu.movieTicketBookingSystem.entity.User;
import com.himanshu.movieTicketBookingSystem.enums.Role;
import com.himanshu.movieTicketBookingSystem.exception.ConflictException;
import com.himanshu.movieTicketBookingSystem.exception.ValidationException;
import com.himanshu.movieTicketBookingSystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Registers a new CUSTOMER account with a hashed password; the username must be unused.
    public User register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.length() < 6) {
            throw new ValidationException("Username is required and password must be at least 6 characters");
        }
        if (userRepo.findByUsername(username.trim()).isPresent()) {
            throw new ConflictException("Username already taken");
        }
        return userRepo.save(new User(username.trim(), passwordEncoder.encode(password), Role.CUSTOMER));
    }
}
