package com.himanshu.movieTicketBookingSystem.entity;

import com.himanshu.movieTicketBookingSystem.enums.Role;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected User() {
    }

    public User(String username, String passwordHash, Role role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // Returns the user id.
    public int getId() {
        return id;
    }

    // Returns the unique login name.
    public String getUsername() {
        return username;
    }

    // Returns the hashed password.
    public String getPasswordHash() {
        return passwordHash;
    }

    // Returns the user's role.
    public Role getRole() {
        return role;
    }
}
