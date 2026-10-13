# API

API Spring Boot do Server Manager. Em produção também serve o painel (arquivos estáticos).

| Método | Rota | Autenticação |
| --- | --- | --- |
| `GET` | `/setup-status` | pública |
| `POST` | `/register` | pública (somente a primeira conta, ou se `APP_REGISTRATION_OPEN=true`) |
| `POST` | `/login` | pública |
| `GET` | `/me` | Bearer JWT |
| `GET/POST` | `/api/servers` | Bearer JWT |
| `GET/PATCH/DELETE` | `/api/servers/{id}` | Bearer JWT |
| `POST` | `/api/servers/{id}/rotate-token` | Bearer JWT |
| `GET` | `/api/servers/{id}/metrics` | Bearer JWT |
| `POST` | `/api/agent/metrics` | `X-Agent-Token` |
| `WS` | `/ws` (STOMP, `/user/queue/metrics`) | Bearer JWT |

Swagger (`/swagger-ui.html`) fica ativo apenas no perfil `dev`. Migrations em
`src/main/resources/db/migration`. O token de agente nunca é devolvido depois da criação.
