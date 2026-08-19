export type RealtimeMetric = {
  serverId: string;
  serverName: string;
  collectedAt: string;
  cpuPercent: number;
  memoryUsedBytes: number;
  memoryTotalBytes: number;
  diskUsedBytes: number;
  diskTotalBytes: number;
  uptimeSeconds: number;
  networkReceivedBytesPerSecond: number;
  networkSentBytesPerSecond: number;
};