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

## Requisitos do sistema

Para executar a API localmente, instale:

| Requisito | Versão / uso |
| --- | --- |
| Java (JDK) | 21 ou superior. O projeto é compilado com `release 21`. |
| Git for Windows | Inclui o Git Bash, usado nos comandos abaixo. |
| PostgreSQL | 17 ou superior, em execução na porta `5432`. |
| Docker Desktop | Opcional no estado atual do projeto; será necessário somente para executar serviços containerizados. |
| Internet | Necessária apenas na primeira execução, para o Maven Wrapper baixar dependências. |

Não é preciso instalar o Maven globalmente: a API inclui o Maven Wrapper (`api/mvnw`).

### Banco de dados de desenvolvimento

Crie o banco `servermanager` no PostgreSQL. A configuração padrão usa as seguintes credenciais:

```text
Host: localhost
Porta: 5432
Banco: servermanager
Usuário: postgres
Senha: postgres
```

Para usar outras credenciais, defina `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD` no ambiente antes
de iniciar a aplicação.

## Executar a API pelo Git Bash

Na raiz do repositório, execute:

```bash
cd api
./mvnw spring-boot:run
```

A API ficará disponível em `http://localhost:8080`. Para confirmar que ela foi
iniciada, acesse `http://localhost:8080/admin/status`.


Para executar localmente, defina uma chave segura antes de iniciar a API:
$env:APP_JWT_SECRET = "uma-chave-aleatoria-com-no-minimo-32-bytes"