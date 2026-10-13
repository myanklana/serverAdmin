package adminServer.mvp.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import adminServer.mvp.user.User;
import adminServer.mvp.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "app.registration.open=false",
        "spring.datasource.url=jdbc:h2:mem:registration-policy;MODE=PostgreSQL;DB_CLOSE_DELAY=-1" })
class RegistrationPolicyTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository users;

    @Test
    void registrationIsClosedOnceAnAccountExists() throws Exception {
        users.save(new User("existing-" + UUID.randomUUID().toString().substring(0, 8), "hash"));

        mvc.perform(get("/setup-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationOpen").value(false))
                .andExpect(jsonPath("$.firstUser").value(false));

        mvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"intruder\",\"password\":\"password-123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidAgentTokenReturnsUnauthorizedProblemDetail() throws Exception {
        mvc.perform(post("/api/agent/metrics")
                .header("X-Agent-Token", "not-a-valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"hostname":"h","cpuPercent":1,"memoryUsedBytes":1,"memoryTotalBytes":2,
                         "diskUsedBytes":1,"diskTotalBytes":2,"operatingSystem":"os","kernel":"k",
                         "architecture":"a","uptimeSeconds":1,"networkReceivedBytes":0,"networkSentBytes":0,
                         "networkReceivedBytesPerSecond":0,"networkSentBytesPerSecond":0}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unvalidatedHistoryIngestionRouteNoLongerExists() throws Exception {
        mvc.perform(post("/api/agent/metrics/history")
                .header("X-Agent-Token", "x")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
