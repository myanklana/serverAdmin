package com.servermanager.agent.process;
public record ProcessInfo(int pid, String name, double cpuPercent, long memoryBytes, String user) { }
