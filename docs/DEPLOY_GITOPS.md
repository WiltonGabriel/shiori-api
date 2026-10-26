# Guia de Deploy GitOps e Alinhamento de Infraestrutura (Shiori API)

Este documento estabelece as diretrizes oficiais de infraestrutura, esteira de integração contínua (CI/CD) via GitOps, gerenciamento de containers no **Portainer** e protocolos de segurança operacional.

---

## 1. Filosofia de Implantação Contínua (GitOps)

O ecossistema Shiori adota o modelo **GitOps**:
1. **Repositório como Única Fonte de Verdade:** Todo o estado desejado da aplicação, parâmetros de build e especificações de container estão versionados no Git.
2. **Esteira Automatizada de CI/CD:**
   - Cada commit na branch `main` executa a validação de testes automatizados e compilação de imagens Docker multi-arch via GitHub Actions.
   - A atualização dos containers em produção é orquestrada automaticamente pelo Portainer via webhook ou sincronização de stack Git.

---

## 2. ⚠️ Diretriz de Segurança e Acesso Operacional

> [!CAUTION]
> **POLÍTICA ESTRITA DE ACESSO OPERACIONAL AO SERVIDOR DE PRODUÇÃO:**
>
> 1. **Ambiente de Produção Gerenciado por GitOps:** O ambiente de produção opera sob automação estrita via **Portainer**.
> 2. **Acesso SSH Restrito:** O acesso direto via SSH ao servidor (no host `usuario@seu-servidor-ip`) é **estritamente restrito a fins de auditoria de infraestrutura, inspeção de logs e gerenciamento de containers** no diretório de serviços do projeto.
> 3. **Proibição de Alterações Manuais:** É terminantemente proibido editar código-fonte, alterar arquivos de runtime ou aplicar "hotfixes" diretamente no servidor de produção. **Toda e qualquer alteração de código deve ser submetida via commit no repositório GitHub e passar obrigatoriamente pelos testes automatizados e pela esteira do GitHub Actions.**

---

## 3. Variáveis de Ambiente de Produção (`.env`)

A aplicação requer a configuração das seguintes variáveis de ambiente (conforme detalhado em `.env.example`):

| Variável | Descrição | Exemplo em Produção |
|:---|:---|:---|
| `PORT` | Porta de escuta da API Java | `8080` |
| `SPRING_PROFILES_ACTIVE` | Perfil ativo do Spring Boot | `prod` |
| `DB_HOST` | Host do banco de dados PostgreSQL | `postgres` ou endereço de rede interno |
| `DB_PORT` | Porta do PostgreSQL | `5432` |
| `DB_NAME` | Nome do banco de dados | `shiori` |
| `DB_USER` | Usuário do banco de dados | `shiori_user` |
| `DB_PASSWORD` | Senha criptográfica do banco | *(Chave segura e complexa)* |
| `JWT_SECRET` | Chave HMAC-SHA256 (mínimo 256 bits) | *(Chave de alta entropia)* |
| `JWT_EXPIRATION` | Tempo de expiração do token (ms) | `86400000` (24 horas) |
| `CORS_ALLOWED_ORIGINS` | Origens autorizadas para clientes web | `*` ou `https://shiori.meudominio.com` |
| `SHIORI_CRYPTO_SECRET_KEY` | Chave AES-256 para URLs confidenciais | *(Chave de 32 bytes codificada em Base64)* |
| `S3_ENDPOINT` | Endpoint S3 (R2, MinIO ou AWS) | `https://<id>.r2.cloudflarestorage.com` |
| `S3_ACCESS_KEY` | Chave de acesso S3 | *(Credencial de storage)* |
| `S3_SECRET_KEY` | Chave secreta S3 | *(Credencial de storage)* |
| `S3_BUCKET_NAME` | Nome do bucket de mídias | `shiori-media` |

---

## 4. Execução via Docker Compose

Para orquestrar a API junto ao banco de dados e Redis/Storage local:

```bash
# 1. Copiar variáveis de ambiente
cp .env.example .env

# 2. Iniciar containers em segundo plano
docker compose up -d --build

# 3. Verificar logs de inicialização
docker compose logs -f api
```

Para inspecionar o status do serviço:
```bash
curl http://localhost:8080/actuator/health
```
