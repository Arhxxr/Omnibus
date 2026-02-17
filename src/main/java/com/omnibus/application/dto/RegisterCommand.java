package com.Omnibus.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Command for user registration.
 */
public record RegisterCommand(
                @NotBlank(message = "Username is required") @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters") String username,

                @NotBlank(message = "Email is required") @Email(message = "Must be a valid email address") String email,

                @NotBlank(message = "Password is required") @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters") String password) {
}
