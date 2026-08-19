package adminServer.mvp.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

class MetricRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void rejectsDiskUsageGreaterThanTotal() {
        var request = request(2_000, 1_000, 100, 50);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getMessage().contains("diskUsedBytes"));
    }

    @Test
    void rejectsNegativeNetworkRates() {
        var request = request(500, 1_000, -1, 50);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString()
                        .equals("networkReceivedBytesPerSecond"));
    }

    @Test
    void acceptsConsistentDiskAndNetworkValues() {
        var request = request(500, 1_000, 100, 50);

        assertThat(validator.validate(request)).isEmpty();
    }

    private MetricRequest request(
            long diskUsed, long diskTotal, long receivedRate, long sentRate) {
        return new MetricRequest(
                "server-01", Instant.parse("2026-08-08T12:00:00Z"), 10,
                500, 1_000, diskUsed, diskTotal, "Linux", "6.1", "amd64",
                100, 10_000, 5_000, receivedRate, sentRate);
    }
}
