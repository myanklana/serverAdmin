# Arquitetura

```text
Frontend (React) → API (Spring Boot) → PostgreSQL
                       ↕
                 REST + WebSocket
                       ↕
                 Agentes Java
```

A API é o ponto central de autenticação, autorização, persistência e distribuição de eventos. Os agentes nunca devem expor portas de administração diretamente para a internet.
