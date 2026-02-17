package com.Omnibus.application.service;

import com.Omnibus.application.dto.AuthResponse;
import com.Omnibus.application.dto.LoginCommand;
import com.Omnibus.application.dto.RegisterCommand;
import com.Omnibus.application.port.out.AccountRepositoryPort;
import com.Omnibus.application.port.out.PasswordEncoderPort;
import com.Omnibus.application.port.out.TokenProviderPort;
import com.Omnibus.application.port.out.UserRepositoryPort;
import com.Omnibus.domain.exception.DomainException;
import com.Omnibus.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthApplicationService}.
 * Verifies registration, login, password hashing, JWT generation,
 * duplicate detection, and invalid credential handling.
 */
@Tag("unit")
class AuthApplicationServiceTest {

    private UserRepositoryPort userRepository;
    private AccountRepositoryPort accountRepository;
    private PasswordEncoderPort passwordEncoder;
    private TokenProviderPort tokenProvider;
    private AuthApplicationService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        accountRepository = mock(AccountRepositoryPort.class);
        passwordEncoder = mock(PasswordEncoderPort.class);
        tokenProvider = mock(TokenProviderPort.class);
        authService = new AuthApplicationService(
                userRepository, accountRepository, passwordEncoder, tokenProvider);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        private final RegisterCommand command =
                new RegisterCommand("alice", "alice@example.com", "SecureP@ss1");

        @BeforeEach
        void stubDefaults() {
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(accountRepository.existsByAccountNumber(anyString())).thenReturn(false);
            when(tokenProvider.generateToken(any(UUID.class), anyString())).thenReturn("jwt.token.here");
            when(tokenProvider.getExpirationMs()).thenReturn(900_000L);
        }

        @Test
        @DisplayName("should register user and return auth response with JWT")
        void shouldRegisterSuccessfully() {
            AuthResponse response = authService.register(command);

            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo("alice");
            assertThat(response.token()).isEqualTo("jwt.token.here");
            assertThat(response.expiresInMs()).isEqualTo(900_000L);
            assertThat(response.userId()).isNotNull();
        }

        @Test
        @DisplayName("should hash password before persisting user")
        void shouldHashPassword() {
            authService.register(command);

            verify(passwordEncoder).encode("SecureP@ss1");

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$hashed");
        }

        @Test
        @DisplayName("should create default account with $10,000 USD balance")
        void shouldCreateDefaultAccount() {
            authService.register(command);

            var accountCaptor = ArgumentCaptor.forClass(
                    com.Omnibus.domain.model.Account.class);
            verify(accountRepository).save(accountCaptor.capture());

            var account = accountCaptor.getValue();
            assertThat(account.getBalance().getAmount())
                    .isEqualByComparingTo("10000.0000");
            assertThat(account.getBalance().getCurrency()).isEqualTo("USD");
            assertThat(account.getStatus())
                    .isEqualTo(com.Omnibus.domain.model.AccountStatus.ACTIVE);
            assertThat(account.getAccountNumber()).hasSize(10);
        }

        @Test
        @DisplayName("should generate JWT with user ID and username")
        void shouldGenerateJwt() {
            AuthResponse response = authService.register(command);

            verify(tokenProvider).generateToken(response.userId(), "alice");
        }

        @Test
        @DisplayName("should reject duplicate username")
        void shouldRejectDuplicateUsername() {
            when(userRepository.existsByUsername("alice")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should reject duplicate email")
        void shouldRejectDuplicateEmail() {
            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(command))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should link account to saved user's ID")
        void shouldLinkAccountToUser() {
            authService.register(command);

            var userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            UUID savedUserId = userCaptor.getValue().getId();

            var accountCaptor = ArgumentCaptor.forClass(
                    com.Omnibus.domain.model.Account.class);
            verify(accountRepository).save(accountCaptor.capture());
            assertThat(accountCaptor.getValue().getUserId()).isEqualTo(savedUserId);
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        private final UUID userId = UUID.randomUUID();
        private final User existingUser = new User(
                userId, "bob", "bob@example.com", "$2a$encoded", "USER");

        @BeforeEach
        void stubDefaults() {
            when(userRepository.findByUsername("bob")).thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches("correctPassword", "$2a$encoded")).thenReturn(true);
            when(tokenProvider.generateToken(userId, "bob")).thenReturn("login.jwt.token");
            when(tokenProvider.getExpirationMs()).thenReturn(900_000L);
        }

        @Test
        @DisplayName("should login with valid credentials and return JWT")
        void shouldLoginSuccessfully() {
            AuthResponse response = authService.login(
                    new LoginCommand("bob", "correctPassword"));

            assertThat(response.userId()).isEqualTo(userId);
            assertThat(response.username()).isEqualTo("bob");
            assertThat(response.token()).isEqualTo("login.jwt.token");
            assertThat(response.expiresInMs()).isEqualTo(900_000L);
        }

        @Test
        @DisplayName("should verify password via PasswordEncoderPort")
        void shouldVerifyPassword() {
            authService.login(new LoginCommand("bob", "correctPassword"));

            verify(passwordEncoder).matches("correctPassword", "$2a$encoded");
        }

        @Test
        @DisplayName("should reject non-existent username")
        void shouldRejectUnknownUser() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(
                    new LoginCommand("unknown", "anyPassword")))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("should reject wrong password")
        void shouldRejectWrongPassword() {
            when(passwordEncoder.matches("wrongPassword", "$2a$encoded")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(
                    new LoginCommand("bob", "wrongPassword")))
                    .isInstanceOf(DomainException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("should not reveal whether username or password was wrong")
        void shouldNotLeakAuthFailureReason() {
            // Wrong username
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
            Throwable unknownUser = catchThrowable(() ->
                    authService.login(new LoginCommand("unknown", "anyPassword")));

            // Wrong password
            when(passwordEncoder.matches("wrongPassword", "$2a$encoded")).thenReturn(false);
            Throwable wrongPassword = catchThrowable(() ->
                    authService.login(new LoginCommand("bob", "wrongPassword")));

            // Both should produce the same error message (security best practice)
            assertThat(unknownUser).hasMessage(wrongPassword.getMessage());
        }

        @Test
        @DisplayName("should generate JWT with correct user ID and username")
        void shouldGenerateJwtForLogin() {
            authService.login(new LoginCommand("bob", "correctPassword"));

            verify(tokenProvider).generateToken(userId, "bob");
        }
    }
}
