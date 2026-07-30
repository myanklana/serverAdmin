package adminServer.mvp.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdminControllerTest {

    @Test
    void shouldReturnApplicationAndJavaVersion() {
        AdminController controller = new AdminController("0.0.1-SNAPSHOT");

        AdminController.VersionResponse response = controller.getVersion();

        assertEquals("0.0.1-SNAPSHOT", response.applicationVersion());
        assertEquals(System.getProperty("java.version"), response.javaVersion());
    }
}
