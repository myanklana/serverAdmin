# Agente

Aplicação Java instalada em cada servidor monitorado. Ela coleta métricas, envia dados à API e recebe somente comandos autorizados.

## Configuração

Crie um arquivo `config.json` a partir de `config.example.json` antes de executar o agente.

## Executar

```bash
mvn package
java -jar target/agent-0.0.1-SNAPSHOT.jar config.json
```

O agente envia métricas para a API a cada cinco segundos usando o token associado ao servidor.
