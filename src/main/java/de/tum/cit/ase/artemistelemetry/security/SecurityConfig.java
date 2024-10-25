package de.tum.cit.ase.artemistelemetry.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${telemetry.user}") // Reads from environment or application.properties
    private String telemetryUser;

    @Value("${telemetry.password}") // Reads from environment or application.properties
    private String telemetryPassword;



    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // disable CSRF for simplicity; enable if needed
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/telemetry/**").authenticated() // secure telemetry endpoints
                        .anyRequest().permitAll() // allow other requests to be public
                )
                .httpBasic(httpBasic -> {}); // enable basic auth with lambda configuration

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
                .username(telemetryPassword)
                .password(telemetryPassword)
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}