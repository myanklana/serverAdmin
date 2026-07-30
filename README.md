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

### Inicio rapido no Windows

Com Docker, Java 21, Node.js e npm instalados, o comando abaixo sobe o banco,
a API e o painel em duas janelas separadas:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-dev.ps1
```

O painel fica em `http://localhost:5173`. Para encerrar, feche as duas janelas
abertas pelo script e execute `docker compose down` se tambem quiser parar o banco.

## Monitorar um servidor externo

1. No computador que executa esta aplicacao, gere o JAR portavel do agente:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\package-agent.ps1
```

2. Copie `dist\server-manager-agent.jar` e o script adequado (`scripts\start-agent.ps1` para Windows ou `scripts/start-agent.sh` para Linux) para o servidor. Esse servidor precisa apenas do Java 21 ou superior; Maven nao e necessario.

3. No painel, cadastre o servidor e escolha um token com pelo menos 32 caracteres. Em seguida execute no servidor monitorado:

```powershell
powershell -ExecutionPolicy Bypass -File .\start-agent.ps1 -ApiUrl "https://api.seudominio.com" -Token "SEU_TOKEN_DO_PAINEL"
```

Ou, no Linux:

```bash
chmod +x start-agent.sh
./start-agent.sh https://api.seudominio.com SEU_TOKEN_DO_PAINEL
```

Use `https://` quando a API estiver fora da rede local. A porta da API (por padrao, `8080`) deve estar acessivel pelo servidor monitorado.
