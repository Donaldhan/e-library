package com.elibrary.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    /** Injected everywhere time is needed so tests can pin "now" without touching the database. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
