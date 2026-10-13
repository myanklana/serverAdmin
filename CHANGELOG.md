# Changelog

Formato baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/); versionamento semântico.

## [Não lançado]

### Adicionado
- Distribuição por Docker (`docker-compose.yml`) com painel embutido na API, volumes persistentes e health checks.
- Chave JWT gerada automaticamente na primeira execução.
- A primeira conta criada vira administradora; novos cadastros ficam fechados por padrão.
- Retenção automática de métricas (`APP_METRICS_RETENTION_DAYS`, padrão 30 dias).
- Instalador do agente como serviço systemd e aviso de HTTP sem TLS.
- CI, Dependabot e workflow de release.

### Alterado
- Painel usa URLs relativas (funciona em qualquer host/porta).
- Token de agente inválido retorna 401 (antes 500).
- Listagem de servidores usa uma única consulta para as últimas métricas.

### Removido
- Rota `/api/agent/metrics/history`, que gravava métricas sem validação.
- Módulo de comandos remotos (incompleto), adiado para uma versão futura.
