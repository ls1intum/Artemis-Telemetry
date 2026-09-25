package de.tum.cit.aet.artemis.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;

class TelemetryDTOTest {
    @Test
    void multiNodeIsIndependentOfTestServer() {
        var dto = JsonMapper.builder().build().readValue("""
                {"serverUrl":"https://example.org", "profiles":["prod"],
                 "isMultiNode":true, "isTestServer":false}
                """, TelemetryDTO.class);
        var telemetry = TelemetryDTO.to(dto);
        assertThat(telemetry.isMultiNode()).isTrue();
        assertThat(telemetry.isTestServer()).isFalse();
    }
}
