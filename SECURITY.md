# Política de segurança

## Reportar uma vulnerabilidade

Não abra uma issue pública. Use **Security → Report a vulnerability** neste repositório
(GitHub private vulnerability reporting). Responderemos em até 7 dias.

## Versões suportadas

Apenas a versão mais recente recebe correções de segurança.

## Boas práticas para quem instala

- Publique o painel somente atrás de HTTPS (veja [docs/instalacao.md](docs/instalacao.md)).
- Mantenha `APP_REGISTRATION_OPEN=false` (padrão); a primeira conta criada é a administradora.
- Não exponha a porta do PostgreSQL. O `docker-compose.yml` já não a publica.
- Faça backup dos volumes `postgres-data` e `app-data` (este último guarda a chave de assinatura dos tokens).
