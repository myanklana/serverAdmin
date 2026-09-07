package com.servermanager.agent.process;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ProcessPolicyTest {
    @Test void rejectsSystemAndCurrentProcesses() {
        assertThrows(SecurityException.class, () -> ProcessPolicy.validate(1));
        assertThrows(SecurityException.class, () -> ProcessPolicy.validate(ProcessHandle.current().pid()));
    }
    @Test void acceptsRegularPid() { assertDoesNotThrow(() -> ProcessPolicy.validate(Long.MAX_VALUE)); }
}
