export type Server = { id: string; name: string; hostname: string; ip: string; port: number; status: 'PENDING' | 'ONLINE' | 'OFFLINE'; lastSeen: string | null };
const baseUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';
async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...init.headers } });
  if (!response.ok) { const body = await response.json().catch(() => null); throw new Error(body?.message ?? 'Erro na requisição.'); }
  return response.status === 204 ? undefined as T : response.json() as Promise<T>;
}
export const register = (credentials: { username: string; password: string }) => request('/register', { method: 'POST', body: JSON.stringify(credentials) });
export const login = (credentials: { username: string; password: string }) => request<{ accessToken: string }>('/login', { method: 'POST', body: JSON.stringify(credentials) });
export const listServers = (token: string) => request<Server[]>('/api/servers', { headers: { Authorization: `Bearer ${token}` } });
export const createServer = (token: string, server: { name: string; ip: string; token: string; port: number }) => request<Server>('/api/servers', { method: 'POST', headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(server) });
