export type Metrics = { collectedAt: string; cpuPercent: number; memoryUsedBytes: number; memoryTotalBytes: number; diskUsedBytes: number; diskTotalBytes: number; operatingSystem: string; kernel: string; architecture: string; uptimeSeconds: number; networkReceivedBytes: number; networkSentBytes: number; networkReceivedBytesPerSecond: number; networkSentBytesPerSecond: number };
export type HistoricalMetric = Metrics & { serverId: string; serverName: string };
type Page<T> = { content: T[]; totalPages: number; last: boolean };
const HISTORY_PAGE_SIZE = 500;
const MAX_HISTORY_PAGES = 25;
const MAX_CHART_POINTS = 700;
export type Server = { id: string; name: string; hostname: string; ip: string; port: number; status: 'PENDING' | 'ONLINE' | 'OFFLINE'; lastSeen: string | null; latestMetrics: Metrics | null };
export type CreatedServer = { server: Server; agentToken: string };
const baseUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
export const apiUrl = baseUrl;
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...init.headers } });
  if (!response.ok) { const body = await response.json().catch(() => null); throw new Error(body?.message ?? 'Erro na requisição.'); }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}
export const register = (credentials: { username: string; password: string }) => request('/register', { method: 'POST', body: JSON.stringify(credentials) });
export const login = (credentials: { username: string; password: string }) => request<{ accessToken: string }>('/login', { method: 'POST', body: JSON.stringify(credentials) });
export const listServers = (token: string) => request<Server[]>('/api/servers', { headers: { Authorization: `Bearer ${token}` } });
export const createServer = (token: string, server: { name: string; ip: string; port: number }) => request<CreatedServer>('/api/servers', { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(server) });
export async function getMetricHistory(token: string, serverId: string, from: Date, to: Date) {
  const loadPage = (page: number) => {
    const query = new URLSearchParams({ from: from.toISOString(), to: to.toISOString(), page: String(page), size: '500' });
    return request<Page<HistoricalMetric>>(`/api/servers/${serverId}/metrics?${query}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
  };

  const first = await loadPage(0);
  if (first.totalPages <= 1) return downsample(first.content, MAX_CHART_POINTS);
  const pageIndexes = sampledPageIndexes(first.totalPages);
  const remaining = await Promise.all(pageIndexes.slice(1).map(loadPage));
  const metrics = [first, ...remaining]
    .flatMap(page => page.content)
    .sort((left, right) => left.collectedAt.localeCompare(right.collectedAt));
  return downsample(metrics, MAX_CHART_POINTS);
}

function sampledPageIndexes(totalPages: number) {
  const count = Math.min(totalPages, MAX_HISTORY_PAGES);
  return Array.from(new Set(Array.from({ length: count }, (_, index) =>
    Math.round(index * (totalPages - 1) / Math.max(count - 1, 1)))));
}

function downsample<T>(values: T[], maximum: number) {
  if (values.length <= maximum) return values;
  return Array.from({ length: maximum }, (_, index) =>
    values[Math.round(index * (values.length - 1) / (maximum - 1))]);
}
