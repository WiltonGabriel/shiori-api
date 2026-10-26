# Shiori API 栞 (Hub Oficial)

[![Build & Tests](https://img.shields.io/badge/tests-244%20passed-brightgreen.svg)](pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/version-0.0.14--alpha-blue.svg)](pom.xml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> Back-end robusto, API REST stateless de alta performance e Hub de catalogação do ecossistema **Shiori**. Desenvolvido com foco absoluto em **Soberania do Leitor**, **Privacidade por Design (Privacy by Design)**, desacoplamento arquitetural e resiliência contra censura.

---

## 📖 Visão Geral do Ecossistema

O **Shiori API** atua como o Hub do ecossistema Shiori:
1. **Governança Comunitária do Catálogo:** Metadados enciclopédicos de obras literárias (mangás, webtoons, manhwas, light novels e livros) mantidos de forma auditável.
2. **Soberania Absoluta da Estante:** A estante do leitor (`ItemCatalogo`) armazena snapshots locais autônomos. Se uma obra for removida ou sofrer DMCA no catálogo global, a estante do leitor **permanece 100% intacta e funcional**.
3. **Privacidade e Criptografia em Repouso:** URLs confidenciais de leitura são cifradas via **AES-256-GCM** antes de atingirem o banco de dados.
4. **Clientes Web Desacoplados:** Suporte dinâmico e flexível a CORS (`CORS_ALLOWED_ORIGINS`), permitindo que instâncias independentes do cliente web (`shiori-web`) ou apps mobile consumam o Hub sem restrições.
5. **Padronização RFC 9457:** Respostas de erro padronizadas sob o MIME type `application/problem+json` (`ProblemDetail`).

---

## 🛠️ Stack Tecnológica

| Camada | Tecnologia | Descrição |
|:---|:---|:---|
| **Linguagem** | Java 25 LTS | OpenJDK com modern switch expressions, records e virtual threads |
| **Framework** | Spring Boot 4.x | Ecossistema maduro com Spring Security, Data JPA e Actuator |
| **Persistência** | PostgreSQL 16+ | Modelagem relacional estrita com índices otimizados |
| **Testes** | JUnit 5 + Mockito + AssertJ | Testes unitários e de integração determinísticos (H2 in-memory) |
| **Criptografia** | AES-256-GCM / BCrypt | Proteção de dados confidenciais e credenciais de acesso |
| **Storage S3** | AWS SDK v2 | Suporte compatível com Cloudflare R2, MinIO e AWS S3 |
| **Documentação** | RFC 9457 / Markdown | Documentação aberta na pasta `docs/` |

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- **JDK 25** instalado e configurado no `JAVA_HOME`.
- **Git** para clonagem do repositório.
- **Docker** e Docker Compose (opcional, para instâncias locais de PostgreSQL).

### Passo a Passo

1. **Clonar o repositório:**
   ```bash
   git clone https://github.com/wiltongabriel/shiori-api.git
   cd shiori-api
   ```

2. **Configurar as variáveis de ambiente:**
   ```bash
   cp .env.example .env
   ```
   *(Ajuste as credenciais locais conforme seu ambiente).*

3. **Executar a suíte de testes automatizados:**
   ```bash
   # Windows
   .\mvnw.cmd test

   # Linux / macOS
   ./mvnw test
   ```

4. **Iniciar a aplicação em modo de desenvolvimento:**
   ```bash
   # Windows
   .\mvnw.cmd spring-boot:run

   # Linux / macOS
   ./mvnw spring-boot:run
   ```
   A API estará disponível em: `http://localhost:8080`.

5. **Acessar o Painel Administrativo da Shiori API:**
   Acesse diretamente no navegador: `http://localhost:8080/admin/`.
   O painel é independente do cliente leitor (`shiori-web`), permitindo gerenciar integrações, catálogo, moderação, usuários, feature flags e auditoria. Consulte [`admin-ui/README.md`](admin-ui/README.md) para detalhes.

6. **Testar endpoint de saúde (Health Check):**
   ```bash
   curl http://localhost:8080/actuator/health
   ```

---

## ⚙️ Variáveis de Ambiente (`.env.example`)

A API lê sua configuração a partir de variáveis de ambiente. Um arquivo de modelo `.env.example` é fornecido na raiz:

```env
# Porta e Perfil
PORT=8080
SPRING_PROFILES_ACTIVE=dev

# Banco de Dados PostgreSQL
DB_HOST=localhost
DB_PORT=5432
DB_NAME=shiori
DB_USER=postgres
DB_PASSWORD=postgres

# Autenticação JWT (mínimo 256 bits)
JWT_SECRET=super_secret_jwt_key_shiori_minimum_32_bytes_long!
JWT_EXPIRATION=86400000

# CORS Dinâmico para Clientes Desacoplados (* ou domínios específicos)
CORS_ALLOWED_ORIGINS=*

# Chave Criptográfica AES-256 para URLs de Leitura
SHIORI_CRYPTO_SECRET_KEY=bXlfc3VwZXJfc2VjcmV0X2tleV8zMl9ieXRlc19sb25nIQ==
```

---

## 📚 Documentação Técnica Aprofundada

Documentos detalhados estão disponíveis na pasta [`docs/`](docs/):

- 🏛️ **[Arquitetura do Sistema](docs/ARQUITETURA.md):** Segregação de camadas, DDD, Princípio da Responsabilidade Única (SRP), snapshots de soberania e modelo criptográfico.
- 🔌 **[Guia de Integração de Clientes](docs/INTEGRACAO_CLIENTE.md):** Manual completo de endpoints públicos, ciclo de vida de autenticação JWT, cabeçalhos HTTP e handshake mesh.
- 🚢 **[Deploy GitOps & Infraestrutura](docs/DEPLOY_GITOPS.md):** Esteira de CI/CD automatizada, Portainer, variáveis de produção e diretrizes restritas de acesso SSH operacional.

---

## 🛡️ Infraestrutura e Acesso Operacional

- **Esteira GitOps:** As implantações em produção são automatizadas pelo **Portainer** acionado pela esteira do GitHub Actions a cada commit na branch `main`.
- **Acesso SSH Restrito:** O acesso operacional via SSH (`usuario@seu-servidor-ip`) é estritamente restrito para auditoria de infraestrutura e gestão de containers. Nenhuma modificação manual de código é permitida diretamente em produção.

---

## 📄 Licença

Este projeto é software livre licenciado sob os termos da licença [MIT](LICENSE).
