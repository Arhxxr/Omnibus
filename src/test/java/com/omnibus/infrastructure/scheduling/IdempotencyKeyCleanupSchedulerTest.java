package com.Omnibus.infrastructure.scheduling;

import com.Omnibus.application.port.out.IdempotencyStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the scheduled idempotency key cleanup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IdempotencyKeyCleanupScheduler")
class IdempotencyKeyCleanupSchedulerTest {

    @Mock
    private IdempotencyStorePort idempotencyStore;

    @InjectMocks
    private IdempotencyKeyCleanupScheduler scheduler;

    @Test
    @DisplayName("purgeExpiredKeys calls deleteExpired on the store")
    void purgeCallsDeleteExpired() {
        when(idempotencyStore.deleteExpired()).thenReturn(5);

        scheduler.purgeExpiredKeys();

        verify(idempotencyStore, times(1)).deleteExpired();
    }

    @Test
    @DisplayName("purgeExpiredKeys works when no keys are expired")
    void purgeWithNoExpiredKeys() {
        when(idempotencyStore.deleteExpired()).thenReturn(0);

        scheduler.purgeExpiredKeys();

        verify(idempotencyStore, times(1)).deleteExpired();
    }
}
