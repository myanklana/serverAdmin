import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { useMetricStream } from './hooks/useMetricStream';
import { createServer, listServers, login, register, Server } from './services/api';
import type { RealtimeMetric } from './types/RealTimeMetric';
import './styles.css';

function App() {
  const [token, setToken] = useState(sessionStorage.getItem('accessToken'));
  const [servers, setServers] = useState<Server[]>([]);
  const serversRef = useRef<Server[]>([]);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => { serversRef.current = servers; }, [servers]);

  const logout = useCallback((reason?: string) => {
    sessionStorage.removeItem('accessToken');
    setToken(null);
    setServers([]);
    if (reason) setMessage(reason);
  }, []);

  const refresh = useCallback(async () => {
    if (!token) return;
    try { setServers(await listServers(token)); }
    catch { logout('Sessão expirada.'); }
  }, [logout, token]);

  useEffect(() => {
    if (!token) return;
    setLoading(true);
    void refresh().finally(() => setLoading(false));
    const interval = window.setInterval(() => void refresh(), 60_000);
    return () => window.clearInterval(interval);
  }, [refresh, token]);

  const handleRealtimeMetric = useCallback((metric: RealtimeMetric) => {
    const currentServer = serversRef.current.find(server => server.id === metric.serverId);
    if (!currentServer?.latestMetrics) {
      void refresh();
      return;
    }
    if (new Date(currentServer.latestMetrics.collectedAt) >= new Date(metric.collectedAt)) return;

    setServers(current => current.map(server => server.id === metric.serverId
      ? { ...server, status: 'ONLINE', lastSeen: metric.collectedAt, latestMetrics: { ...server.latestMetrics!, ...metric } }
      : server));
  }, [refresh]);

  const streamStatus = useMetricStream(token, handleRealtimeMetric);

  async function authenticate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setLoading(true);
    setMessage('');
    const data = new FormData(event.currentTarget);
    const credentials = { username: String(data.get('username')), password: String(data.get('password')) };
    try {
      if (mode === 'register') {
        await register(credentials);
        setMode('login');
        setMessage('Conta criada. Faça login.');
      } else {
        const session = await login(credentials);
        sessionStorage.setItem('accessToken', session.accessToken);
        setToken(session.accessToken);
      }
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Falha na autenticação.');
    } finally { setLoading(false); }
  }

  async function submitServer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) return;
    setLoading(true);
    const form = event.currentTarget;
    const data = new FormData(form);
    try {
      await createServer(token, { name: String(data.get('name')), ip: String(data.get('ip')), token: String(data.get('agentToken')), port: Number(data.get('port')) });
      form.reset();
      await refresh();
      setMessage('Servidor cadastrado. Inicie o agente com o mesmo token.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Falha ao cadastrar.');
    } finally { setLoading(false); }
  }

  if (!token) return <main className="auth">
    <h1>Server Manager</h1>
    <form onSubmit={authenticate}>
      <label>Usuário<input name="username" minLength={3} required /></label>
      <label>Senha<input name="password" type="password" minLength={8} required /></label>
      <button disabled={loading}>{loading ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}</button>
    </form>
    <button className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Criar conta' : 'Entrar'}</button>
    {message && <p role="alert">{message}</p>}
  </main>;

  return <main>
    <header>
      <div><h1>Dashboard</h1><small className={`connection ${streamStatus}`}>Tempo real: {streamStatusLabel(streamStatus)}</small></div>
      <button onClick={() => logout()}>Sair</button>
    </header>
    <section>
      <h2>Novo servidor</h2>
      <form className="server-form" onSubmit={submitServer}>
        <label>Nome<input name="name" required /></label>
        <label>IP<input name="ip" placeholder="192.168.1.10" required /></label>
        <label>Token do agente<input name="agentToken" type="password" minLength={32} required /></label>
        <label>Porta<input name="port" type="number" min="1" max="65535" defaultValue="8081" required /></label>
        <button disabled={loading}>Cadastrar</button>
      </form>
      {message && <p role="alert">{message}</p>}
    </section>
    <section>
      <h2>Monitoramento</h2>
      {servers.length === 0 ? <p>{loading ? 'Carregando servidores…' : 'Nenhum servidor cadastrado.'}</p> : <div className="cards">{servers.map(server => <article className="card" key={server.id}>
        <div><strong>{server.name}</strong><span className={`status ${server.status.toLowerCase()}`}>{server.status}</span></div>
        <small>{server.hostname} · {server.ip}</small>
        {server.latestMetrics ? <>
          <p>CPU <b>{server.latestMetrics.cpuPercent.toFixed(1)}%</b></p>
          <p>RAM <b>{percentage(server.latestMetrics.memoryUsedBytes, server.latestMetrics.memoryTotalBytes)}%</b></p>
          <p>Disco <b>{percentage(server.latestMetrics.diskUsedBytes, server.latestMetrics.diskTotalBytes)}%</b></p>
          <p>Rede <b>↓ {formatRate(server.latestMetrics.networkReceivedBytesPerSecond)} · ↑ {formatRate(server.latestMetrics.networkSentBytesPerSecond)}</b></p>
          <small>{server.latestMetrics.operatingSystem} · {new Date(server.latestMetrics.collectedAt).toLocaleTimeString('pt-BR')}</small>
        </> : <p>Aguardando dados do agente.</p>}
      </article>)}</div>}
    </section>
  </main>;
}

function percentage(used: number, total: number) { return total === 0 ? '0.0' : ((used / total) * 100).toFixed(1); }
function formatRate(bytesPerSecond: number) { if (bytesPerSecond < 1024) return `${bytesPerSecond} B/s`; if (bytesPerSecond < 1024 ** 2) return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`; return `${(bytesPerSecond / 1024 ** 2).toFixed(1)} MB/s`; }
function streamStatusLabel(status: ReturnType<typeof useMetricStream>) { return { disconnected: 'desconectado', connecting: 'conectando…', connected: 'conectado', error: 'com erro' }[status]; }

createRoot(document.getElementById('root')!).render(<App />);

export default App;
