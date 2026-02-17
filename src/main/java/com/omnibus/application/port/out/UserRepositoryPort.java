package com.Omnibus.application.port.out;

import com.Omnibus.domain.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port for user persistence.
 */
public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
