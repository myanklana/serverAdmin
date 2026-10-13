# Contribuindo

## Ambiente de desenvolvimento

Requisitos: JDK 21, Node.js 22+, Docker.

```bash
./scripts/start-dev.sh      # Linux/macOS: banco + API (:8080) + painel (:5173)
# Windows: powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

O painel em desenvolvimento encaminha `/api`, `/login` e `/ws` para a API via proxy do Vite.

## Testes

```bash
(cd api && ./mvnw verify)
(cd api && ./mvnw -f ../agent/pom.xml verify)
(cd frontend && npm ci && npm run build)
```

O CI executa os mesmos comandos em cada pull request.

## Pull requests

- Uma mudança por PR, com testes quando houver lógica nova.
- Migrations novas ficam em `api/src/main/resources/db/migration` (`V<n>__descricao.sql`); nunca edite uma migration já publicada.
- Atualize o `CHANGELOG.md`.
