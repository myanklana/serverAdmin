# Instalação e operação

## Configuração (`.env`)

| Variável | Padrão | Descrição |
| --- | --- | --- |
| `POSTGRES_PASSWORD` | gerada por `setup.sh` | Senha do banco (não é exposto fora do Compose). |
| `APP_PORT` | `8080` | Porta publicada no host. |
| `APP_CORS_ALLOWED_ORIGINS` | `http://localhost:8080` | Origens permitidas; ajuste se usar outro domínio. |
| `APP_REGISTRATION_OPEN` | `false` | `true` permite novos cadastros além do primeiro. |
| `APP_METRICS_RETENTION_DAYS` | `30` | Métricas mais antigas são apagadas a cada hora. |

A chave de assinatura dos tokens JWT é gerada automaticamente na primeira execução e guardada
no volume `app-data` (`/data/jwt-secret`). Para fornecer a sua, defina `APP_JWT_SECRET` (mín. 32 bytes).

## HTTPS (obrigatório fora de uma rede confiável)

O token do agente e a senha trafegam em texto puro sobre HTTP. Coloque um proxy reverso na
frente. Exemplo com Caddy (certificado automático):

```text
painel.exemplo.com {
    reverse_proxy localhost:8080
}
```

Defina `APP_CORS_ALLOWED_ORIGINS=https://painel.exemplo.com` e use essa URL nos agentes.
WebSocket (`/ws`) passa pelo mesmo proxy sem configuração extra.

## Agente

- **Linux:** `sudo ./install-agent-linux.sh URL TOKEN` cria o usuário `server-manager-agent`
  e o serviço systemd (`journalctl -u server-manager-agent -f`).
- **Windows:** `start-agent.ps1 -ApiUrl URL -Token TOKEN`; para rodar como serviço, use o
  Agendador de Tarefas ("ao iniciar o sistema") ou NSSM apontando para `java -jar`.
- O `config.json` contém o token; mantenha-o legível apenas pelo usuário do agente.
- Perdeu o token? Gere outro pela rotação de token da API (`POST /api/servers/{id}/rotate-token`).

## Backup

```bash
docker compose exec postgres pg_dump -U servermanager servermanager > backup.sql
docker run --rm -v serverAdmin_app-data:/data -v "$PWD":/out alpine tar czf /out/app-data.tgz /data
```

## Atualizar

```bash
git pull && docker compose up -d --build
```

As migrations do banco rodam sozinhas na inicialização.

## Solução de problemas

- `POSTGRES_PASSWORD ... Run ./scripts/setup.sh`: crie o `.env` primeiro.
- Servidor aparece offline: o agente envia a cada 5 s; offline após 15 s sem dados. Verifique a
  URL, o token e se a porta do painel é acessível pelo servidor monitorado.
- Esqueceu a senha do administrador: não há recuperação por e-mail; redefina via banco
  (`app_users`) gerando um novo hash BCrypt.
