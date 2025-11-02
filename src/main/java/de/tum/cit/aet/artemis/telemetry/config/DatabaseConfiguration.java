package de.tum.cit.aet.artemis.telemetry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories({ "de.tum.cit.aet.artemis.telemetry" })
@EnableTransactionManagement
public class DatabaseConfiguration {
}
