package com.finbridge.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the Hibernate6 Jackson module so that controllers returning JPA entities
 * with lazy {@code @ManyToOne}/{@code @OneToMany} associations serialize cleanly.
 *
 * Without it, Jackson tries to serialize the Hibernate proxy's {@code ByteBuddyInterceptor}
 * and fails with a 500. With the default settings here, uninitialized lazy associations are
 * written as {@code null} (no forced N+1 loading), which is the desired behavior for the B2B
 * list endpoints (the client already knows its own organization context).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Module hibernate6Module() {
        return new Hibernate6Module();
    }
}
