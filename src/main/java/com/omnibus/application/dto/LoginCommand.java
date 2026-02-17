package com.Omnibus.application.dto;

/**
 * Command for user login.
 */
public record LoginCommand(
        String username,
        String password
) {
}
