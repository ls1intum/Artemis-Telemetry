package de.tum.cit.ase.artemistelemetry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class ArtemisTelemetryApplication {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtemisTelemetryApplication.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ArtemisTelemetryApplication.class);
        var context = app.run(args);
        Environment env = context.getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (StringUtils.isBlank(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
        log.info("""

                ----------------------------------------------------------
                \t'{}' is running! Access URLs:
                \tLocal:      {}://localhost:{}{}
                \tExternal:   {}://{}:{}{}
                ----------------------------------------------------------

                """, env.getProperty("spring.application.name"), protocol, serverPort, contextPath, protocol, hostAddress, serverPort, contextPath);
    }

}
