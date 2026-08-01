# Arquitetura

```text
Navegador ── HTTP/JSON ──> Frontend React ── HTTP/JSON + JWT ──> API Spring Boot ──> PostgreSQL
                                                                      ^
                                                                      │ HTTP/JSON + token
                                                                      │ (saída a cada 5 s)
                                                            Agente Java nos servidores
```

## Componentes

- **Frontend:** autenticação, cadastro de servidores e visualização das últimas métricas.
- **API:** autenticação JWT, autorização por usuário, cadastro de servidores e recebimento de métricas.
- **PostgreSQL:** usuários, servidores gerenciados e histórico de métricas; o esquema é versionado por migrations.
- **Agente:** processo Java instalado no servidor monitorado. Coleta CPU, memória, disco, rede e informações do sistema e as envia à API.

## Fluxos e segurança

- O usuário se autentica na API e o frontend envia o JWT nas chamadas protegidas.
- Cada servidor possui um token próprio; o agente o envia no cabeçalho `X-Agent-Token` ao publicar métricas.
- O agente somente inicia conexões de saída para a API. Não há porta de administração exposta nos servidores monitorados.
- Para uso fora de uma rede confiável, a API deve ser publicada com HTTPS. O banco de dados permanece interno.

## Execução

O ambiente de desenvolvimento usa PostgreSQL em Docker, API na porta `8080` e frontend na porta `5173`. Scripts em `scripts/` iniciam o ambiente e geram um JAR portátil do agente para servidores externos.
