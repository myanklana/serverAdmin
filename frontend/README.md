# Frontend

## Executar

```bash
npm install
npm run dev
```

A interface abre em `http://localhost:5173` e, por padrão, usa a API em
`http://localhost:8080`. Para outro endereço, defina `VITE_API_URL`.

Aplicação React responsável pelo dashboard, visualização de métricas e operações administrativas.

```text
src/
├── components/  # Componentes reutilizáveis
├── hooks/       # Hooks da aplicação
├── pages/       # Páginas e rotas
├── services/    # Clientes HTTP e WebSocket
├── styles/      # Estilos globais
└── types/       # Tipos TypeScript
```
