package com.Omnibus;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Tag("integration")
class OmnibusApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // Verifies Spring context starts successfully with Testcontainers PostgreSQL
    }
}
