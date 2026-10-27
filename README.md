# Shiori API 栞 (Hub de Catalogação e Serviços)

[![Build & Tests](https://img.shields.io/badge/tests-254%20passed-brightgreen.svg)](pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-0.0.16--alpha-blue.svg)](pom.xml)
[![Status](https://img.shields.io/badge/status-active%20alpha-orange.svg)](docs/memory/roadmap_e_escalonamento.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Cansado de perder sua estante quando agregadores de leitura caem?**  
> O **Shiori** foi concebido para resolver essa dor: criar um **snapshot soberano da sua estante pessoal** no seu próprio dispositivo — se uma obra sumir do catálogo global por DMCA ou encerramento de servidores externos, seus registros, capítulos lidos e notas permanecem 100% intactos.

O **Shiori API** é a camada de serviços e catálogo do ecossistema Shiori: uma API REST stateless em Java 25 / Spring Boot 4 com modelagem relacional estrita no PostgreSQL, governança auditável de metadados literários e foco contínuo em privacidade e segurança.

> [!NOTE]
> **Estágio do Projeto:** O Shiori está em fase **Alpha ativa**. O núcleo de domínio e dados já é funcional e maduro, mas continuamos evoluindo e refinando os contratos públicos, segurança e testes antes de uma abertura mais ampla à comunidade. Feedbacks e discussões de arquitetura são muito bem-vindos!

---

## 📖 Proposta de Valor e Princípios

1. **Soberania do Leitor:** O leitor é dono exclusivo do seu histórico de leitura. Os itens da estante preservam snapshots autônomos de metadados.
2. **Governança Comunitária de Metadados:** Catálogo colaborativo com busca avançada multicritério, moderação transparente de tags e sugestões de edição pelos usuários.
3. **Privacidade por Padrão:** Links confidenciais de fontes de leitura são criptografados em repouso com **AES-256-GCM** antes de qualquer persistência.
4. **Clientes Desacoplados:** Arquitetura flexível para clientes web SPA independentes (`shiori-web`), extensões de navegador e futuros apps mobile.
5. **Erros Padronizados (RFC 9457):** Todas as falhas de API retornam payloads `application/problem+json` (`ProblemDetail`), facilitando o consumo por qualquer cliente.

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Descrição |
|:---|:---|:---|
| **Linguagem** | Java 25 LTS | Switch expressions, records, virtual threads e tipagem moderna |
| **Framework** | Spring Boot 4.x | Spring Data JPA, Spring Security, Actuator e Bean Validation |
| **Persistência** | PostgreSQL 16+ | Migrações determinísticas com Flyway e modelagem relacional |
| **Testes** | JUnit 5 + Mockito + AssertJ | Testes unitários, de fatias de contexto e testes de API com MockMvc |
| **Criptografia** | AES-256-GCM / BCrypt | Cifragem de campos confidenciais e senhas com PBKDF2/BCrypt |
| **Storage S3** | AWS SDK v2 | Compatível com Cloudflare R2, MinIO ou AWS S3 para capas e banners |
| **Padrão de Erros** | RFC 9457 Problem Details | Respostas semânticas e previsíveis para todos os códigos HTTP de erro |

---

## 🚀 Como Executar Localmente (Quickstart)

### Opção A — Subida Rápida com Docker Compose (Recomendado)

O repositório inclui suporte a Docker Compose para orquestrar a aplicação e o banco de dados:

1. **Clonar o repositório:**
   ```bash
   git clone https://github.com/wiltongabriel/shiori-api.git
   cd shiori-api
   ```

2. **Configurar as variáveis de ambiente:**
   ```bash
   cp .env.example .env
   ```
   *(Ajuste as credenciais locais em `.env` se necessário).*

3. **Subir os serviços:**
   ```bash
   docker compose up -d
   ```
   A API inicializará e estará pronta em: `http://localhost:8080`.

---

### Opção B — Execução Nativa com Maven

**Pré-requisitos:**
- JDK 25 configurado (`java -version`).
- PostgreSQL em execução na porta 5432 (ou container Docker ativo).

1. **Executar a suíte de testes:**
   ```bash
   # Linux / macOS
   ./mvnw test

   # Windows
   .\mvnw.cmd test
   ```

2. **Iniciar a aplicação em modo de desenvolvimento:**
   ```bash
   # Linux / macOS
   ./mvnw spring-boot:run

   # Windows
   .\mvnw.cmd spring-boot:run
   ```
   A API estará disponível em `http://localhost:8080`.

3. **Acessar o Painel Administrativo Independente da Shiori API:**
   Acesse diretamente no navegador: `http://localhost:8080/admin/`.
   O painel é embutido na própria API e desacoplado do cliente leitor (`shiori-web`), permitindo gerenciar integrações, curadoria do catálogo, taxonomia de gêneros, governança de tags comunitárias, moderação, usuários, feature flags e auditoria. Consulte [`admin-ui/README.md`](admin-ui/README.md) para documentação completa.

4. **Verificar a integridade (Health Check):**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## ⚙️ Variáveis de Ambiente e Segurança

Copie `.env.example` para `.env` e configure os parâmetros essenciais:

```env
# Servidor e Porta
PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Banco de Dados PostgreSQL
DB_URL=jdbc:postgresql://localhost:5432/shiori
DB_USERNAME=shiori
DB_PASSWORD=shiori_secret

# Criptografia AES-256 para URLs confidenciais (chave de 256 bits em hex)
SHIORI_CRYPTO_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef

# Autenticação JWT (mínimo 256 bits)
JWT_SECRET=super_secret_jwt_key_shiori_minimum_32_bytes_long!
JWT_EXPIRATION_MS=86400000

# CORS — Origens Permitidas para Clientes Web
# AVISO: Nunca utilize '*' em produção. Liste suas origens confiáveis:
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# Bootstrap de Administrador Master (opcional para primeira inicialização)
ADMIN_DEFAULT_PASSWORD=minha_senha_mestra_segura
```

---

## 🎛️ Painel Administrativo Embutido (Shiori API Admin)

A Shiori API inclui uma interface administrativa SPA (Single Page Application) embutida em `/admin`, permitindo a gestão da instância diretamente pelo navegador sem dependência do cliente leitor (`shiori-web`):

- **Dashboard de Métricas:** Estatísticas de acervo, usuários, estantes ativas e telemetria básica do Spring Boot Actuator.
- **Curadoria do Catálogo & Taxonomia:** Busca avançada com Criteria API, tags comunitárias com pontuação de relevância, gestão de gêneros literários canônicos, mesclagem de obras duplicadas e overrides de metadados.
- **Governança & Moderação:** Fila de moderação de tags comunitárias (aprovação direta ou rejeição com justificativa obrigatória), auditoria de votos e moderação de denúncias de conteúdo.
- **Gestão de Usuários:** Listagem paginada, aplicação de advertências (strikes) e suspensões preventivas com justificativa formal.
- **Integrações Externas:** Teste de conectividade e status dos provedores de catálogo (Kitsu para novels/mangas e Open Library para livros).
- **Federação & Pareamentos:** Gestão de nós vizinhos na malha federada e handshake de instâncias pareadas.
- **Trilha de Auditoria:** Histórico imutável de ações administrativas filtrado por categorias semânticas.

Para mais detalhes sobre a stack, arquitetura do frontend e fluxo de build, consulte [`admin-ui/README.md`](admin-ui/README.md).

---

## 🎯 Prioridades Técnicas e Roadmap Aberto (P0)

O projeto prioriza os seguintes passos técnicos antes de abrir oficialmente para grandes contribuições externas:

1. **Testcontainers com PostgreSQL 16:** Transição dos testes de integração para instâncias reais de Postgres via Testcontainers, aposentando H2 in-memory.
2. **CORS Seguro por Padrão:** Restrição estrita de origens permitidas em produção sem curingas permissivos.
3. **Ciclo de Vida do JWT:** Implementação de rotação de Refresh Tokens e blacklist/revogação em logouts explícitos.
4. **Proteção Anti-Brute-Force e Rate Limiting:** Limitação de requisições por IP e conta nos endpoints de autenticação e busca.
5. **Congelamento da API Pública:** Estabilização de rotas canônicas sob o prefixo `/api/v1/...`.

Consulte o documento detalhado em [`docs/memory/roadmap_e_escalonamento.md`](docs/memory/roadmap_e_escalonamento.md).

---

## 📚 Documentação Técnica

- 🏛️ **[Arquitetura do Sistema](docs/ARQUITETURA.md):** Camadas, princípios SOLID, Clean Code e padrões de persistência.
- 🎛️ **[Painel Administrativo da API](admin-ui/README.md):** Documentação do frontend SPA independente embutido em `/admin`.
- 🔌 **[Manual de Integração de Clientes](docs/INTEGRACAO_CLIENTE.md):** Autenticação JWT, endpoints de catálogo e payloads RFC 9457.
- 🚢 **[Guia Operacional da Instância de Referência](docs/DEPLOY-REFERENCE.md):** Detalhes específicos de GitOps e manutenção da infraestrutura de referência operada pelo mantenedor.

---

## 📄 Licença

Este projeto é software livre licenciado sob os termos da licença [MIT](LICENSE).
