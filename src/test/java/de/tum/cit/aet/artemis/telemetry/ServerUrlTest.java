package de.tum.cit.aet.artemis.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import de.tum.cit.aet.artemis.telemetry.service.ServerUrl;

class ServerUrlTest {
    @ParameterizedTest
    @CsvSource({"HTTPS://EXAMPLE.ORG:443/,https://example.org", "http://Example.org:80,http://example.org",
            "https://example.org/Artemis/,https://example.org/Artemis", "http://localhost:8080/,http://localhost:8080",
            "http://[::1]:8080/,http://[::1]:8080", "https://example.org/A%2Fb,https://example.org/A%2Fb", "https://example.org/path//,https://example.org/path"})
    void canonicalizesOnlyEquivalentUrls(String value, String expected) {
        assertThat(ServerUrl.canonicalize(value)).isEqualTo(expected);
        assertThat(ServerUrl.canonicalize(ServerUrl.canonicalize(value))).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "example.org", "ftp://example.org", "https://user:secret@example.org", "https://example.org?x=1",
            "https://example.org/#fragment", "https:///missing-host", "https://example.org:99999", "https://example.org/path with spaces"})
    void rejectsInvalidUrls(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> ServerUrl.canonicalize(value));
    }
}
