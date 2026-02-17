package com.Omnibus.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@DisplayName("User — domain invariants")
class UserTest {

    private static final UUID ID = UUID.randomUUID();

    @Test
    @DisplayName("creates valid user with all required fields")
    void validConstruction() {
        User user = new User(ID, "alice", "alice@example.com", "hashed", "USER");

        assertThat(user.getId()).isEqualTo(ID);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getEmail()).isEqualTo("alice@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo("USER");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejects null id")
    void nullId() {
        assertThatThrownBy(() -> new User(null, "alice", "a@b.com", "hash", "USER"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    @DisplayName("rejects null username")
    void nullUsername() {
        assertThatThrownBy(() -> new User(ID, null, "a@b.com", "hash", "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("rejects blank username")
    void blankUsername() {
        assertThatThrownBy(() -> new User(ID, "  ", "a@b.com", "hash", "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("username");
    }

    @Test
    @DisplayName("rejects null email")
    void nullEmail() {
        assertThatThrownBy(() -> new User(ID, "alice", null, "hash", "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("rejects blank email")
    void blankEmail() {
        assertThatThrownBy(() -> new User(ID, "alice", " ", "hash", "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("rejects null passwordHash")
    void nullPasswordHash() {
        assertThatThrownBy(() -> new User(ID, "alice", "a@b.com", null, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("passwordHash");
    }

    @Test
    @DisplayName("rejects null role")
    void nullRole() {
        assertThatThrownBy(() -> new User(ID, "alice", "a@b.com", "hash", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("role");
    }
}
