package com.himanshu.movieTicketBookingSystem.repository;

import com.himanshu.movieTicketBookingSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    // Finds a user by their unique username.
    Optional<User> findByUsername(String username);
}
