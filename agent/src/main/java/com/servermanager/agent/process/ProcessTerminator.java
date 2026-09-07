package com.servermanager.agent.process;
import java.time.Duration;
public final class ProcessTerminator {
    public void terminate(long pid) throws InterruptedException {
        ProcessPolicy.validate(pid);
        ProcessHandle process = ProcessHandle.of(pid).orElseThrow(() -> new IllegalArgumentException("Processo não encontrado"));
        process.destroy();
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (process.isAlive() && System.nanoTime() < deadline) Thread.sleep(100);
        if (process.isAlive()) process.destroyForcibly();
    }
}
