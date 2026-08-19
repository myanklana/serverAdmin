# API

API Spring Boot do Server Manager.

Os módulos de negócio ficam no pacote `adminServer.mvp`:

```text
auth/       users/      servers/    metrics/    websocket/
alerts/     commands/   audit/      docker/     terminal/
config/     security/
```

As migrações de banco de dados devem ser criadas em `src/main/resources/db/migration/`.

## Fases 1 a 3: endpoints disponíveis

| Método | Rota | Autenticação |
| --- | --- | --- |
| `POST` | `/register` | pública |
| `POST` | `/login` | pública |
| `GET` | `/me` | Bearer JWT |
| `GET` | `/api/servers` | Bearer JWT |
| `POST` | `/api/servers` | Bearer JWT |
| `GET` | `/api/servers/{id}` | Bearer JWT |
| `DELETE` | `/api/servers/{id}` | Bearer JWT |

O token de agente nunca é devolvido pela API: somente o hash BCrypt é persistido.
