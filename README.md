# Server Manager

Painel open-source para monitorar servidores. Você instala o painel **na sua própria máquina**,
instala um agente leve em cada servidor monitorado e acompanha CPU, memória, disco e rede em
tempo real, com histórico de até 30 dias.

| Componente | Função |
| --- | --- |
| `api/` | API Spring Boot (autenticação, métricas, WebSocket). Também serve o painel. |
| `frontend/` | Painel web em React. |
| `agent/` | Agente Java que roda nos servidores monitorados e envia métricas. |

## Instalação rápida (Docker)

Requisito: Docker com Compose.

```bash
git clone https://github.com/myanklana/serverAdmin.git && cd serverAdmin
./scripts/setup.sh                 # Windows: .\scripts\setup.ps1
docker compose up -d --build
```

Abra <http://localhost:8080>. **A primeira conta criada é a administradora**; depois disso o
cadastro fecha (reabra com `APP_REGISTRATION_OPEN=true` no `.env`). Os dados ficam nos volumes
Docker e sobrevivem a reinícios e atualizações.

## Conectar um servidor

1. No painel, cadastre o servidor (nome e IP). O token do agente aparece **uma única vez**,
   junto com o comando pronto.
2. Baixe `server-manager-agent.jar` na página de [Releases](../../releases) (ou gere com
   `./scripts/package-agent.sh`) e copie para o servidor, que precisa apenas de Java 21+.
3. Execute o comando exibido. No Linux, para rodar como serviço:
   `sudo ./install-agent-linux.sh https://seu-painel TOKEN`.

> **Rede:** os agentes precisam alcançar o painel. Se ele roda em um computador doméstico, use
> uma VPN (Tailscale/WireGuard) ou um proxy reverso. Veja [docs/instalacao.md](docs/instalacao.md),
> incluindo HTTPS.

## Documentação

- [Instalação, HTTPS, backup e atualização](docs/instalacao.md)
- [Arquitetura](docs/arquitetura.md)
- [Contribuindo](CONTRIBUTING.md) · [Segurança](SECURITY.md) · [Changelog](CHANGELOG.md)

## Licença

[MIT](LICENSE)
