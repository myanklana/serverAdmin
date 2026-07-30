import { FormEvent, useEffect, useState } from 'react';
import { createServer, listServers, login, register, Server } from './services/api';
import './styles.css';

function App() {
  const [token, setToken] = useState(sessionStorage.getItem('accessToken'));
  const [loading, setLoading] = useState(false);
  const [servers, setServers] = useState<Server[]>([]);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [message, setMessage] = useState('');

  async function refresh() {
    if (!token) return;
    try { setLoading(true); setServers(await listServers(token)); } catch { setMessage('Sua sessão expirou. Entre novamente.'); setToken(null); sessionStorage.removeItem('accessToken'); } finally { setLoading(false); }
  }
  useEffect(() => { refresh(); }, [token]);

  async function authenticate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setMessage(''); setLoading(true);
    const data = new FormData(event.currentTarget);
    const credentials = { username: String(data.get('username')), password: String(data.get('password')) };
    try {
      if (mode === 'register') { await register(credentials); setMessage('Conta criada. Faça login para continuar.'); setMode('login'); return; }
      const session = await login(credentials); sessionStorage.setItem('accessToken', session.accessToken); setToken(session.accessToken);
    } catch (error) { setMessage(error instanceof Error ? error.message : 'Não foi possível concluir a operação.'); } finally { setLoading(false); }
  }
  async function submitServer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); if (!token) return; setMessage(''); setLoading(true);
    const data = new FormData(event.currentTarget);
    try {
      await createServer(token, { name: String(data.get('name')), ip: String(data.get('ip')), token: String(data.get('agentToken')), port: Number(data.get('port') || 8081) });
      event.currentTarget.reset(); await refresh(); setMessage('Servidor cadastrado. Ele ficará pendente até o agente se comunicar.');
    } catch (error) { setMessage(error instanceof Error ? error.message : 'Não foi possível cadastrar o servidor.'); } finally { setLoading(false); }
  }
  if (!token) return <main className="auth"><h1>Server Manager</h1><p>Entre para administrar seus servidores.</p><form onSubmit={authenticate}><label>Usuário<input name="username" minLength={3} required /></label><label>Senha<input name="password" type="password" minLength={8} required /></label><button disabled={loading}>{loading ? 'Aguarde…' : mode === 'login' ? 'Entrar' : 'Criar conta'}</button></form><button className="link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Ainda não tenho conta' : 'Já tenho uma conta'}</button>{message && <p role="alert">{message}</p>}</main>;
  return <main><header><h1>Servidores</h1><button onClick={() => { sessionStorage.removeItem('accessToken'); setToken(null); }}>Sair</button></header><section><h2>Novo servidor</h2><form onSubmit={submitServer} className="server-form"><label>Nome<input name="name" required /></label><label>IP<input name="ip" placeholder="192.168.1.10" required /></label><label>Token do agente<input name="agentToken" type="password" minLength={32} required /></label><label>Porta<input name="port" type="number" min="1" max="65535" defaultValue="8081" required /></label><button disabled={loading}>{loading ? 'Salvando…' : 'Cadastrar servidor'}</button></form>{message && <p role="alert">{message}</p>}</section><section><h2>Servidores cadastrados</h2>{loading ? <p>Carregando…</p> : servers.length === 0 ? <p>Nenhum servidor cadastrado.</p> : <ul>{servers.map(server => <li key={server.id}><strong>{server.name}</strong><span>{server.hostname} · {server.ip}:{server.port}</span><span>{server.lastSeen ? new Date(server.lastSeen).toLocaleString('pt-BR') : 'Nunca conectado'}</span><span className={`status ${server.status.toLowerCase()}`}>{server.status}</span></li>)}</ul>}</section></main>;
}
export default App;
