package com.servermanager.agent.command;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SystemOperations {
    private static final int MAX_OUTPUT = 512 * 1024;
    private final ObjectMapper json;
    private final boolean terminalEnabled;
    private final Set<String> allowedCommands;
    private final List<Path> allowedPaths;

    public SystemOperations(ObjectMapper json, boolean terminalEnabled, List<String> allowedCommands, List<String> allowedPaths) {
        this.json = json; this.terminalEnabled = terminalEnabled;
        this.allowedCommands = Set.copyOf(allowedCommands == null ? List.of() : allowedCommands);
        this.allowedPaths = (allowedPaths == null ? List.<String>of() : allowedPaths).stream().map(Path::of).map(Path::toAbsolutePath).map(Path::normalize).toList();
    }
    public String execute(AgentCommandType type, JsonNode payload) throws Exception {
        return switch (type) {
            case LIST_SERVICES -> run(List.of("systemctl", "list-units", "--type=service", "--all", "--no-pager", "--plain"), Duration.ofSeconds(15));
            case CONTROL_SERVICE -> run(List.of("systemctl", action(payload), payload.path("name").asText()), Duration.ofSeconds(20));
            case LIST_CONTAINERS -> run(List.of("docker", "ps", "-a", "--format", "{{json .}}"), Duration.ofSeconds(15));
            case CONTROL_CONTAINER -> run(List.of("docker", action(payload), container(payload)), Duration.ofSeconds(30));
            case CONTAINER_LOGS -> run(List.of("docker", "logs", "--tail", Integer.toString(lines(payload)), container(payload)), Duration.ofSeconds(20));
            case SYSTEM_LOGS -> run(List.of("journalctl", "--no-pager", "-n", Integer.toString(lines(payload)), "-o", "short-iso"), Duration.ofSeconds(20));
            case RUN_TERMINAL_COMMAND -> terminal(payload.path("command").asText());
            case WRITE_FILE -> writeFile(payload);
            case READ_FILE -> readFile(payload);
            case CHECK_UPDATE -> json.writeValueAsString(Map.of("version", "0.0.1", "automaticUpdate", false));
            default -> throw new IllegalArgumentException("Operação de sistema desconhecida");
        };
    }
    private String action(JsonNode p) { String action = p.path("action").asText(); if (!Set.of("start", "stop", "restart").contains(action)) throw new IllegalArgumentException("Ação inválida"); return action; }
    private String container(JsonNode p) { String id = p.path("container").asText(); if (!id.matches("[a-zA-Z0-9_.-]{1,128}")) throw new IllegalArgumentException("Container inválido"); return id; }
    private int lines(JsonNode p) { int value = p.path("lines").asInt(200); return Math.max(1, Math.min(value, 2000)); }
    private String terminal(String command) throws Exception {
        if (!terminalEnabled) throw new SecurityException("Terminal remoto desativado na configuração do agente");
        List<String> args = Arrays.stream(command.trim().split("\\s+")).filter(s -> !s.isBlank()).toList();
        if (args.isEmpty() || !allowedCommands.contains(args.getFirst())) throw new SecurityException("Executável não autorizado");
        return run(args, Duration.ofSeconds(20));
    }
    private String writeFile(JsonNode p) throws IOException {
        Path target = authorized(p.path("path").asText()); byte[] content = Base64.getDecoder().decode(p.path("contentBase64").asText());
        if (content.length > 1024 * 1024) throw new IllegalArgumentException("Arquivo maior que 1 MiB");
        Files.createDirectories(target.getParent()); Path temporary = Files.createTempFile(target.getParent(), ".server-manager-", ".tmp");
        try { Files.write(temporary, content); try { Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); } catch (AtomicMoveNotSupportedException e) { Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING); } }
        finally { Files.deleteIfExists(temporary); }
        return jsonValue(Map.of("path", target.toString(), "bytes", content.length));
    }
    private String readFile(JsonNode p) throws IOException {
        Path path = authorized(p.path("path").asText()); long size = Files.size(path); if (size > 1024 * 1024) throw new IllegalArgumentException("Arquivo maior que 1 MiB");
        return jsonValue(Map.of("path", path.toString(), "contentBase64", Base64.getEncoder().encodeToString(Files.readAllBytes(path))));
    }
    private Path authorized(String raw) throws IOException {
        Path path = Path.of(raw).toAbsolutePath().normalize();
        if (allowedPaths.isEmpty() || allowedPaths.stream().noneMatch(path::startsWith)) throw new SecurityException("Caminho fora das raízes autorizadas");
        return path;
    }
    private String run(List<String> command, Duration timeout) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Thread reader = Thread.ofVirtual().start(() -> { try (InputStream in = process.getInputStream()) { in.transferTo(new LimitedOutputStream(output, MAX_OUTPUT)); } catch (IOException ignored) { } });
        if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) { process.destroyForcibly(); throw new IOException("Operação excedeu o tempo limite"); }
        reader.join(1000); String text = output.toString(StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new IOException(text.isBlank() ? "Processo retornou código " + process.exitValue() : text);
        return text;
    }
    private String jsonValue(Object value) throws IOException { return json.writeValueAsString(value); }
    private static final class LimitedOutputStream extends OutputStream {
        private final OutputStream delegate; private final int limit; private int written;
        LimitedOutputStream(OutputStream delegate, int limit) { this.delegate = delegate; this.limit = limit; }
        public void write(int value) throws IOException { if (written++ < limit) delegate.write(value); }
        public void write(byte[] bytes, int offset, int length) throws IOException { int accepted = Math.min(length, Math.max(0, limit - written)); if (accepted > 0) delegate.write(bytes, offset, accepted); written += length; }
    }
}
