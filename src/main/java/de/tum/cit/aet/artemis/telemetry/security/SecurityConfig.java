package de.tum.cit.aet.artemis.telemetry.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${telemetry.user}")
    private String telemetryUser;

    @Value("${telemetry.password}")
    private String telemetryPassword;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
            // Preserve challenge-driven Basic clients; the SPA marks its requests to avoid a native password prompt.
            if (!"XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
                response.setHeader("WWW-Authenticate", "Basic realm=\"Artemis Telemetry\"");
            }
            response.setStatus(401);
        };
        http
                .csrf(csrf -> csrf
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/api/telemetry")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/telemetry").permitAll()
                        .requestMatchers("/api/auth/session", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .requestCache(cache -> cache.disable())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .httpBasic(basic -> basic.authenticationEntryPoint(entryPoint))
                .formLogin(login -> login.loginPage("/").loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> response.setStatus(401)))
                .logout(logout -> logout.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)));

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        var builder = User.builder().passwordEncoder(encoder::encode);
        UserDetails user = builder
                .username(telemetryUser)
                .password(telemetryPassword)
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(user);
    }
}
