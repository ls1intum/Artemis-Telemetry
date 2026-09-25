package de.tum.cit.aet.artemis.telemetry.web.rest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Session status and CSRF token for the same-origin dashboard. Credentials never leave the login request. */
@RestController
public class AuthResource {
    public record Session(boolean authenticated, String username, String csrfToken) { }

    @GetMapping("/api/auth/session")
    public Session session(Authentication authentication, CsrfToken csrfToken) {
        boolean authenticated = authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
        return new Session(authenticated, authenticated ? authentication.getName() : null, csrfToken.getToken());
    }
}
