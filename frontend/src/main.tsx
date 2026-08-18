import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { useMetricStream } from './hooks/useMetricStream';
import { MetricChart } from './components/MetricChart';
import { createServer, getMetricHistory, HistoricalMetric, listServers, login, register, Server } from './services/api';
import type { RealtimeMetric } from './types/RealTimeMetric';
import './styles.css';

function App() {
  const [token, setToken] = useState(sessionStorage.getItem('accessToken'));
  const [servers, setServers] = useState<Server[]>([]);
  const serversRef = useRef<Server[]>([]);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedServerId, setSelectedServerId] = useState('');
  const [periodDays, setPeriodDays] = useState(1);
  const [history, setHistory] = useState<HistoricalMetric[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyError, setHistoryError] = useState('');

  useEffect(() => { serversRef.current = servers; }, [servers]);
  useEffect(() => {
    if (!selectedServerId && servers.length > 0) setSelectedServerId(servers[0].id);
    if (selectedServerId && !servers.some(server => server.id === selectedServerId)) setSelectedServerId(servers[0]?.id ?? '');
  }, [selectedServerId, servers]);

  useEffect(() => {
    if (!token || !selectedServerId) { setHistory([]); return; }
    let active = true;
    const to = new Date();
    const from = new Date(to.getTime() - periodDays * 86_400_000);
    setHistoryLoading(true);
    setHistoryError('');
    void getMetricHistory(token, selectedServerId, from, to)
      .then(metrics => { if (active) setHistory(metrics); })
      .catch(error => { if (active) setHistoryError(error instanceof Error ? error.message : 'Falha ao carregar o histórico.'); })
      .finally(() => { if (active) setHistoryLoading(false); });
    return () => { active = false; };
  }, [periodDays, selectedServerId, token]);

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
    if (metric.serverId === selectedServerId) {
      const historicalMetric: HistoricalMetric = {
        ...currentServer.latestMetrics,
        ...metric,
        serverId: metric.serverId,
        serverName: metric.serverName,
      };
      setHistory(current => [...current, historicalMetric].filter(item =>
        new Date(item.collectedAt).getTime() >= Date.now() - periodDays * 86_400_000).slice(-700));
    }
  }, [periodDays, refresh, selectedServerId]);

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
    <section>
      <div className="history-header">
        <div><h2>Histórico de uso</h2><small>Os gráficos recebem novas amostras em tempo real.</small></div>
        <div className="history-controls">
          <label>Servidor<select value={selectedServerId} onChange={event => setSelectedServerId(event.target.value)} disabled={servers.length === 0}>
            {servers.map(server => <option value={server.id} key={server.id}>{server.name}</option>)}
          </select></label>
          <label>Período<select value={periodDays} onChange={event => setPeriodDays(Number(event.target.value))}>
            <option value={1}>24 horas</option><option value={7}>7 dias</option><option value={30}>30 dias</option>
          </select></label>
        </div>
      </div>
      {historyError && <p role="alert">{historyError}</p>}
      {historyLoading ? <p>Carregando histórico…</p> : servers.length === 0 ? <p>Cadastre um servidor para visualizar o histórico.</p> : <div className="charts">
        <MetricChart title="CPU" metrics={history} maximum={100} series={[{ label: 'Uso', unit: '%', color: '#1769e0', value: metric => metric.cpuPercent }]} />
        <MetricChart title="Memória RAM" metrics={history} maximum={100} series={[{ label: 'Uso', unit: '%', color: '#7c3aed', value: metric => percentageValue(metric.memoryUsedBytes, metric.memoryTotalBytes) }]} />
        <MetricChart title="Disco" metrics={history} maximum={100} series={[{ label: 'Uso', unit: '%', color: '#d97706', value: metric => percentageValue(metric.diskUsedBytes, metric.diskTotalBytes) }]} />
        <MetricChart title="Rede" metrics={history} series={[
          { label: 'Download', unit: 'B/s', color: '#07805b', value: metric => metric.networkReceivedBytesPerSecond },
          { label: 'Upload', unit: 'B/s', color: '#c2415d', value: metric => metric.networkSentBytesPerSecond },
        ]} />
      </div>}
    </section>
  </main>;
}

function percentage(used: number, total: number) { return total === 0 ? '0.0' : ((used / total) * 100).toFixed(1); }
function percentageValue(used: number, total: number) { return total === 0 ? 0 : (used / total) * 100; }
function formatRate(bytesPerSecond: number) { if (bytesPerSecond < 1024) return `${bytesPerSecond} B/s`; if (bytesPerSecond < 1024 ** 2) return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`; return `${(bytesPerSecond / 1024 ** 2).toFixed(1)} MB/s`; }
function streamStatusLabel(status: ReturnType<typeof useMetricStream>) { return { disconnected: 'desconectado', connecting: 'conectando…', connected: 'conectado', error: 'com erro' }[status]; }

createRoot(document.getElementById('root')!).render(<App />);

export default App;
