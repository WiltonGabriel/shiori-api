# Painel Administrativo Autônomo - Shiori API (`admin-ui`)

Interface administrativa nativa e desacoplada para gerenciamento da instância do backend **Shiori API**.

Permite que operadores que hospedam exclusivamente a API administrem o catálogo, moderação, configurações, integrações bibliográficas, auditoria, usuários e nós federados diretamente pelo navegador em `/admin/`, sem necessidade de instalar ou depender do `shiori-web`.

---

## 🏗️ Arquitetura

- **Frontend:** React 19 + TypeScript + Vite + Lucide React.
- **Design System:** Design Tokens Shiori (Glassmorphism, Tipografia fluida, Obsidian Dark padrão e Slate Light alternável).
- **Backend Delivery:** Servido nativamente pelo Spring Boot através de `AdminSpaController` e `AdminSpaWebConfig` em `classpath:/static/admin/`.
- **Roteamento SPA:** Rotas cliente (ex: `/admin/integracoes`, `/admin/usuarios`, `/admin/dashboard`) entregam `index.html` com suporte a recarregamento direto sem erro 404/500.
- **Segurança & RBAC:** Endpoints `/admin` e `/admin/**` entregam os recursos estáticos publicamente; todos os dados e ações sensíveis em `/api/admin/**` exigem autenticação via Bearer Token JWT e autorização estrita `ROLE_ADMIN`.

---

## 🚀 Como Executar

### 1. Servido Diretamente pelo Spring Boot (Modo Produção/Embarcado)

O build do frontend já está compilado e embutido em `src/main/resources/static/admin/`.

1. Inicie a API Shiori:
   ```bash
   ./mvnw spring-boot:run
   ```
2. Acesse no navegador:
   ```
   http://localhost:8080/admin/
   ```
   *(Caso a porta esteja configurada como 8082 ou outra, acesse `http://localhost:<PORTA>/admin/`)*.

3. Credenciais de Acesso:
   - **E-mail:** `sys_overseer@shiori.dev`
   - **Senha padrão inicial:** Definida pela variável de ambiente `ADMIN_DEFAULT_PASSWORD` (ou gerada e exibida nos logs do Spring Boot caso não informada).

---

### 2. Desenvolvimento com Hot-Reload (Vite Dev Server)

Para desenvolver a interface com recarregamento instantâneo:

```bash
cd admin-ui
npm run dev
```

O Vite subirá em `http://localhost:5173/admin/` e configurará automaticamente um proxy reverso transparente para redirecionar requisições `/api` e `/actuator` para o backend Shiori (`http://localhost:8082` ou porta configurada em `vite.config.ts`).

---

## 🛠️ Como Compilar e Atualizar os Ativos Embarcados

Para gerar uma nova versão compilada dos arquivos estáticos para o Spring Boot:

```bash
cd admin-ui
npm run build
```

Os arquivos otimizados e versionados serão gerados diretamente em `../src/main/resources/static/admin/`.

---

## 📋 Módulos e Funcionalidades Disponíveis

| Módulo | Endpoint Backend | Descrição |
| :--- | :--- | :--- |
| **Dashboard** | `GET /api/admin/dashboard`, `GET /actuator/health` | Métricas gerais de obras, edições, usuários, relatórios pendentes, status dos serviços e saúde da instância. |
| **Integrações** | `GET/POST /api/admin/integracoes/**` | Gerenciamento dos provedores bibliográficos (AniList, Kitsu, MangaDex, OpenLibrary), ativação/desativação e ferramenta interativa de busca externa ao vivo. |
| **Configurações** | `GET/PATCH /api/admin/configs/feature-flags` | Gerenciamento de Feature Flags com exigência de justificativa de auditoria obrigatória. |
| **Catálogo** | `GET/POST /api/admin/curadoria/**` | Busca profunda de obras, oficialização, override de metadados, merge de obras duplicadas, gerenciamento de gêneros e fusão de tags comunitárias. |
| **Moderação** | `GET/POST /api/admin/moderacao/**` | Fila de relatórios de comentários abusivos, denúncias de direitos autorais de obras e revisão com diff de sugestões comunitárias de edição. |
| **Usuários** | `GET/PUT /api/admin/usuarios/**` | Listagem paginada, atribuição de cargos (`ROLE_ADMIN`, `ROLE_CURADOR`, `ROLE_MODERADOR`, `ROLE_LEITOR`), aplicação de advertências (strikes), suspensões temporárias e banimento. |
| **Broadcasts** | `GET/POST/DELETE /api/admin/broadcasts/**` | Criação, acompanhamento e desativação de comunicados globais do sistema. |
| **Federação** | `GET/POST /api/admin/federacao/**` | Gestão dos nós da malha federada e aprovação/rejeição de pedidos de pareamento (handshakes). |
| **Auditoria** | `GET /api/admin/auditoria` | Trilha de auditoria administrativa completa com filtro por ação, módulo e identificador de registro. |

---

## 🧪 Testes Automatizados

Os testes de integração que garantem o roteamento SPA, a entrega de ativos estáticos e a proteção RBAC da API estão em:
- `src/test/java/dev/shiori/infra/config/AdminSpaWebIntegrationTest.java`

Executar os testes:
```bash
./mvnw test -Dtest=AdminSpaWebIntegrationTest
```
