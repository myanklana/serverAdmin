package com.servermanager.agent.process;

import java.util.List;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

public final class ProcessCollector {
    private final OperatingSystem os;
    public ProcessCollector(SystemInfo system) { this.os = system.getOperatingSystem(); }
    public List<ProcessInfo> collect() {
        return os.getProcesses(null, OperatingSystem.ProcessSorting.CPU_DESC, 250).stream()
                .map(p -> new ProcessInfo(p.getProcessID(), p.getName(), Math.max(0, p.getProcessCpuLoadCumulative() * 100), Math.max(0, p.getResidentSetSize()), p.getUser()))
                .toList();
    }
}
