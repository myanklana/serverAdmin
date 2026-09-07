package com.servermanager.agent.process;
public final class ProcessPolicy {
    private ProcessPolicy() { }
    public static void validate(long pid) {
        if (pid <= 1 || pid == ProcessHandle.current().pid()) throw new SecurityException("Processo protegido não pode ser encerrado");
    }
}
