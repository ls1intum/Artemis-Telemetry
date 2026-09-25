package de.tum.cit.aet.artemis.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import de.tum.cit.aet.artemis.telemetry.domain.TelemetryInstance;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryIngestionRepository;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryInstanceRepository;
import de.tum.cit.aet.artemis.telemetry.repository.TelemetryRepository;
import de.tum.cit.aet.artemis.telemetry.service.dto.TelemetryDTO;

class TelemetryIngestionRepositoryTest {
    @Test
    void earlierReceiptCannotMoveLastSeenBackwardsAfterWaitingForTheLock() {
        var instances = mock(TelemetryInstanceRepository.class);
        var startups = mock(TelemetryRepository.class);
        var instance = new TelemetryInstance();
        instance.setId(1L);
        // Simulate a later receipt already committed while this request waited for its row lock.
        var laterReceipt = ZonedDateTime.now().plusSeconds(5);
        instance.setFirstSeen(laterReceipt);
        instance.setLastSeen(laterReceipt);
        when(instances.findByServerUrl("https://example.org")).thenReturn(Optional.of(instance));
        var dto = JsonMapper.builder().build().readValue("{\"profiles\":[\"prod\"]}", TelemetryDTO.class);
        var saved = new TelemetryIngestionRepository(startups, instances).record(dto, "https://example.org");
        assertThat(instance.getLastSeen()).isEqualTo(laterReceipt);
        assertThat(instance.getFirstSeen().toInstant()).isEqualTo(saved.getTimestamp().toInstant());
    }
}
