package com.himanshu.movieTicketBookingSystem.dto;

import com.himanshu.movieTicketBookingSystem.constants.Constants;
import com.himanshu.movieTicketBookingSystem.entity.User;
import com.himanshu.movieTicketBookingSystem.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(@NotBlank String username, @NotBlank @Size(min = Constants.Security.MIN_PASSWORD_LENGTH, max = Constants.Security.MAX_PASSWORD_LENGTH) String password) {
    }

    public record UserResponse(int id, String username, Role role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getUsername(), user.getRole());
        }
    }
}
