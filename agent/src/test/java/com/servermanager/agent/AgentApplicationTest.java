package com.servermanager.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class AgentApplicationTest {
    @Test
    void firstNetworkSampleHasZeroRates() {
        var rates = AgentApplication.calculateNetworkRates(
                null, 10_000, 5_000, Instant.parse("2026-08-08T12:00:00Z"));

        assertEquals(0, rates.receivedBytesPerSecond());
        assertEquals(0, rates.sentBytesPerSecond());
    }

    @Test
    void calculatesRatesUsingActualElapsedTime() {
        var previous = new AgentApplication.NetworkSample(
                10_000, 4_000, Instant.parse("2026-08-08T12:00:00Z"));

        var rates = AgentApplication.calculateNetworkRates(
                previous, 20_000, 6_500, Instant.parse("2026-08-08T12:00:05Z"));

        assertEquals(2_000, rates.receivedBytesPerSecond());
        assertEquals(500, rates.sentBytesPerSecond());
    }

    @Test
    void resetCounterDoesNotProduceNegativeRates() {
        var previous = new AgentApplication.NetworkSample(
                10_000, 4_000, Instant.parse("2026-08-08T12:00:00Z"));

        var rates = AgentApplication.calculateNetworkRates(
                previous, 100, 50, Instant.parse("2026-08-08T12:00:05Z"));

        assertEquals(0, rates.receivedBytesPerSecond());
        assertEquals(0, rates.sentBytesPerSecond());
    }

    @Test
    void diskUsageIsAlwaysWithinTotal() {
        assertEquals(600, AgentApplication.usedBytes(1_000, 400));
        assertEquals(1_000, AgentApplication.usedBytes(1_000, -1));
        assertEquals(0, AgentApplication.usedBytes(1_000, 2_000));
        assertEquals(0, AgentApplication.usedBytes(0, 0));
    }
}
