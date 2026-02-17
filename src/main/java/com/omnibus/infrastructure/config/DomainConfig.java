package com.Omnibus.infrastructure.config;

import com.Omnibus.domain.service.TransferDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers pure domain services as Spring beans.
 * Domain services have no Spring annotations — they're wired here.
 */
@Configuration
public class DomainConfig {

    @Bean
    public TransferDomainService transferDomainService() {
        return new TransferDomainService();
    }
}
