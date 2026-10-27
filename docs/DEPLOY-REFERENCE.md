# Guia Operacional — Instância de Referência do Mantenedor (Shiori API)

Este documento registra os procedimentos operacionais específicos utilizados para a manutenção da **instância de referência oficial** do Shiori API operada pelo mantenedor. 

> [!NOTE]
> Se você está buscando como rodar o Shiori em seu próprio ambiente (self-hosting) ou como contribuir com código, consulte o [README principal da API](file:///home/wiltongx/Projetos/Projeto-Shiori/shiori-api/README.md) e o [Guia de Arquitetura](file:///home/wiltongx/Projetos/Projeto-Shiori/shiori-api/docs/ARQUITETURA.md).

---

## 1. Topologia da Instância de Referência

A instância de referência mantida pela equipe do projeto utiliza a seguinte pilha de suporte:

1. **Orquestração e Containers:** Docker Engine gerenciado via **Portainer Community Edition**.
2. **Proxy Reverso e SSL:** Traefik / Nginx com terminação TLS automática (Let's Encrypt).
3. **Rede de Borda e Cache:** Cloudflare CDN para entrega de assets estáticos e proteção de endpoints.
4. **Armazenamento de Mídias:** Cloudflare R2 / MinIO compatível com S3 para capas e avatares.

---

## 2. Esteira GitOps e Atualizações

1. **Repositório como Fonte Canônica:** Todo o deploy de produção é acionado a partir de tags ou merges validados na branch `main`.
2. **GitHub Actions:**
   - Compilação dos testes unitários e de integração (`./mvnw test`).
   - Construção e publicação de imagens Docker multi-arquitetura (`linux/amd64`, `linux/arm64`) no GitHub Container Registry (`ghcr.io/wiltongabriel/shiori-api`).
3. **Webhook do Portainer:** O Portainer recebe o webhook do GitHub Actions e recarrega os containers da stack sem downtime perceptual.

---

## 3. Diretrizes de Acesso SSH e Governança

> [!CAUTION]
> **POLÍTICA DE ACESSO OPERACIONAL RESTRITO:**
> - O acesso SSH ao servidor de produção (`usuario@host-referencia`) destina-se **exclusivamente a auditorias de sistema, rotação de certificados, inspeção de recursos do host e diagnóstico emergencial de containers**.
> - **Nenhuma alteração de código ou "hotfix" é executada diretamente no servidor de produção.**
> - Toda e qualquer alteração deve ser rastreada via commit, submetida a Pull Request, aprovada na suíte de testes e liberada via esteira de CI/CD.
