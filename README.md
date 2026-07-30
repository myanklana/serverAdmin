# Server Manager

Plataforma para monitorar e administrar servidores por meio de uma API central, uma interface web e agentes instalados nas máquinas monitoradas.

## Módulos

| Diretório | Responsabilidade |
| --- | --- |
| `api/` | API Spring Boot, autenticação, métricas, comandos e WebSocket |
| `frontend/` | Interface React do painel administrativo |
| `agent/` | Agente Java que coleta métricas e executa ações autorizadas |
| `docs/` | Documentação de arquitetura, API e operação |

Consulte [roteiro.md](roteiro.md) para as fases de desenvolvimento.
