package com.servermanager.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import oshi.SystemInfo;
import oshi.hardware.HWDiskStore;
import oshi.hardware.NetworkIF;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class AgentApplication {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule());
    private static volatile long[] previousCpuTicks;
    public static void main(String[] args) throws Exception {
        Path configPath = args.length > 0 ? Path.of(args[0]) : Path.of("config.json");
        Config config = JSON.readValue(Files.readString(configPath), Config.class);
        if (config.server() == null || config.token() == null || config.token().length() < 32) throw new IllegalArgumentException("config.json exige server e token com ao menos 32 caracteres");
        SystemInfo system = new SystemInfo(); HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> send(client, config, system), 0, 5, TimeUnit.SECONDS);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdown));
    }
    private static void send(HttpClient client, Config config, SystemInfo system) {
        try {
            MetricPayload payload = collect(system);
            HttpRequest request = HttpRequest.newBuilder(URI.create(config.server().replaceAll("/$", "") + "/api/agent/metrics"))
                    .header("Content-Type", "application/json").header("X-Agent-Token", config.token())
                    .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(payload))).timeout(Duration.ofSeconds(15)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) System.err.println("API respondeu " + response.statusCode());
        } catch (Exception ex) { System.err.println("Falha ao enviar métricas: " + ex.getMessage()); }
    }
    private static MetricPayload collect(SystemInfo system) {
        var os = system.getOperatingSystem(); var hardware = system.getHardware();
        long[] currentCpuTicks = hardware.getProcessor().getSystemCpuLoadTicks();
        double cpu = previousCpuTicks == null ? 0d : hardware.getProcessor().getSystemCpuLoadBetweenTicks(previousCpuTicks) * 100d;
        previousCpuTicks = currentCpuTicks;
        long diskTotal = 0, diskUsed = 0; for (HWDiskStore disk : hardware.getDiskStores()) { diskTotal += disk.getSize(); }
        for (var store : os.getFileSystem().getFileStores()) { diskUsed += Math.max(0, store.getTotalSpace() - store.getUsableSpace()); }
        long received = 0, sent = 0; for (NetworkIF network : hardware.getNetworkIFs()) { network.updateAttributes(); received += network.getBytesRecv(); sent += network.getBytesSent(); }
        return new MetricPayload(os.getNetworkParams().getHostName(), Instant.now(), Math.max(0, cpu), hardware.getMemory().getTotal() - hardware.getMemory().getAvailable(), hardware.getMemory().getTotal(), diskUsed, diskTotal, os.toString(), os.getVersionInfo().toString(), System.getProperty("os.arch"), os.getSystemUptime(), received, sent);
    }
    public record Config(String server, String token) { }
    public record MetricPayload(String hostname, Instant collectedAt, double cpuPercent, long memoryUsedBytes, long memoryTotalBytes, long diskUsedBytes, long diskTotalBytes, String operatingSystem, String kernel, String architecture, long uptimeSeconds, long networkReceivedBytes, long networkSentBytes) { }
}
