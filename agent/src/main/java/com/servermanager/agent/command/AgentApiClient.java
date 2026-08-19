package com.servermanager.agent.command;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * AgentApiClient
 */
public final class AgentApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI cUri;
    private final String token;

    public AgentApiClient(HttpClient httpClient, String apiUrl, String token) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        this.cUri = URI.create(apiUrl.replaceAll("/$", "") + "/api/agent/command/claim");
        this.token = token;
    }

    public Optional<AgentCommandResponse> claimNext() throws IllegalAccessException, IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder(cUri).timeout(Duration.ofSeconds(5))
                .header("X-Agent-Token", token).header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 204) {
            return Optional.empty();
        }

        if (response.statusCode() != 200) {
            throw new IllegalAccessException("Falha ao buscar comando" + response.statusCode());
        }

        AgentCommandResponse command = objectMapper.readValue(response.body(), AgentCommandResponse.class);

        return Optional.of(command);

    }

}
