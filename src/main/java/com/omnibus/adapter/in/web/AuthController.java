package com.Omnibus.adapter.in.web;

import com.Omnibus.application.dto.AuthResponse;
import com.Omnibus.application.dto.LoginCommand;
import com.Omnibus.application.dto.RegisterCommand;
import com.Omnibus.application.dto.UserProfileResponse;
import com.Omnibus.application.port.in.AuthUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User registration, login, and profile")
public class AuthController {

        private final AuthUseCase authUseCase;

        public AuthController(AuthUseCase authUseCase) {
                this.authUseCase = authUseCase;
        }

        @PostMapping("/register")
        @Operation(summary = "Register a new user", description = "Creates a user with a default account (balance $10,000 for demo). Returns a JWT.", responses = {
                        @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Validation error"),
                        @ApiResponse(responseCode = "409", description = "Username or email already taken")
        })
        public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterCommand command) {
                AuthResponse response = authUseCase.register(command);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PostMapping("/login")
        @Operation(summary = "Login", description = "Authenticates a user and returns a JWT.", responses = {
                        @ApiResponse(responseCode = "200", description = "Login successful", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Invalid credentials")
        })
        public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginCommand command) {
                AuthResponse response = authUseCase.login(command);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/me")
        @Operation(summary = "Get current user profile", description = "Returns the authenticated user's profile including their accounts.", responses = {
                        @ApiResponse(responseCode = "200", description = "Profile retrieved", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
                        @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UUID userId) {
                UserProfileResponse profile = authUseCase.getProfile(userId);
                return ResponseEntity.ok(profile);
        }
}
