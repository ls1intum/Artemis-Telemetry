package de.tum.cit.ase.artemistelemetry.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories({ "de.tum.cit.ase.artemistelemetry" })
@EnableTransactionManagement
public class DatabaseConfiguration {
}