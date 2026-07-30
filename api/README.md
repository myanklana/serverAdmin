# API

API Spring Boot do Server Manager.

Os módulos de negócio ficam no pacote `adminServer.mvp`:

```text
auth/       users/      servers/    metrics/    websocket/
alerts/     commands/   audit/      docker/     terminal/
config/     security/
```

As migrações de banco de dados devem ser criadas em `src/main/resources/db/migration/`.
