package com.Omnibus.application.service;

import com.Omnibus.application.dto.AccountDTO;
import com.Omnibus.application.dto.AuthResponse;
import com.Omnibus.application.dto.LoginCommand;
import com.Omnibus.application.dto.RegisterCommand;
import com.Omnibus.application.dto.UserProfileResponse;
import com.Omnibus.application.port.in.AuthUseCase;
import com.Omnibus.application.port.out.AccountRepositoryPort;
import com.Omnibus.application.port.out.PasswordEncoderPort;
import com.Omnibus.application.port.out.TokenProviderPort;
import com.Omnibus.application.port.out.UserRepositoryPort;
import com.Omnibus.domain.exception.DomainException;
import com.Omnibus.domain.model.Account;
import com.Omnibus.domain.model.AccountStatus;
import com.Omnibus.domain.model.Money;
import com.Omnibus.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthApplicationService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final AccountRepositoryPort accountRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort jwtTokenProvider;

    public AuthApplicationService(UserRepositoryPort userRepository,
            AccountRepositoryPort accountRepository,
            PasswordEncoderPort passwordEncoder,
            TokenProviderPort jwtTokenProvider) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterCommand command) {
        // Validate uniqueness
        if (userRepository.existsByUsername(command.username())) {
            throw new DomainException("Username already taken: " + command.username());
        }
        if (userRepository.existsByEmail(command.email())) {
            throw new DomainException("Email already registered: " + command.email());
        }

        // Create user
        User user = new User(
                UUID.randomUUID(),
                command.username(),
                command.email(),
                passwordEncoder.encode(command.password()),
                "USER");
        User savedUser = userRepository.save(user);

        // Create default account with initial balance
        Account account = new Account(
                UUID.randomUUID(),
                savedUser.getId(),
                generateAccountNumber(),
                Money.of("10000.0000", "USD"), // Starting balance for demo
                AccountStatus.ACTIVE);
        accountRepository.save(account);

        // Generate JWT
        String token = jwtTokenProvider.generateToken(savedUser.getId(), savedUser.getUsername());

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                token,
                jwtTokenProvider.getExpirationMs());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginCommand command) {
        User user = userRepository.findByUsername(command.username())
                .orElseThrow(() -> new DomainException("Invalid username or password"));

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            throw new DomainException("Invalid username or password");
        }

        String token = jwtTokenProvider.generateToken(user.getId(), user.getUsername());

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                token,
                jwtTokenProvider.getExpirationMs());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new DomainException("User not found: " + userId));

        List<AccountDTO> accounts = accountRepository.findByUserId(userId).stream()
                .map(account -> new AccountDTO(
                        account.getId(),
                        account.getUserId(),
                        account.getAccountNumber(),
                        account.getBalance().getAmount(),
                        account.getBalance().getCurrency(),
                        account.getStatus().name(),
                        account.getCreatedAt()))
                .toList();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                accounts);
    }

    private String generateAccountNumber() {
        // Generate a unique 10-digit account number
        String number;
        do {
            number = String.valueOf(1_000_000_000L + ThreadLocalRandom.current().nextLong(9_000_000_000L));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
