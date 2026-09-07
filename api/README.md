# API

API Spring Boot do Server Manager.

Os módulos de negócio ficam no pacote `adminServer.mvp`:

```text
auth/       users/      servers/    metrics/    websocket/
alerts/     commands/   audit/      docker/     terminal/
config/     security/
```

As migrações de banco de dados devem ser criadas em `src/main/resources/db/migration/`.

## Endpoints disponíveis

| Método | Rota | Autenticação |
| --- | --- | --- |
| `POST` | `/register` | pública |
| `POST` | `/login` | pública |
| `GET` | `/me` | Bearer JWT |
| `GET` | `/api/servers` | Bearer JWT |
| `POST` | `/api/servers` | Bearer JWT |
| `GET` | `/api/servers/{id}` | Bearer JWT |
| `DELETE` | `/api/servers/{id}` | Bearer JWT |
| `POST` | `/api/servers/{id}/processes/refresh` | Bearer JWT |
| `POST` | `/api/servers/{id}/processes/terminate` | Bearer JWT |
| `GET` | `/api/servers/{id}/processes/commands/{commandId}` | Bearer JWT |
| `POST` | `/api/agent/commands/claim` | X-Agent-Token |
| `POST` | `/api/agent/commands/{commandId}/complete` | X-Agent-Token |

O token do agente é devolvido uma única vez na criação ou rotação. Somente os hashes de verificação e busca são persistidos.
