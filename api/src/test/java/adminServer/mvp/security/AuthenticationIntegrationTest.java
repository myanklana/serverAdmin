package adminServer.mvp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void protectedEndpointWithoutTokenReturnsProblemDetail() throws Exception {
        mvc.perform(get("/api/servers"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Não autenticado"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").exists())
                .andExpect(jsonPath("$.instance").value("/api/servers"));
    }

    @Test
    void malformedOrExpiredLikeTokenReturnsProblemDetail() throws Exception {
        mvc.perform(get("/api/servers").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Não autenticado"));
    }

    @Test
    void userCannotReadOrDeleteAnotherUsersServer() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = login("owner-" + suffix);
        String otherUserToken = login("other-" + suffix);

        MvcResult created = mvc.perform(post("/api/servers")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"server-%s","ip":"192.168.10.10","port":8081,"token":"agent-token-%s-123456789012345678901234567890"}
                        """.formatted(suffix, suffix)))
                .andExpect(status().isCreated())
                .andReturn();

        String serverId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("server").get("id").asText();

        mvc.perform(get("/api/servers/" + serverId)
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/api/servers/" + serverId)
                .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/servers/" + serverId)
                .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
    }

    @Test
    void serverListingOnlyContainsServersOwnedByAuthenticatedUser() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String ownerToken = login("list-owner-" + suffix);
        String otherUserToken = login("list-other-" + suffix);

        createServer(ownerToken, "owner-server-" + suffix, "192.168.10.11", suffix + "a");
        createServer(otherUserToken, "other-server-" + suffix, "192.168.10.12", suffix + "b");

        mvc.perform(get("/api/servers").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'owner-server-%s')]".formatted(suffix)).isNotEmpty())
                .andExpect(jsonPath("$[?(@.name == 'other-server-%s')]".formatted(suffix)).isEmpty());
    }

    private String login(String username) throws Exception {
        String password = "password-123";
        mvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isCreated());

        MvcResult result = mvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private void createServer(String jwt, String name, String ip, String tokenSuffix) throws Exception {
        mvc.perform(post("/api/servers")
                .header("Authorization", "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","ip":"%s","port":8081,"token":"agent-token-%s-123456789012345678901234567890"}
                        """.formatted(name, ip, tokenSuffix)))
                .andExpect(status().isCreated());
    }
}
