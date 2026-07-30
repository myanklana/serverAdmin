# Melhorias de segurança a implementar

Este documento reúne as melhorias identificadas na autenticação, autorização e proteção da API.

## Prioridade alta

### 1. Corrigir a codificação dos textos de resposta

**Situação atual:** há mensagens exibidas com caracteres corrompidos, como `NÃ£o autenticado` e `RequisiÃ§Ã£o invÃ¡lida`.

**Implementação:** salvar os fontes como UTF-8 e corrigir os textos em `RestAuthenticationEntryPoint`, `ApiExceptionHandler` e validações relacionadas.

**Critério de aceite:** respostas JSON retornam corretamente mensagens como `Não autenticado`, `Requisição inválida` e `IP inválido`.

### 2. Não ocultar erros inesperados no filtro JWT

**Situação atual:** `JwtAuthenticationFilter` descarta qualquer `RuntimeException` durante a validação/autenticação.

**Risco:** uma falha de banco de dados ou um erro de programação pode parecer apenas um token inválido, dificultando diagnóstico e ocultando indisponibilidades.

**Implementação:** capturar somente `InvalidTokenException` para deixar a requisição não autenticada. Registrar e propagar exceções inesperadas para o tratamento global.

**Critério de aceite:** token inválido retorna 401; erros de infraestrutura retornam 500 e ficam registrados em log sem expor detalhes sensíveis ao cliente.

### 3. Padronizar respostas de erro de autenticação

**Situação atual:** o `RestAuthenticationEntryPoint` escreve JSON manualmente, enquanto o restante da API usa `ProblemDetail`.

**Implementação:** gerar a resposta 401 no formato RFC 9457 (`application/problem+json`), usando o mesmo padrão do `ApiExceptionHandler`.

**Critério de aceite:** erros 401 contêm, de forma consistente, `type`, `title`, `status`, `detail` e, quando aplicável, `instance`.

### 4. Limitar tentativas de login e cadastro

**Situação atual:** `/login` e `/register` são públicos e não possuem limitação de requisições.

**Risco:** força bruta, enumeração de usuários e criação abusiva de contas.

**Implementação:** aplicar rate limit por IP e, no login, também por nome de usuário. Avaliar CAPTCHA ou bloqueio progressivo após repetidas falhas. Decidir se o cadastro deve continuar público ou exigir convite/permissão administrativa.

**Critério de aceite:** tentativas excessivas recebem HTTP 429, sem revelar se um usuário existe.

## Prioridade média

### 5. Adicionar papéis e permissões

**Situação atual:** a API diferencia somente requisições autenticadas e não autenticadas. A proteção por proprietário de servidor é feita no controlador.

**Implementação:** definir papéis, por exemplo `ROLE_USER` e `ROLE_ADMIN`, e usar autorização declarativa (`@PreAuthorize`) nas operações administrativas. Manter a verificação de propriedade dos servidores.

**Critério de aceite:** apenas administradores acessam funções administrativas; usuários só acessam recursos que lhes pertencem.

### 6. Implementar revogação e renovação de tokens JWT

**Situação atual:** tokens permanecem válidos até a expiração configurada, atualmente 120 minutos.

**Implementação:** incluir um identificador de token (`jti`) e uma estratégia de revogação para logout, mudança de senha e resposta a incidentes. Considerar access tokens curtos com refresh tokens rotacionados e revogáveis.

**Critério de aceite:** logout ou troca de senha invalida tokens ativos; refresh token não pode ser reutilizado após rotação.

### 7. Reforçar a segurança dos tokens de agente

**Situação atual:** o token é protegido por BCrypt, mas há um SHA-256 determinístico para busca no banco.

**Implementação:** gerar tokens exclusivamente com fonte criptograficamente segura e alta entropia. Usar HMAC-SHA-256, com segredo da aplicação, para o índice de busca; manter BCrypt ou Argon2 para a confirmação do token.

**Critério de aceite:** tokens são aleatórios, têm entropia suficiente e um vazamento do banco não permite verificar facilmente candidatos ao token sem o segredo da aplicação.

### 8. Tornar a política de hash de senha explícita

**Situação atual:** o BCrypt usa o custo padrão da biblioteca.

**Implementação:** configurar explicitamente o fator de custo após medição de desempenho no ambiente de produção. Avaliar Argon2id como alternativa.

**Critério de aceite:** a configuração está documentada, é compatível com a latência esperada e pode ser aumentada em futuras migrações.

### 9. Endurecer headers HTTP

**Implementação:** definir headers de segurança adequados à API, incluindo `X-Content-Type-Options: nosniff`, política de cache para respostas autenticadas e uma política de transporte HTTPS em produção (HSTS).

**Critério de aceite:** respostas autenticadas não são armazenadas por caches compartilhados e os headers configurados estão presentes em produção.

## Testes necessários

- Requisição a rota protegida sem token retorna 401 no formato `application/problem+json`.
- Token ausente, malformado, adulterado, expirado ou com emissor/audiência incorretos retorna 401.
- Falha inesperada ao consultar o usuário não é mascarada como token inválido.
- Um usuário não consegue ler nem excluir um servidor pertencente a outro usuário.
- Origem não permitida não recebe headers CORS permissivos.
- Tentativas repetidas de login ou cadastro são limitadas com HTTP 429.
- Logout, revogação ou alteração de senha invalida tokens conforme a estratégia adotada.
- Token de agente inválido não autentica o agente; token válido autentica somente o servidor correspondente.

---

## Observabilidade, métricas e banco de dados

### 10. Definir retenção e agregação das métricas

**Situação atual:** cada agente envia uma amostra a cada cinco segundos, cerca de 17 mil registros por agente a cada dia.

**Risco:** a tabela `metrics` crescerá sem limite, degradando consultas, backups e custos de armazenamento.

**Implementação:** definir retenção, agregação por minuto e hora, limpeza automática e particionamento por data quando o volume justificar.

**Critério de aceite:** dados respeitam a retenção definida e consultas recentes e agregadas mantêm desempenho previsível.

### 11. Tornar a ingestão de métricas atômica e validada

**Situação atual:** a atualização do status do servidor e a gravação da métrica são operações separadas.

**Implementação:** executar a ingestão em transação única; validar timestamps, rejeitando amostras muito antigas ou futuras; limitar payloads e campos textuais.

**Critério de aceite:** não existe estado em que o servidor fique online sem métrica gravada, ou métrica seja gravada sem atualização de `lastSeen`.

### 12. Remover consultas N+1 do dashboard

**Situação atual:** a listagem consulta a última métrica individualmente para cada servidor.

**Implementação:** criar query projetada que retorne servidores e a métrica mais recente em uma única consulta, com paginação.

**Critério de aceite:** o número de consultas não cresce linearmente com o número de servidores exibidos.

### 13. Corrigir coleta de disco e melhorar rede

**Situação atual:** o agente usa discos físicos para capacidade total e file systems montados para uso, que podem representar conjuntos diferentes. A rede é enviada apenas como contador acumulado.

**Implementação:** calcular total e usado a partir dos mesmos pontos de montagem; enviar métricas por file system quando necessário; calcular taxa de rede entre amostras.

**Critério de aceite:** percentuais de disco não ultrapassam 100% por inconsistência de origem e o dashboard pode exibir tráfego por intervalo.

## Agente e comunicação

### 14. Tornar o agente resiliente a falhas de rede

**Situação atual:** métricas são perdidas quando a API está indisponível.

**Implementação:** aplicar retry com backoff exponencial e limite, fila local temporária limitada, logs estruturados e encerramento gracioso.

**Critério de aceite:** indisponibilidade temporária não derruba o agente e métricas pendentes são enviadas após a recuperação, dentro dos limites configurados.

### 15. Documentar e reforçar a instalação do agente

**Implementação:** documentar permissões restritas para `config.json`, instalação como serviço Linux e Windows, proxy, timeout, TLS e certificados corporativos. Enviar versão do agente e capacidades no primeiro contato.

**Critério de aceite:** um operador instala, atualiza e diagnostica o agente sem acessar o código-fonte.

## API, frontend e experiência de uso

### 16. Versionar e documentar os contratos da API

**Implementação:** adotar prefixo como `/api/v1`, documentar exemplos JSON, códigos de erro, paginação e contratos de métricas no OpenAPI.

**Critério de aceite:** frontend e agente podem evoluir de forma independente usando o contrato publicado.

### 17. Separar responsabilidades da API

**Situação atual:** controladores concentram HTTP, acesso a repositório, regras de negócio e montagem de respostas.

**Implementação:** introduzir serviços de domínio, DTOs dedicados, mapeadores e transações no nível de serviço.

**Critério de aceite:** regras de negócio são testáveis sem HTTP e controladores ficam limitados ao contrato web.

### 18. Evoluir o dashboard

**Implementação:** separar páginas, componentes, hooks e clientes HTTP; mostrar valores absolutos, rede, uptime, última comunicação e motivo de indisponibilidade. Adicionar edição, exclusão confirmada e rotação de token.

**Critério de aceite:** o painel fornece contexto suficiente para identificar o estado de cada servidor e ações sensíveis exigem confirmação.

### 19. Preparar gráficos e atualizações em tempo real

**Implementação:** criar endpoint paginado de histórico por servidor; introduzir gráficos para CPU, memória, disco e rede; substituir polling por WebSocket ou SSE.

**Critério de aceite:** gráficos usam séries agregadas e novas métricas aparecem sem recarregar a página.

## Operação, testes e entrega

### 20. Completar a infraestrutura local e de produção

**Implementação:** adicionar volume persistente, health check e variáveis por `.env` ao Docker Compose; remover credenciais padrão de ambientes não locais; criar imagens para API, frontend e agente.

**Critério de aceite:** o ambiente sobe de forma reproduzível, preserva dados entre reinícios e não depende de segredos no repositório.

### 21. Automatizar qualidade e testes de integração

**Implementação:** configurar CI para build dos três módulos, lint do frontend, análise de dependências e migrations. Adicionar Testcontainers com PostgreSQL e testes para autenticação, isolamento entre usuários, ingestão e status offline.

**Critério de aceite:** alterações que quebrem contrato, migration ou fluxo agente–API–dashboard falham automaticamente no CI.

### 22. Padronizar codificação e estrutura do repositório

**Implementação:** salvar fontes e documentação em UTF-8 sem BOM; aplicar `.editorconfig`; corrigir textos corrompidos; definir estratégia única de build para API, agente e frontend.

**Critério de aceite:** textos em português aparecem corretamente e o processo de build completo é documentado e reproduzível.
