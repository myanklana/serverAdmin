import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { useMetricStream } from './hooks/useMetricStream';
import { MetricChart } from './components/MetricChart';
import { apiUrl, createServer, getMetricHistory, getOperation, getProcessCommand, HistoricalMetric, listServers, login, OperationType, ProcessInfo, refreshProcesses, register, requestOperation, Server, terminateProcess } from './services/api';
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
  const [agentSetup, setAgentSetup] = useState<{ serverName: string; token: string } | null>(null);
  const [processes, setProcesses] = useState<ProcessInfo[]>([]);
  const [processLoading, setProcessLoading] = useState(false);
  const [processError, setProcessError] = useState('');
  const [remoteOutput, setRemoteOutput] = useState('');
  const [remoteLoading, setRemoteLoading] = useState(false);

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
      const created = await createServer(token, { name: String(data.get('name')), ip: String(data.get('ip')), port: Number(data.get('port')) });
      form.reset();
      await refresh();
      setAgentSetup({ serverName: created.server.name, token: created.agentToken });
      setMessage('Servidor cadastrado. Copie um dos comandos abaixo para iniciar o agente.');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Falha ao cadastrar.');
    } finally { setLoading(false); }
  }

  async function waitForCommand(commandId: string) {
    if (!token || !selectedServerId) throw new Error('Selecione um servidor.');
    for (let attempt = 0; attempt < 24; attempt++) {
      const command = await getProcessCommand(token, selectedServerId, commandId);
      if (command.status === 'SUCCEEDED') return command.result;
      if (['FAILED', 'REJECTED', 'TIMED_OUT'].includes(command.status)) throw new Error(command.errorMessage ?? 'O agente não concluiu a operação.');
      await new Promise(resolve => window.setTimeout(resolve, 1000));
    }
    throw new Error('O agente demorou demais para responder.');
  }

  async function loadProcesses() {
    if (!token || !selectedServerId) return;
    setProcessLoading(true); setProcessError('');
    try {
      const command = await refreshProcesses(token, selectedServerId);
      const result = await waitForCommand(command.id);
      setProcesses(result ? JSON.parse(result) as ProcessInfo[] : []);
    } catch (error) { setProcessError(error instanceof Error ? error.message : 'Falha ao listar processos.'); }
    finally { setProcessLoading(false); }
  }

  async function killProcess(pid: number, name: string) {
    if (!token || !selectedServerId || !window.confirm(`Encerrar ${name} (PID ${pid})?`)) return;
    setProcessLoading(true); setProcessError('');
    try { const command = await terminateProcess(token, selectedServerId, pid); await waitForCommand(command.id); await loadProcesses(); }
    catch (error) { setProcessError(error instanceof Error ? error.message : 'Falha ao encerrar processo.'); setProcessLoading(false); }
  }
  async function remote(type: OperationType, payload: Record<string,string> = {}) {
    if (!token || !selectedServerId) return; setRemoteLoading(true); setRemoteOutput('');
    try { const created = await requestOperation(token, selectedServerId, type, payload);
      for (let i=0;i<45;i++) { const command=await getOperation(token,selectedServerId,created.id); if(command.status==='SUCCEEDED'){setRemoteOutput(command.result??'Concluído.');return;} if(['FAILED','REJECTED','TIMED_OUT'].includes(command.status)) throw new Error(command.errorMessage??'Operação falhou.'); await new Promise(r=>window.setTimeout(r,1000)); }
      throw new Error('Tempo limite excedido.');
    } catch(e){setRemoteOutput(e instanceof Error?e.message:'Falha na operação.');} finally{setRemoteLoading(false);}
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
        <details className="advanced-options">
          <summary>Opções avançadas</summary>
          <label>Porta<input name="port" type="number" min="1" max="65535" defaultValue="8081" required /></label>
        </details>
        <button disabled={loading}>Cadastrar</button>
      </form>
      {message && <p role="alert">{message}</p>}
      {agentSetup && <AgentSetup serverName={agentSetup.serverName} token={agentSetup.token} onDismiss={() => setAgentSetup(null)} />}
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
    <section>
      <div className="process-heading"><div><h2>Processos</h2><small>Lista coletada sob demanda pelo agente selecionado.</small></div><button disabled={!selectedServerId || processLoading} onClick={() => void loadProcesses()}>{processLoading ? 'Aguardando agente…' : 'Atualizar processos'}</button></div>
      {processError && <p role="alert">{processError}</p>}
      {processes.length > 0 && <div className="table-scroll"><table><thead><tr><th>PID</th><th>Processo</th><th>Usuário</th><th>CPU</th><th>RAM</th><th></th></tr></thead><tbody>
        {processes.map(process => <tr key={process.pid}><td>{process.pid}</td><td>{process.name}</td><td>{process.user || '—'}</td><td>{process.cpuPercent.toFixed(1)}%</td><td>{formatBytes(process.memoryBytes)}</td><td><button className="danger" disabled={processLoading} onClick={() => void killProcess(process.pid, process.name)}>Encerrar</button></td></tr>)}
      </tbody></table></div>}
      {!processLoading && processes.length === 0 && <p>Atualize para consultar os processos em execução.</p>}
    </section>
    <RemoteTools disabled={!selectedServerId||remoteLoading} output={remoteOutput} run={remote}/>
  </main>;
}

function RemoteTools({disabled,output,run}:{disabled:boolean;output:string;run:(type:OperationType,payload?:Record<string,string>)=>Promise<void>}) { const [value,setValue]=useState(''); return <section><h2>Administração remota</h2><div className="tool-actions"><button disabled={disabled} onClick={()=>void run('LIST_SERVICES')}>Serviços</button><button disabled={disabled} onClick={()=>void run('LIST_CONTAINERS')}>Containers</button><button disabled={disabled} onClick={()=>void run('SYSTEM_LOGS',{lines:'300'})}>Logs</button><button disabled={disabled} onClick={()=>void run('CHECK_UPDATE')}>Versão</button></div><form className="terminal-form" onSubmit={e=>{e.preventDefault();void run('RUN_TERMINAL_COMMAND',{command:value});}}><label>Comando permitido<input value={value} onChange={e=>setValue(e.target.value)} placeholder="uptime" required/></label><button disabled={disabled}>Executar</button></form>{output&&<pre className="terminal-output">{output}</pre>}<small>O terminal e os arquivos dependem das permissões explícitas do agente.</small></section>; }

function percentage(used: number, total: number) { return total === 0 ? '0.0' : ((used / total) * 100).toFixed(1); }
function percentageValue(used: number, total: number) { return total === 0 ? 0 : (used / total) * 100; }
function formatRate(bytesPerSecond: number) { if (bytesPerSecond < 1024) return `${bytesPerSecond} B/s`; if (bytesPerSecond < 1024 ** 2) return `${(bytesPerSecond / 1024).toFixed(1)} KB/s`; return `${(bytesPerSecond / 1024 ** 2).toFixed(1)} MB/s`; }
function formatBytes(bytes: number) { if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KB`; if (bytes < 1024 ** 3) return `${(bytes / 1024 ** 2).toFixed(1)} MB`; return `${(bytes / 1024 ** 3).toFixed(1)} GB`; }
function streamStatusLabel(status: ReturnType<typeof useMetricStream>) { return { disconnected: 'desconectado', connecting: 'conectando…', connected: 'conectado', error: 'com erro' }[status]; }

function AgentSetup({ serverName, token, onDismiss }: { serverName: string; token: string; onDismiss: () => void }) {
  const windowsCommand = `.\\start-agent.ps1 -ApiUrl "${apiUrl}" -Token "${token}"`;
  const linuxCommand = `./start-agent.sh '${apiUrl}' '${token}'`;
  const [copied, setCopied] = useState('');

  async function copy(label: string, command: string) {
    await navigator.clipboard.writeText(command);
    setCopied(label);
  }

  return <aside className="agent-setup" aria-live="polite">
    <div><h3>Conectar {serverName}</h3><button className="dismiss" type="button" onClick={onDismiss}>Fechar</button></div>
    <p>Este token é exibido somente agora. Copie o comando adequado e execute-o na pasta que contém o agente.</p>
    <label>Windows</label>
    <div className="command"><code>{windowsCommand}</code><button type="button" onClick={() => void copy('Windows', windowsCommand)}>Copiar</button></div>
    <label>Linux</label>
    <div className="command"><code>{linuxCommand}</code><button type="button" onClick={() => void copy('Linux', linuxCommand)}>Copiar</button></div>
    {copied && <small>Comando para {copied} copiado.</small>}
    <p><small>Se perder o token, gere outro pela rotação de token; o valor atual não pode ser recuperado.</small></p>
  </aside>;
}

createRoot(document.getElementById('root')!).render(<App />);

export default App;
