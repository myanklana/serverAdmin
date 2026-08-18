# Agente

Aplicação Java instalada em cada servidor monitorado. Ela coleta métricas, envia dados à API e recebe somente comandos autorizados.

## Executar

O painel gera um token seguro e mostra o comando completo logo após o cadastro
do servidor. Copie para esta pasta o JAR empacotado e o script do seu sistema e
execute o comando apresentado. O script cria o `config.json` automaticamente.

Windows:

```powershell
.\start-agent.ps1 -ApiUrl "https://api.seudominio.com" -Token "TOKEN_GERADO"
```

Linux:

```bash
./start-agent.sh https://api.seudominio.com TOKEN_GERADO
```

Para desenvolvimento manual do agente:

```bash
mvn package
java -jar target/agent-0.0.1-SNAPSHOT.jar config.json
```

O agente envia métricas para a API a cada cinco segundos usando o token associado ao servidor.
