# Guia Público de Integração com a Shiori API

Bem-vindo ao Guia Oficial de Integração da **Shiori API**. Este documento foi elaborado para desenvolvedores Open Source que desejam consumir a API para integrar ferramentas, bots ou construir clientes front-end alternativos (Mobile, Desktop, CLI ou Web).

---

## 1. Arquitetura Geral

A Shiori API é uma API RESTful de alta performance desenvolvida com **Java e Spring Boot**, desenhada sob os princípios de **Clean Architecture**, **Domain-Driven Design (DDD)** e comunicação totalmente **Stateless**:

- **Segurança:** Autenticação via JSON Web Tokens (JWT) assinados via HMAC-SHA256.
- **Isolamento de Domínio:** Entidades robustas com regras de negócio blindadas.
- **Resiliência:** Rate limiting integrado por IP e interceptores de exceção globais.
- **Conformidade com Padrões Web:** Respostas de erro padronizadas seguindo a especificação **RFC 9457 (`ProblemDetail`)**.
- **Armazenamento de Objetos:** Integração S3 compatível (Cloudflare R2, MinIO ou AWS S3) para mídias de capas e avatares.

---

## 2. Autenticação e Autorização (JWT)

A maioria dos endpoints de personalização (Estante, Feed, Conexões) exige que o cliente envie o token de acesso no cabeçalho HTTP:

```http
Authorization: Bearer <seu_token_jwt>
```

### Endpoints de Autenticação (`/api/auth`)

#### Registrar Novo Usuário
- **Endpoint:** `POST /api/auth/registrar`
- **Acesso:** Público
- **Payload (JSON):**
  ```json
  {
    "nome": "Leitor Noturno",
    "email": "leitor@shiori.dev",
    "senha": "SenhaForte123!"
  }
  ```
- **Resposta Sucesso (201 Created):**
  ```json
  {
    "id": 42,
    "nome": "Leitor Noturno",
    "email": "leitor@shiori.dev",
    "roles": ["ROLE_USER"]
  }
  ```

#### Obter Token (Login)
- **Endpoint:** `POST /api/auth/login`
- **Acesso:** Público
- **Payload (JSON):**
  ```json
  {
    "email": "leitor@shiori.dev",
    "senha": "SenhaForte123!"
  }
  ```
- **Resposta Sucesso (200 OK):**
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tipo": "Bearer",
    "usuario": {
      "id": 42,
      "nome": "Leitor Noturno",
      "email": "leitor@shiori.dev",
      "roles": ["ROLE_USER"]
    }
  }
  ```

---

## 3. Padrão de Resposta de Erros (RFC 9457 `ProblemDetail`)

A Shiori API não retorna mensagens de erro ad-hoc. Todos os erros utilizam o MIME Type `application/problem+json` e seguem a RFC 9457:

```json
{
  "type": "https://shiori.dev/erros/validacao",
  "title": "Erro de Validação",
  "status": 400,
  "detail": "Um ou mais campos contêm erros de validação.",
  "path": "/obras",
  "timestamp": "2026-10-19T06:12:00.000Z",
  "invalidParams": [
    {
      "campo": "titulo",
      "motivo": "O título da obra é obrigatório."
    }
  ]
}
```

### Principais Tipos de Erro
| Status HTTP | URI do Tipo (`type`) | Descrição |
|:---|:---|:---|
| **400 Bad Request** | `https://shiori.dev/erros/requisicao-invalida` | Payload malformatado ou violação de regras de validação. |
| **401 Unauthorized** | `https://shiori.dev/erros/nao-autorizado` | Token Bearer ausente, inválido ou expirado. |
| **403 Forbidden** | `https://shiori.dev/erros/acesso-negado` | Permissão insuficiente (ex: rota restrita a `ROLE_ADMIN`). |
| **404 Not Found** | `https://shiori.dev/erros/recurso-nao-encontrado` | Obra, item de catálogo ou usuário inexistente. |
| **409 Conflict** | `https://shiori.dev/erros/conflito` | Tentativa de duplicidade (ex: email já cadastrado). |
| **429 Too Many Requests**| `https://shiori.dev/erros/limite-excedido` | Limite de requisições excedido pelo Rate Limiter. |

---

## 4. Catálogo de Endpoints Essenciais

Abaixo estão os endpoints fundamentais para quem deseja criar uma interface completa de usuário:

### 4.1 Vitrine e Home Pública
- **`GET /obras/home`** (ou `/api/obras/home`)
  - **Acesso:** Público (não requer token)
  - **Finalidade:** Retorna uma lista curada de obras populares e recentemente atualizadas para a tela inicial.

### 4.2 Catálogo Global de Obras
- **`GET /obras`** (ou `/api/obras`)
  - **Acesso:** Público
  - **Query Params Opcionais:** `status` (`OFICIAL`, `COMUNITARIO`, `EM_ANALISE`)
  - **Finalidade:** Listagem geral do acervo para busca, filtros e exploração.
- **`GET /obras/busca-avancada`** (ou `/api/obras/busca-avancada`)
  - **Acesso:** Público (não requer token)
  - **Finalidade:** Mecanismo avançado de busca e filtragem do catálogo inspirado no Series Finder, executado de forma paginada e performática diretamente no banco via Criteria API.
  - **Query Params Suportados:**
    - `termo` (string): Busca textual case-insensitive no título principal ou título original.
    - `tipos` (enum/lista): Formatos literários (`MANGA`, `MANHWA`, `MANHUA`, `LIGHT_NOVEL`, `WEB_NOVEL`, `COMIC`, `LIVRO`).
    - `status` (enum/lista): Status de publicação (`EM_LANCAMENTO`, `CONCLUIDO`, `EM_HIATO`, `CANCELADO`, `NAO_LANCADO`).
    - `minCapitulos` (inteiro >= 0): Mínimo de capítulos lançados.
    - `maxCapitulos` (inteiro >= 0): Máximo de capítulos lançados.
    - `minNota` (decimal de 0.0 a 10.0): Nota comunitária mínima.
    - `maxNota` (decimal de 0.0 a 10.0): Nota comunitária máxima.
    - `generosIncluidos` (lista de strings): Nomes ou slugs de gêneros a incluir.
    - `generosModo` (`AND` ou `OR`, padrão `AND`): `AND` exige que a obra possua todos os gêneros; `OR` exige pelo menos um.
    - `generosExcluidos` (lista de strings): Nomes ou slugs de gêneros proibidos (exclusão estrita `NOT`).
    - `tagsIncluidas` (lista de strings): Nomes ou slugs de tags a incluir.
    - `tagsModo` (`AND` ou `OR`, padrão `AND`): `AND` exige todas as tags; `OR` exige ao menos uma.
    - `tagsExcluidas` (lista de strings): Nomes ou slugs de tags proibidas (exclusão estrita `NOT`).
    - `ordenacao` (enum, padrão `NOTA_DESC`): `NOTA_DESC`, `NOTA_ASC`, `CAPITULOS_DESC`, `CAPITULOS_ASC`, `TITULO_ASC`, `TITULO_DESC`, `RECENTES`, `ANTIGAS`.
    - `pagina` (inteiro >= 0, padrão 0): Índice da página requisitada.
    - `itensPorPagina` (inteiro de 1 a 50, padrão 20): Quantidade de itens por página.
  - **Resposta Sucesso (200 OK):**
    Objeto `Page<ObraResponse>` padronizado do Spring Data (`content`, `totalElements`, `totalPages`, `size`, `number`, etc.).
- **`GET /obras/{id}`** (ou `/api/obras/{id}`)
  - **Acesso:** Público
  - **Finalidade:** Detalhes de uma obra (sinopse, gêneros, tipo, contagem de capítulos).
- **`POST /obras`**
  - **Acesso:** Autenticado
  - **Finalidade:** Cadastrar uma nova obra no acervo comunitário.

### 4.3 Minha Estante (Acompanhamento Pessoal de Leitura)
- **`GET /api/catalogo/minha-estante`**
  - **Acesso:** Autenticado
  - **Finalidade:** Retorna todas as obras registradas pelo usuário conectado com status de leitura:
    - `LENDO` (Em andamento)
    - `LIDO` (Concluído)
    - `QUERO_LER` (Planejado)
    - `ENGAVETADO` (Interrompido/Pausado temporariamente ou permanentemente)
- **`POST /api/catalogo`**
  - **Acesso:** Autenticado
  - **Payload:**
    ```json
    {
      "obraId": 1,
      "statusLeitura": "LENDO",
      "capituloAtual": 12,
      "volumeAtual": 1,
      "notaPessoal": 9.5,
      "linkLeituraPrivado": "https://meu-leitor-favorito.com/obra/1"
    }
    ```
- **`POST /api/catalogo/{id}/progresso`**
  - **Acesso:** Autenticado
  - **Payload:**
    ```json
    {
      "capituloAtual": 13,
      "volumeAtual": 1
    }
    ```
- **`DELETE /api/catalogo/{id}`**
  - **Acesso:** Autenticado
  - **Finalidade:** Remove uma obra da estante pessoal.

### 4.4 Comunidade e Conexões Sociais
- **`GET /api/conexoes/seguidores`**
  - **Acesso:** Autenticado
  - **Finalidade:** Lista os leitores que seguem o usuário logado.
- **`GET /api/conexoes/seguindo`**
  - **Acesso:** Autenticado
  - **Finalidade:** Lista os leitores que o usuário logado acompanha.
- **`POST /api/conexoes/{usuarioIdAlvo}/seguir`**
  - **Acesso:** Autenticado
  - **Finalidade:** Alterna a relação de seguir (follow/unfollow).

### 4.5 Feed Social
- **`GET /api/feed`**
  - **Acesso:** Autenticado
  - **Finalidade:** Timeline agregada com atualizações recentes de progresso e avaliações feitas pelos amigos seguidos.

### 4.6 Perfil do Usuário e Privacidade LGPD
- **`GET /api/usuarios/me`**
  - **Acesso:** Autenticado
  - **Finalidade:** Retorna dados completos da conta logada.
- **`GET /api/usuarios/{id}/perfil`**
  - **Acesso:** Público
  - **Finalidade:** Retorna o perfil público (estatísticas e estante aberta) de qualquer leitor.
- **`POST /api/usuarios/avatar`**
  - **Acesso:** Autenticado (Form-Data: `multipart/form-data; key=file`)
  - **Finalidade:** Realiza o upload seguro de imagem de avatar para o bucket S3/R2.
- **`DELETE /api/usuarios/conta`**
  - **Acesso:** Autenticado
  - **Finalidade:** Exclusão definitiva e anonimização total dos dados conforme LGPD/GDPR.

### 4.7 Avisos e Manutenção do Sistema
- **`GET /api/broadcasts/ativos`**
  - **Acesso:** Público
  - **Finalidade:** Retorna avisos globais ativos, comunicados de atualização ou notas operacionais.

---

## 5. Exemplo de Consumo com TypeScript / Fetch

```typescript
const API_URL = "http://localhost:8080";

// 1. Obter catálogo público
async function carregarCatalogo() {
  const response = await fetch(`${API_URL}/obras`);
  if (!response.ok) {
    const error = await response.json();
    console.error("Falha ao carregar catálogo:", error.title, error.detail);
    return;
  }
  const obras = await response.json();
  console.log("Obras carregadas:", obras);
}

// 2. Requisição autenticada
async function carregarMinhaEstante(token: string) {
  const response = await fetch(`${API_URL}/api/catalogo/minha-estante`, {
    headers: {
      Authorization: `Bearer ${token}`,
      Accept: "application/json",
    },
  });

  if (response.status === 401) {
    throw new Error("Sessão expirada. Faça login novamente.");
  }

  return await response.json();
}
```

---

## 6. Contribuição e Suporte

A Shiori API é mantida de forma colaborativa. Para propor melhorias, novos endpoints ou reportar bugs, abra uma Issue ou Pull Request no repositório oficial:
👉 **[shiori-api no GitHub](https://github.com/wiltongabriel/shiori-api)**
