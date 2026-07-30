# Roteiro de desenvolvimento — Server Manager

Este projeto deve evoluir como um produto real: ao final de cada fase, deve existir uma entrega funcional e utilizável. Assim, você constrói um MVP cedo e evita passar meses sem algo concreto para testar.

## Visão geral da arquitetura

```text
Web (React)
    │
    ▼
API (Spring Boot)
    │
    ├── PostgreSQL
    └── REST + WebSocket
            │
            ├── Agente Java
            ├── Agente Java
            └── Agente Java
```

## Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Security, Spring Data JPA, PostgreSQL, WebSocket, Maven |
| Frontend | React, Tailwind CSS, Chart.js ou ECharts |
| Agente | Java, OSHI, HttpClient e Jackson |

---

## Fase 0 — Planejamento

Defina a arquitetura, os módulos e o escopo inicial antes de escrever código.

## Fase 1 — Estrutura inicial e API

Comece somente pela API.

### Estrutura do projeto

```text
server-manager/
├── api/
├── frontend/
├── agent/
└── docs/
```

### Organização da API Spring Boot

```text
src/
├── controllers/
├── services/
├── repositories/
├── entities/
├── dto/
├── config/
├── security/
├── websocket/
├── monitoring/
├── docker/
└── ssh/
```

## Fase 2 — Autenticação

Implemente apenas o essencial:

- Cadastro de usuário
- Login
- Autenticação com JWT

### Endpoints

```http
POST /register
POST /login
GET  /me
```

## Fase 3 — Cadastro de servidores

### Entidade `Server`

| Campo | Descrição |
| --- | --- |
| `id` | Identificador do servidor |
| `name` | Nome de exibição |
| `hostname` | Nome da máquina |
| `ip` | Endereço IP |
| `port` | Porta de comunicação |
| `token` | Token de autenticação do agente |
| `status` | Estado atual do servidor |
| `lastSeen` | Última comunicação recebida |

### Tela inicial

Formulário **Novo servidor** com os campos: nome, IP e token.

## Fase 4 — Agente

Crie o agente como uma aplicação Java independente, executada assim:

```bash
java -jar agent.jar
```

Ele deve ler o arquivo `config.json`:

```json
{
  "server": "https://api.meusistema.com",
  "token": "xxxxxxxx"
}
```

Após iniciar, o agente se conecta à API.

## Fase 5 — Coleta de métricas

Use o OSHI para coletar:

- CPU
- RAM
- Disco
- Hostname
- Sistema operacional
- Kernel
- Arquitetura
- Uptime
- Rede

Envie os dados para `POST /api/metrics` a cada 5 segundos.

## Fase 6 — Dashboard

Exiba, por servidor:

- Status online/offline
- Uso de CPU, RAM e disco
- Sistema operacional
- Última atualização

Os dados devem aparecer em tempo real.

## Fase 7 — Atualização em tempo real

Use WebSocket para não depender de atualização manual da página.

```text
Agente → API → WebSocket → Navegador
```

Por exemplo, quando o agente enviar CPU em 42%, o gráfico deverá ser atualizado automaticamente.

## Fase 8 — Gráficos e histórico visual

Adicione gráficos para:

- CPU
- RAM
- Rede
- Disco
- Histórico de uso

## Fase 9 — Processos

Use o OSHI para listar processos e mostrar:

- PID
- Nome
- Uso de CPU
- Uso de RAM

Inclua a ação **Encerrar processo**.

## Fase 10 — Serviços Linux

Liste serviços com `systemctl`, por exemplo: nginx, PostgreSQL, Redis e Docker.

Inclua as ações: iniciar, parar e reiniciar.

## Fase 11 — Docker

Se Docker estiver disponível, exiba para cada container:

- Container
- Imagem
- Status
- Portas

Inclua as ações: iniciar, parar, reiniciar e visualizar logs.

## Fase 12 — Logs

Execute `journalctl` no agente e transmita a saída em tempo real:

```text
Agente → API → WebSocket → Navegador
```

A experiência deve ser semelhante a um terminal.

## Fase 13 — Terminal web

Implemente um terminal remoto controlado pelo navegador.

```text
Navegador → WebSocket → API → Agente → bash
Agente → API → Navegador
```

O resultado será uma experiência próxima a um SSH, com as devidas restrições de segurança.

## Fase 14 — Upload de arquivos

Permita enviar arquivos, como `nginx.conf`, para o agente salvá-los em destinos autorizados, como `/etc/nginx/`.

## Fase 15 — Download de arquivos

Permita baixar arquivos gerados no servidor, como `logs.zip` e `backup.sql`.

## Fase 16 — Alertas

Crie regras para eventos como:

- CPU acima de 90%
- RAM acima de 95%
- Disco acima de 80%
- Servidor offline

Envie notificações por e-mail, Discord e Telegram.

## Fase 17 — Histórico de métricas

Armazene métricas de CPU, RAM, disco e temperatura para gerar consultas de:

- Últimas 24 horas
- Últimos 7 dias
- Últimos 30 dias

## Fase 18 — Permissões

Defina papéis de acesso:

| Papel | Permissões |
| --- | --- |
| Administrador | Acesso completo |
| Operador | Pode operar servidores, como reiniciar serviços; não pode apagar recursos críticos |
| Visitante | Apenas visualização |

## Fase 19 — Auditoria

Registre todas as ações relevantes com:

- Quem executou
- Quando executou
- IP de origem
- Servidor afetado
- Comando ou ação realizada

Exemplo: Pedro, às 15:21, reiniciou o nginx.

## Fase 20 — Multiempresa

Isole os dados por empresa:

```text
Empresa A → Servidores A
Empresa B → Servidores B
```

Nenhuma empresa deve acessar os dados da outra.

## Fase 21 — Atualização automática do agente

Quando houver uma versão mais recente do agente, a API deve detectar a diferença, baixar a nova versão, reiniciar o agente e manter a operação.

Exemplo: agente na versão `1.0.1`; versão `1.0.2` disponível.

---

## Estrutura sugerida do agente

```text
agent/
├── config/
├── collector/
├── sender/
├── websocket/
├── terminal/
├── docker/
├── services/
└── updater/
```

## Estrutura sugerida da API

```text
api/
├── auth/
├── users/
├── servers/
├── metrics/
├── websocket/
├── alerts/
├── commands/
├── audit/
├── docker/
└── terminal/
```

## Banco de dados inicial

- `users`
- `servers`
- `metrics`
- `alerts`
- `commands`
- `audit_logs`
- `companies`

## Competências desenvolvidas

Ao concluir o projeto, você terá prática com:

- Comunicação HTTP e WebSocket
- Arquitetura distribuída: servidor central e agentes
- Segurança: JWT, autenticação entre agente e API e autorização por papéis
- Persistência com JPA e PostgreSQL
- Coleta de métricas do sistema operacional
- Execução controlada de comandos remotos
- Observabilidade: logs, auditoria e monitoramento
- Deploy com Docker e integração com Linux

## Ordem recomendada para o MVP

Resista à tentação de começar pelo terminal remoto ou pelo gerenciamento de Docker. O valor do projeto está em construir uma base sólida primeiro.

1. Autenticação
2. Cadastro de servidores
3. Agente enviando métricas
4. Dashboard em tempo real
5. Histórico de métricas
6. Alertas
7. Execução remota de comandos
8. Logs
9. Docker e serviços
10. Recursos avançados: multiempresa, atualização automática do agente e outros

Com essa sequência, em poucas semanas você terá um MVP funcional. As próximas funcionalidades poderão ser construídas sobre uma arquitetura estável, com menos necessidade de refatorações grandes.
