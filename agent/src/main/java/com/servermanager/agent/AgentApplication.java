package com.servermanager.agent;

import java.net.NetworkInterface;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import oshi.SystemInfo;
import oshi.hardware.NetworkIF;
import oshi.software.os.OSFileStore;

public final class AgentApplication {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static volatile long[] previousCpuTicks;
    private static volatile NetworkSample previousNetworkSample;

    private AgentApplication() {
    }

    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("config.json");
        Config config = JSON.readValue(Files.readString(configPath), Config.class);
        if (config.server() == null || config.token() == null || config.token().length() < 32) {
            throw new IllegalArgumentException("config.json exige server e token com ao menos 32 caracteres");
        }

        SystemInfo system = new SystemInfo();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> send(client, config, system), 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }

    private static void send(HttpClient client, Config config, SystemInfo system) {
        try {
            MetricPayload payload = collect(system);
            HttpRequest request = HttpRequest
                    .newBuilder(URI.create(config.server().replaceAll("/$", "") + "/api/agent/metrics"))
                    .header("Content-Type", "application/json")
                    .header("X-Agent-Token", config.token())
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload)))
                    .timeout(Duration.ofSeconds(15))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                System.err.println("API respondeu " + response.statusCode());
            }
        } catch (Exception ex) {
            System.err.println("Falha ao enviar métricas: " + ex.getMessage());
        }
    }

    private static MetricPayload collect(SystemInfo system) {
        var os = system.getOperatingSystem();
        var hardware = system.getHardware();
        long[] currentCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
        double cpu = previousCpuTicks == null ? 0d
                : hardware.getProcessor().getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100d;
        previousCpuTicks = currentCpuTicks;

        long diskTotal = 0;
        long diskUsed = 0;
        for (OSFileStore store : os.getFileSystem().getFileStores()) {
            long total = store.getTotalSpace();
            if (total > 0 && shouldInclude(store)) {
                diskTotal += total;
                diskUsed += usedBytes(total, store.getUsableSpace());
            }
        }

        long received = 0;
        long sent = 0;
        for (NetworkIF network : hardware.getNetworkIFs()) {
            network.updateAttributes();
            if (shouldInclude(network)) {
                received += Math.max(0, network.getBytesRecv());
                sent += Math.max(0, network.getBytesSent());
            }
        }

        Instant collectedAt = Instant.now();
        NetworkRates rates = calculateNetworkRates(previousNetworkSample, received, sent, collectedAt);
        previousNetworkSample = new NetworkSample(received, sent, collectedAt);

        return new MetricPayload(os.getNetworkParams().getHostName(), collectedAt, Math.max(0, cpu),
                hardware.getMemory().getTotal() - hardware.getMemory().getAvailable(), hardware.getMemory().getTotal(),
                diskUsed, diskTotal, os.toString(), os.getVersionInfo().toString(), System.getProperty("os.arch"),
                os.getSystemUptime(), received, sent, rates.receivedBytesPerSecond(), rates.sentBytesPerSecond());
    }

    static long usedBytes(long total, long usable) {
        if (total <= 0) {
            return 0;
        }
        long normalizedUsable = Math.max(0, Math.min(usable, total));
        return total - normalizedUsable;
    }

    static NetworkRates calculateNetworkRates(
            NetworkSample previous, long currentReceived, long currentSent, Instant collectedAt) {
        if (previous == null || collectedAt == null) {
            return NetworkRates.ZERO;
        }

        long elapsedMillis = Duration.between(previous.collectedAt(), collectedAt).toMillis();
        long receivedDifference = currentReceived - previous.receivedBytes();
        long sentDifference = currentSent - previous.sentBytes();
        if (elapsedMillis <= 0 || receivedDifference < 0 || sentDifference < 0) {
            return NetworkRates.ZERO;
        }

        return new NetworkRates(
                bytesPerSecond(receivedDifference, elapsedMillis),
                bytesPerSecond(sentDifference, elapsedMillis));
    }

    private static long bytesPerSecond(long difference, long elapsedMillis) {
        double rate = difference * 1000.0d / elapsedMillis;
        return rate >= Long.MAX_VALUE ? Long.MAX_VALUE : (long) rate;
    }

    private static boolean shouldInclude(OSFileStore store) {
        String name = store.getName().toLowerCase();
        String mount = store.getMount().toLowerCase();
        String type = store.getType().toLowerCase();
        return !type.equals("tmpfs") && !type.equals("devtmpfs") && !type.equals("proc")
                && !type.equals("sysfs") && !type.equals("squashfs")
                && !name.contains("loop") && !name.contains("snap") && !mount.startsWith("/snap");
    }

    private static boolean shouldInclude(NetworkIF network) {
        try {
            NetworkInterface systemInterface = NetworkInterface.getByName(network.getName());
            return systemInterface == null || (systemInterface.isUp() && !systemInterface.isLoopback());
        } catch (Exception ignored) {
            return !network.getName().toLowerCase().startsWith("lo");
        }
    }

    public record Config(String server, String token) {
    }

    public record MetricPayload(String hostname, Instant collectedAt, double cpuPercent, long memoryUsedBytes,
            long memoryTotalBytes, long diskUsedBytes, long diskTotalBytes, String operatingSystem, String kernel,
            String architecture, long uptimeSeconds, long networkReceivedBytes, long networkSentBytes,
            long networkReceivedBytesPerSecond, long networkSentBytesPerSecond) {
    }

    public record NetworkSample(long receivedBytes, long sentBytes, Instant collectedAt) {
    }

    public record NetworkRates(long receivedBytesPerSecond, long sentBytesPerSecond) {
        private static final NetworkRates ZERO = new NetworkRates(0, 0);
    }
}
