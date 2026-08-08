package adminServer.mvp.metrics;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record MetricRequest(@NotBlank String hostname, Instant collectedAt,
        @DecimalMin("0.0") @DecimalMax("100.0") double cpuPercent,
        @PositiveOrZero long memoryUsedBytes, @Positive long memoryTotalBytes,
        @PositiveOrZero long diskUsedBytes,
        @Positive long diskTotalBytes,
        @NotBlank String operatingSystem, @NotBlank String kernel, @NotBlank String architecture,
        @PositiveOrZero long uptimeSeconds,
        @PositiveOrZero long networkReceivedBytes, @PositiveOrZero long networkSentBytes,
        @PositiveOrZero long networkReceivedBytesPerSecond,
        @PositiveOrZero long networkSentBytesPerSecond) {
    @AssertTrue(message = "diskUsedBytes must not exceed diskTotalBytes")
    public boolean isDiskUsageValid() {
        return diskUsedBytes <= diskTotalBytes;
    }

    @AssertTrue(message = "memoryUsedBytes must not exceed memoryTotalBytes")
    public boolean isMemoryUsageValid() {
        return memoryUsedBytes <= memoryTotalBytes;
    }
}
