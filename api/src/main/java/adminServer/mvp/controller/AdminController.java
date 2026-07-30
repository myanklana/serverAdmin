package adminServer.mvp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final String applicationVersion;

    public AdminController(@Value("${app.version:0.0.1-SNAPSHOT}") String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    @GetMapping("/status")
    public String getStatus() {
        return "Admin server is running.";
    }

    @GetMapping("/version")
    public VersionResponse getVersion() {
        return new VersionResponse(applicationVersion, System.getProperty("java.version"));
    }

    public record VersionResponse(String applicationVersion, String javaVersion) {
    }
}
