package com.Omnibus.application.port.in;

import com.Omnibus.application.dto.AuthResponse;
import com.Omnibus.application.dto.LoginCommand;
import com.Omnibus.application.dto.RegisterCommand;
import com.Omnibus.application.dto.UserProfileResponse;

import java.util.UUID;

/**
 * Use-case port: user authentication (register + login + profile).
 */
public interface AuthUseCase {

    AuthResponse register(RegisterCommand command);

    AuthResponse login(LoginCommand command);

    /**
     * Get the current user's profile including their accounts.
     *
     * @param userId the authenticated user's ID (from JWT)
     */
    UserProfileResponse getProfile(UUID userId);
}
