# Rizer Marketplaces

> Plataforma **SaaS B2B white-label multi-tenant** de anúncios de veículos (similar a iCarros / Webmotors).
> Empresas parceiras (garagistas, concessionárias, redes) cadastram suas lojas, publicam anúncios
> e ganham um site dedicado em `slug.motorise.com.br` ou em domínio próprio.

**Stack:** Spring Boot 4 + Java 25 (backend) · Vue 3 + Quasar 2 SSR/PWA (frontend) · PostgreSQL/PostGIS · Redis · Magalu S3 · Stripe · Meta Graph API · Google Shopping · Cloudflare DNS.

**Status (jun/2026):** MVP completo — 9 fases concluídas (ver `docs/PLAN-saas-b2b.md`).
25 migrations, ~50 entidades JPA, 21 controllers REST, 7 services de domínio, 8 jobs agendados, 5 filtros, 83 testes JUnit passando.

---

## Índice

1. [Visão geral](#1-visão-geral)
2. [Subir dependências locais](#2-subir-dependências-locais)
3. [Backend](#3-backend)
4. [Frontend](#4-frontend)
5. [Configuração e variáveis de ambiente](#5-configuração-e-variáveis-de-ambiente)
6. [Funcionalidades implementadas](#6-funcionalidades-implementadas)
7. [Integrações externas — chaves e configuração](#7-integrações-externas--chaves-e-configuração)
8. [LGPD / Privacidade](#8-lgpd--privacidade)
9. [Troubleshooting](#9-troubleshooting)
10. [Comandos úteis](#10-comandos-úteis)
11. [Docker & Deploy](#11-docker--deploy)
12. [Plano macro](#12-plano-macro)
13. [Contato](#13-contato)

---

## 1) Visão geral

### 1.1 Hierarquia do domínio

```
Country (region context: BR, US, …)
  └── Tenant (empresa/rede — slug.motorise.com.br, domínio custom opcional)
        ├── PhysicalStore 1..N  (filiais com endereço e geolocalização)
        │     └── Product 1..N  (anúncio sempre vinculado a uma loja)
        └── TenantUser 1..N     (OWNER | MANAGER | SELLER)

User              (plataforma: sys_admin | sys_manager | agency_owner | agency_admin | agency_employee | user)
Plan + Subscription + Payment  (billing; plano é por tenant, não por loja)
TenantIntegration (Instagram, META Business, Google Merchant — tokens encriptados)
Consent, DataExportRequest, AuditEntry  (LGPD + auditoria)
```

### 1.2 Planos e billing

| Plano    | Preço/mês | Lojas | Trial | Parceiro | Domínio custom | Instagram | META DPA | Google Shopping |
|----------|-----------|-------|-------|----------|----------------|-----------|----------|------------------|
| `BASIC`   | R$ 99     | 1     | 7d    | ✗        | ✗              | ✗         | ✗        | ✗                |
| `PRO`     | R$ 249    | 3     | 14d   | ✓        | ✓              | ✓         | ✗        | ✗                |
| `PLATINUM`| R$ 599    | ∞     | 14d   | ✓        | ✓              | ✓         | ✓        | ✓                |

- **Billing:** Stripe Checkout + webhook + Customer Portal (em dev, mock ativável em `/billing/webhooks/simulate`).
- **Pagamentos manuais:** admin/owner lança cash, transferência, Pix externo, bônus, cortesia — todos persistidos em `payments` (livro-caixa).
- **Multi-currency-ready:** `plans.currency` e `payments.currency` são `CHAR(3)`. BRL é o único seed nesta fase.
- **Multi-tenant:** tenant ativo vem de JWT claim > `X-Tenant-Slug` > path `/{cc}/public/tenants/{slug}` > Host.

---

## 2) Subir dependências locais

Requisitos: Docker + Docker Compose.

```bash
docker compose up -d
```

Sobe:
- **Postgres 17 + PostGIS** (`postgis/postgis:17-3.5`) — porta `5432`, banco `rizer_marketplaces`, user `rizer`/`rizer`.
- **Redis Stack** (`redis/redis-stack:7.4.0-v8`) — porta `6379`.
- **Redis Insight** (`redis/redisinsight:2.54`) — porta `5540` (UI de inspeção).

Os scripts SQL de inicialização ficam em `docker/postgres/init/` (habilitam `postgis`, `postgis_topology`, `ltree`).

Para parar / limpar volume:
```bash
docker compose down        # para containers
docker compose down -v     # remove também o volume de dados
```

---

## 3) Backend

### 3.1 Stack
- Spring Boot 4.0.5, Java 25, Spring Security + OAuth2 Client/Resource Server
- JPA/Hibernate + Hibernate Spatial (PostGIS), Flyway (migrations)
- jjwt 0.12.6 (HS256), Stripe SDK (TODO em prod), SpringDoc OpenAPI 3.0.2
- AWS SDK S3 2.33.3 (Magalu Cloud) — `S3StorageService`
- AES-GCM 256 (criptografia de tokens), JNDI DNS (resolução CNAME)
- Logback + LogstashEncoder (JSON structured logs)

### 3.2 Subir
```bash
cd backend
cp .env.example .env       # preencher credenciais (ver §5)
./mvnw spring-boot:run
```

Roda em `http://localhost:8080`. As 17 migrations do Flyway aplicam automaticamente na primeira execução.

### 3.3 Comandos úteis
```bash
./mvnw spring-boot:run                          # sobe o app
./mvnw test                                     # roda os 83 testes JUnit
./mvnw -DskipTests compile                      # só compila

# Flyway direto
./mvnw -Dflyway.url=jdbc:postgresql://localhost:5432/rizer_marketplaces \
      -Dflyway.user=rizer -Dflyway.password=rizer \
      flyway:info
./mvnw -Dflyway.url=jdbc:postgresql://localhost:5432/rizer_marketplaces \
      -Dflyway.user=rizer -Dflyway.password=rizer \
      -Dflyway.baselineOnMigrate=true -Dflyway.baselineVersion=0 \
      flyway:migrate
```

### 3.4 Endpoints
- **Documentação interativa (Scalar):** `http://localhost:8080/docs`
- **OpenAPI JSON:** `http://localhost:8080/openapi`
- **Health:** `/health`, `/health/live`, `/health/ready`
- **Auth:** `/auth/login`, `/auth/login/refresh`, `/auth/logout`, `/auth/me`, `/auth/switch-tenant`
- **OAuth Google:** `/oauth2/authorization/google`, `/login/oauth2/code/google`
- **Admin (sys_admin):** `/admin/tenants/**`, `/admin/billing/**`
- **Tenant (autenticado, com `currentTenantId`):** `/tenant/stores/**`, `/tenant/members/**`, `/tenant/products/**`, `/tenant/integrations/**`, `/tenant/billing/**`, `/tenant/settings/**`, `/tenant/billing/trial/{planCode}`
- **Público regional (`/{countryCode}/...`):** `/BR/public/categories`, `/BR/public/brands`, `/BR/public/products`, `/BR/public/tenants/partner`, `/BR/public/tenants/{slug}`, `/BR/public/tenants/{slug}/products`, `/BR/public/tenants/{slug}/feed.xml`, `/BR/public/tenants/{slug}/feed-meta.csv`, `/BR/public/billing/plans`
- **Me (LGPD):** `/me/consents`, `/me/data-export`, `/me/account`
- **Webhooks:** `/billing/webhooks/stripe`, `/billing/webhooks/simulate` (dev)

### 3.5 Migrations aplicadas (V1-V17)

| # | Conteúdo |
|---|---|
| V1 | Extensões Postgres: `postgis`, `postgis_topology`, `ltree` |
| V2-V4 | Marcas e modelos FIPE (Carro, Moto, Caminhão, Náutico, Ônibus) |
| V5 | `countries` (region context, substitui `store` ambíguo do `esquema.tabelas.md`) |
| V6 | `users` (uuid, email único, password_hash, system_role, soft delete) + `addresses` (geography) |
| V7 | `tenants` (theme JSONB, custom_domain, had_trial) + `tenant_users` (OWNER/MANAGER/SELLER, physical_store_ids) |
| V8 | `physical_stores` (geolocalização, is_main, opening_hours JSONB) |
| V9 | `categories` ltree (até 3 níveis, 5 reinos) |
| V10 | `attribute_schemas` (JSON Schema por país + categoria) |
| V11 | `products` + `product_localizations` (price_cents BIGINT, location opcional) + `product_images` |
| V12 | `plans` + `subscriptions` (status enum, source enum, grace_period_days) |
| V13 | `payments` (livro-caixa) + `stripe_invoices` |
| V14 | `tenant_integrations` (tokens encriptados para Instagram/META/Google) |
| V15 | `custom_domain_checks` (log de auditoria das verificações CNAME) |
| V16 | `consents` (LGPD) + `data_export_requests` (JSON + S3 presigned 7d) |
| V17 | `audit_log` (ação, recurso, severidade, payload, correlation_id) |
| V18 | `leads` (captura de leads via formulário público) |
| V19 | `lead_sources` (origem do lead: site, instagram, facebook) |
| V20 | `catalog_visibility` (controle de visibilidade de produtos por tenant) |
| V21 | `product_bookmarks` (favoritos do usuário) |
| V22 | `partner_widgets` (widgets customizáveis para parceiros) |

### 3.6 Layout de pacotes (17 packages)
```
br.com.rizermarketplaces.core.marketplace
├── config/         Security, OpenAPI, Aws, ApiErrorAdvice, CorrelationIdFilter, RateLimitFilter
├── context/        CountryContextHolder, TenantContextHolder + filters
├── auth/           AuthService, JwtTokenProvider, JwtAuthenticationFilter, OAuth2SuccessHandler, CurrentUser
├── controller/     públicos (auth, billing/webhooks, lgpd, health, media)
├── controller/admin/   AdminTenantController, AdminBillingController
├── controller/tenant/  StoreController, ProductController, BillingController, SettingsController, IntegrationController, MemberController, TrialController
├── controller/publicapi/ PartnerController, CatalogController, PlansController, FeedsController
├── admin/          AdminTenantService
├── tenant/         PhysicalStoreService, CustomDomainService, DnsLookupService, StoreLimitGuard, CloudflareService
├── catalog/        CatalogService (categorias, marcas, modelos)
├── product/        ProductService, ProductImageService
├── rules/          DynamicAttributeValidationService (JSON Schema validator)
├── billing/        PlanService, SubscriptionService, SubscriptionStateMachine, StripeService, BillingWebhookService, ManualPaymentService, TrialService, TrialExpirationJob
├── integration/    InstagramService, MetaCatalogService, GoogleShoppingFeedService, MetaGraphClient, EncryptionService, IntegrationAccessGuard, IntegrationSyncJob
├── lgpd/           ConsentService, DataExportService, DataRetentionJob
├── audit/          AuditService (audit log centralizado)
├── tools/          CnpjValidator, SlugGenerator, PhoneNormalizer
├── model/          entidades JPA
├── repository/     Spring Data
├── dto/            DTOs de entrada/saída
└── service/        S3StorageService
```

### 3.7 Testes
83 testes JUnit passando:
- `CnpjValidatorTest` (5) — algoritmo oficial Receita Federal
- `SlugGeneratorTest` (5) — normalização de acentos, colapso de hífens, truncamento
- `PhoneNormalizerTest` (3) — onlyDigits
- `SubscriptionStateMachineTest` (10) — guards por status, grace period, feature flags

---

## 4) Frontend

### 4.1 Stack
- Vue 3.5 + Quasar 2.16 (SPA/SSR/PWA)
- Vue Router 5, Vue I18n 11 (pt-BR único nesta fase)
- Axios 1.13 (instância `boot/axios.ts` injeta `X-Tenant-Slug` e `Authorization`)
- TypeScript 5.9 strict mode
- Composables manuais (sem Pinia nesta fase)

### 4.2 Subir
> **Requisito:** o backend deve estar rodando em `http://localhost:8080` (ver §3.2).  
> O frontend **não possui mais mocks** — todas as páginas consomem a API real.

```bash
cd frontend-motor
pnpm install           # ou: npm install
pnpm quasar dev        # ou: npm run dev          → http://localhost:9000
pnpm quasar dev -m ssr # modo SSR
```

### 4.3 Qualidade
```bash
npx vue-tsc --noEmit    # typecheck (0 erros)
npm run lint           # ESLint (0 erros)
npm run format         # Prettier write
```

### 4.4 Estrutura
- `src/boot/` — `i18n, axios, auth, animate, appReady, tenant` (axios injeta `X-Tenant-Slug` e faz retry 1x em 401)
- `src/stores/authStore.ts` — `useAuthStore` (singleton reativo: user, memberships, currentTenantId, login/logout/refresh)
- `src/composables/` — `useTenant` (multi-tenant), `useConsent` (LGPD), `useLoading` (overlay)
- `src/services/api.ts` — cliente HTTP real (`adminApi`, `tenantApi`, `billingApi`, `integrationApi`, `partnerApi`, `lgpdApi`, `settingsApi`, `catalogApi`, `tenantProductApi`, `leadApi`)
- `src/data/` — `tenants.ts` (registro estático), `types.ts` (Vehicle, Store, User, Lead, etc.), `legalVersions.ts`

### 4.5 Páginas (~40)
- **MainLayout (público):** `Home, Produtos, Produto, Comparar, LojasParceiras (/parceiros), LojaPage, SejaParceiro, Entrar, Registro, Favoritos, Leads, Anuncios`
- **Partner:** `PartnerListPage, PartnerPage, PartnerStorePage`
- **Legal:** `TermosDeUso, PoliticaPrivacidade, PoliticaCookies`
- **AppLayout (autenticado):** `Dashboard, Stores, Products, ProductWizard, ProductEdit, Leads, Members, Billing, Integrations, Settings`
- **AdminLayout (sys_admin):** `Dashboard, Tenants, Users, Plans, Payments`
- **Error:** `ErrorNotFound` (404)

---

## 5) Configuração e variáveis de ambiente

### 5.1 Backend — `backend/.env`

#### Banco de dados e Redis (obrigatório)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `SPRING_DATASOURCE_URL` | sim | — | JDBC URL (`jdbc:postgresql://localhost:5432/rizer_marketplaces`) |
| `SPRING_DATASOURCE_USERNAME` | sim | — | Usuário Postgres |
| `SPRING_DATASOURCE_PASSWORD` | sim | — | Senha Postgres |
| `SPRING_DATA_REDIS_HOST` | sim | — | Host do Redis |
| `SPRING_DATA_REDIS_PORT` | sim | — | Porta (6379) |
| `SPRING_DATA_REDIS_PASSWORD` | não | vazio | Senha do Redis (vazio se local) |
| `SERVER_PORT` | não | 8080 | Porta HTTP do backend |

#### JWT (obrigatório em prod)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_JWT_SECRET` | **sim** | — | Chave HS256. Gerar com `openssl rand -base64 48` (mín. 32 bytes) |
| `APP_JWT_ACCESS_TTL_MINUTES` | não | 60 | TTL do access token |
| `APP_JWT_REFRESH_TTL_DAYS` | não | 30 | TTL do refresh token |
| `APP_JWT_ISSUER` | não | `motorise` | Claim `iss` |
| `APP_OAUTH2_SUCCESS_REDIRECT` | não | `http://localhost:3000/auth/callback` | Para onde o Google OAuth redireciona após sucesso |

#### Google OAuth (opcional)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `GOOGLE_OAUTH_CLIENT_ID` | não | vazio | Console: `console.cloud.google.com/apis/credentials` |
| `GOOGLE_OAUTH_CLIENT_SECRET` | não | vazio | Authorized redirect URI: `APP_OAUTH2_SUCCESS_REDIRECT` |

#### Magalu Cloud S3 (obrigatório)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_S3_REGION` | sim | `br-se1` | |
| `APP_S3_ENDPOINT` | sim | `https://br-se1.magaluobjects.com` | |
| `APP_S3_PATH_STYLE` | não | `true` | |
| `APP_S3_PUBLIC_BUCKET` | sim | `rizer-pic` | Imagens de anúncios (URL direta) |
| `APP_S3_PUBLIC_KEY_PREFIX` | não | `uploads` | |
| `APP_S3_PRIVATE_BUCKET` | sim | `rizer-storage` | Documentos, comprovantes, exports (presigned) |
| `APP_S3_PRIVATE_KEY_PREFIX` | não | `docs` | |
| `APP_S3_PRESIGNED_DURATION_MINUTES` | não | 15 | |
| `AWS_ACCESS_KEY_ID` | sim | — | Credenciais Magalu (use IAM role em prod) |
| `AWS_SECRET_ACCESS_KEY` | sim | — | |
| `AWS_SESSION_TOKEN` | não | — | Para credenciais temporárias |

#### Criptografia de tokens (obrigatório em prod)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_ENCRYPTION_KEY` | **sim** | — | Base64 de 32 bytes (AES-256). `openssl rand -base64 32` |

#### Plataforma

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_TENANT_PLATFORM_DOMAIN` | não | `motorise.com.br` | Domínio base para subdomínios dos tenants |

#### Rate limit

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_RATELIMIT_AUTH_CAPACITY` | não | 5 | requests/min para `/auth/**` por IP |
| `APP_RATELIMIT_ADMIN_CAPACITY` | não | 60 | requests/min para `/admin/**` por IP |
| `APP_RATELIMIT_GLOBAL_CAPACITY` | não | 300 | requests/min para o resto por IP |

#### LGPD / Retenção

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_LGPD_RETENTION_JOB_ENABLED` | não | `true` | Liga/desliga o job de expirar data-exports |
| `APP_LGPD_RETENTION_JOB_INTERVAL_MS` | não | 86400000 | Periodicidade (24h) |

#### Stripe (desabilitado em dev)

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_STRIPE_ENABLED` | não | `false` | Quando `false`, StripeService gera URLs mock para dev |
| `APP_STRIPE_API_KEY` | em prod | — | `sk_live_...` ou `sk_test_...` |
| `APP_STRIPE_WEBHOOK_SECRET` | em prod | — | `whsec_...` (configurar endpoint `/billing/webhooks/stripe` no dashboard) |
| `APP_STRIPE_SUCCESS_URL` | não | `http://localhost:3000/app/assinatura/checkout` | |
| `APP_STRIPE_CANCEL_URL` | não | `http://localhost:3000/app/assinatura` | |

#### Meta (Facebook + Instagram) — Platinum

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_META_APP_ID` | em prod | vazio | `developers.facebook.com/apps` → Business |
| `APP_META_APP_SECRET` | em prod | vazio | |
| `APP_META_REDIRECT_URI` | em prod | `http://localhost:3000/app/integracoes/instagram/callback` | Domínio deve estar no App |

> **Importante:** scopes de Instagram (`instagram_basic`, `instagram_content_publish`) exigem **App Review** da Meta antes de produção.

#### Cloudflare (custom domain) — TODO em prod

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `APP_CLOUDFLARE_ENABLED` | não | `false` | Liga o provisionamento de DNS via API |
| `APP_CLOUDFLARE_API_TOKEN` | em prod | — | Token com permissão `Zone:DNS:Edit` |
| `APP_CLOUDFLARE_ZONE_ID` | em prod | — | ID da zona `motorise.com.br` |

> SSL é emitido via **Cloudflare Origin CA** quando o tenant está com `custom_domain_status=VERIFIED`. `// TODO(fase-7-prod)` no `CloudflareService`.

#### Google Merchant Center (Platinum, opcional)

Atualmente o `GoogleShoppingFeedService` gera feed XML estático. Para usar Content API:
- Service account no Google Cloud, scope `https://www.googleapis.com/auth/content`
- API key ou OAuth 2.0 — `// TODO(fase-6-prod)` no `GoogleShoppingFeedService`

### 5.2 Frontend — `frontend-motor/.env`

| Variável | Obrigatório | Default | Descrição |
|----------|-------------|---------|-----------|
| `VITE_API_URL` | **sim** | `/api` | URL base da API. Em dev local: `http://localhost:8080` |
| `VITE_BASE_DOMAIN` | não | `motorise.com.br` | Domínio base para multi-tenant por subdomínio |

> ⚠️ O frontend **não possui mais mocks**. O backend deve estar rodando e o `VITE_API_URL` deve apontar para ele (ex: `http://localhost:8080`).

---

## 6) Funcionalidades implementadas

### 6.1 Autenticação
- **JWT HS256** com chave em `APP_JWT_SECRET` (mín. 32 bytes). Tokens em cookies HttpOnly `motorise_access` (60min) e `motorise_refresh` (30d).
- **Login email/senha** (`POST /auth/login`) e **Google OAuth** (`/oauth2/authorization/google`).
- **`/auth/me`** retorna user + memberships com `tenantSlug` e `tenantName` para o frontend construir URLs de feed e de navegação.
- **`/auth/switch-tenant`** alterna o tenant ativo e devolve novo access token com claim `tenantId`.
- **RBAC duplo:** papel na plataforma (`users.system_role`: `sys_admin`, `sys_manager`, `agency_owner`, `agency_admin`, `agency_employee`, `user`) + papel dentro do tenant (`tenant_users.role`: `OWNER`, `MANAGER`, `SELLER`).
- **CORS** habilitado para `http://localhost:*` e `*.motorise.com.br` (em `SecurityConfig.corsConfigurationSource`).

### 6.2 Multi-tenant
- **Resolução** do tenant ativo: JWT claim `tenantId` > header `X-Tenant-Slug` > path `/{cc}/public/tenants/{slug}` > Host (subdomínio/CNAME).
- **`TenantContextFilter`** popula `TenantContextHolder` (ThreadLocal) e **`CountryContextFilter`** popula `CountryContextHolder`.
- **Subdomínios:** `slug.motorise.com.br` apontando para o LB; `tenant.theme` injetado como CSS variables no DOM pelo frontend (`useTenant.applyTheme`).
- **Custom domain:** tenant configura `meucliente.com.br`, aponta CNAME → `slug.motorise.com.br`; `CustomDomainService.verify` consulta DNS via JNDI, grava `VERIFIED/FAILED` em `custom_domain_checks` e atualiza `tenants.custom_domain_status`. SSL via Cloudflare Origin CA (placeholder `// TODO(fase-7-prod)` no `CloudflareService`).

### 6.3 Lojas físicas
- Tenant tem 1..N `PhysicalStore` (filiais), cada uma com endereço, telefone, WhatsApp, e-mail, `opening_hours` JSONB, `is_main` e geolocalização PostGIS.
- **Limite por plano** enforçado em `StoreLimitGuard.assertCanCreate` (BASIC=1, PRO=3, Platinum=ilimitado).
- Apenas **1 loja principal** por tenant (unique index `uk_physical_stores_main`).
- Anúncio sempre vinculado a 1 loja (`products.physical_store_id NOT NULL`).

### 6.4 Anúncios
- **5 reinos:** Carro, Moto, Caminhão, Náutico, Ônibus — via seeds de `categories` (ltree).
- **Atributos dinâmicos JSON Schema** (`attribute_schemas`): validados por `(country_code, category_path)` via `DynamicAttributeValidationService` (subset draft-07: `required`, `type`, `enum`, `minimum/maximum`, `minLength/maxLength`, `pattern`, `items`, `maxItems`).
- **Wizard de criação** (5 passos): tipo → dados (marca, modelo, ano, km, combustível, câmbio) → descrição (título, preço) → fotos (upload multipart com auto-cover) → loja + publicar.
- **Localização do anúncio:** herda da loja (default) ou customizada (`location_source: STORE|CUSTOM`).
- **Geometria PostGIS:** `product_localizations.location geography(Point,4326)` para busca por raio.
- **Imagens:** S3 público, cover automática na primeira foto (unique index `uk_product_images_cover`).

### 6.5 Páginas públicas
- **`/parceiros`** (`PartnerListPage`): grid de cards com banner/logo, cidades, contador de lojas e produtos. Filtros: busca textual, cidade.
- **`/parceiros/:slug`** (`PartnerPage`): hero com banner + logo + WhatsApp, seção "Sobre", cards de lojas, grid de produtos com cover/preço/km/combustível. Injeta tema do tenant no DOM.
- **`/parceiros/:slug/loja/:storeSlug`** (`PartnerStorePage`): mesma UI focada em uma filial, com produtos filtrados.
- **`/parceiros` é o destino de `/lojas-parceiras`** (redirect para SEO).

### 6.6 Billing
- **Planos seed:** BASIC R$99, PRO R$249, Platinum R$599 (BRL), trials 7/14/14 dias.
- **Stripe Checkout** + **webhook** + **Customer Portal** (dev: mock em `/billing/webhooks/simulate`).
- **Pagamentos manuais** via `ManualPaymentService` — admin ou owner lança `cash`, `bank_transfer`, `pix_external`, `bonus`, `courtesy`, `other` em `POST /tenant/billing/payments` (owner) ou `POST /admin/billing/tenants/{id}/payments` (admin).
- **Trial** por tenant, uma única vez (`tenants.had_trial`). `TrialExpirationJob` (1×/hora) cancela trials vencidos.
- **Endpoints principais:**
  - `GET /tenant/billing/subscription` — plano atual + dias restantes
  - `POST /tenant/billing/checkout/{planCode}` — URL do Stripe
  - `POST /tenant/billing/cancel` / `/resume` — `cancel_at_period_end`
  - `GET /admin/billing/payments` — livro-caixa global paginado
  - `GET /admin/billing/stats` — MRR, ativos, trial, past_due, receita 30d
- **`SubscriptionStateMachine`** centraliza enforcements: `canCreateStore`, `canPublishAds`, `isActiveLike`, `isInGracePeriod`, `assertFeatureEnabled(plan, feature)`.

### 6.7 Integrações de marketing
- **Instagram** (`InstagramService`): OAuth Meta → descobre `instagram_business_account` da página → tokens encriptados com `EncryptionService` (AES-GCM 256) → `publishProduct` cria container + publica; `IntegrationSyncJob` (5 min) posta até 3 produtos novos por tick.
- **META Business / Dynamic Ads** (`MetaCatalogService`): Platinum only. Sincroniza produtos ACTIVE com Meta Commerce Manager via Graph API.
- **Google Shopping** (`GoogleShoppingFeedService`): Platinum only. Feed XML (formato GMC RSS 2.0 com namespace `g:`) gerado on-the-fly em `GET /{cc}/public/tenants/{slug}/feed.xml`. Atualização automática via Content API é `TODO(fase-6-prod)`.
- **Tokens encriptados** em repouso (`tenant_integrations.access_token_encrypted`) com AES-GCM 256 (`APP_ENCRYPTION_KEY`).
- **Endpoints:**
  - `GET /tenant/integrations/{provider}/authorize` — URL OAuth
  - `POST /tenant/integrations/{provider}/callback` — troca code → token
  - `DELETE /tenant/integrations/{provider}` — desconectar
  - `POST /tenant/integrations/instagram/publish/{productId}` — postar manualmente

### 6.8 LGPD
- **`consents` (V16)** com `document_version`: quando a versão dos termos muda, frontend pede re-consentimento.
- **Banner de consentimento** (`ConsentBanner.vue` + `useConsent`): persistente em cookie `motorise_consent` (formato JSON `{purpose: {v, version}}`).
- **`/me/data-export`** (LGPD art. 18 V): `DataExportService` gera JSON com todos os dados do user, sobe para S3 bucket privado, gera presigned URL válida por 7 dias.
- **`/me/account`** (DELETE): anonimiza PII (email → `anon+{uuid}@deleted.local`, nome → `[Conta excluída]`, apaga senha/avatar) + soft-delete.
- **Retenção:** `DataRetentionJob` (diário) expira exports e remove arquivos do S3. Audit log retido 5 anos.
- **DPO:** `dpo@riser.com` (configurável por tenant).
- **Política de privacidade** com botões de export/delete para usuários logados.

### 6.9 Polish
- **`audit_log` (V17):** toda ação sensível gravada com `actor_user_id`, `tenant_id`, `action`, `resource_type`, `resource_id`, `severity` (INFO/WARN/ERROR), `payload` JSONB, `ip`, `user_agent`, `correlation_id`. Retenção 5 anos.
- **Rate limit** in-memory por IP+rota: 5/min em `/auth/**`, 60/min em `/admin/**`, 300/min no resto. Responde 429 com `Retry-After: 60` e JSON `{code: "rate_limited"}`. Bypassa docs, openapi, health, feeds públicos.
- **`CorrelationIdFilter`:** gera/propaga `X-Correlation-Id` no request/response e no MDC do SLF4J (aparece nos logs como `[cid=...]`).
- **`/health`, `/health/live`, `/health/ready`** (sem auth).
- **Logback JSON** rolling file (`./logs/app.json`) com `LogstashEncoder` — pronto para ELK/Datadog/Loki. Rotação 50MB, 14 dias, cap 1GB.
- **`ApiErrorAdvice`** (`@RestControllerAdvice`) converte exceções em `ProblemDetail` padronizado. `ValidationException` retorna 400 com `fields` por campo.
- **83 testes JUnit** passando (`./mvnw test`).

---

## 7) Integrações externas — chaves e configuração

### 7.1 Banco de dados (Postgres + PostGIS)
- **Dev:** Docker compose já sobe `postgis/postgis:17-3.5`.
- **Prod:** RDS, Aurora ou similar. Versão mínima: Postgres 15 com PostGIS 3.x.
- O Flyway é a fonte de verdade do schema (`spring.jpa.hibernate.ddl-auto: validate`).

### 7.2 Redis
- **Dev:** Docker compose já sobe `redis/redis-stack:7.4.0-v8`.
- **Prod:** ElastiCache, MemoryStore ou similar. Usado para: rate limit (futuro), cache, sessão.

### 7.3 Magalu Cloud S3
1. Criar conta em https://magalu.cloud/
2. No painel, criar **2 buckets** na região `br-se1`:
   - `rizer-pic` (público, para imagens de anúncios)
   - `rizer-storage` (privado, para documentos/comprovantes/exports)
3. Em **Credenciais**, criar Access Key + Secret (ou usar IAM Role para produção).
4. Configurar as env vars `APP_S3_*` + `AWS_*` (ver §5.1).

### 7.4 JWT
Gerar a chave de assinatura:
```bash
openssl rand -base64 48
```
Resultado (48 bytes em base64 = 64 chars) → colar em `APP_JWT_SECRET`. Mínimo 32 bytes, recomendado 48.

### 7.5 Google OAuth
1. Google Cloud Console → `console.cloud.google.com/apis/credentials`.
2. Criar credencial do tipo **OAuth 2.0 Client ID** (Web application).
3. Authorized JavaScript origins: `http://localhost:9000` (dev), `https://app.motorise.com.br` (prod).
4. Authorized redirect URIs: `http://localhost:3000/auth/callback` (= `APP_OAUTH2_SUCCESS_REDIRECT`).
5. Copiar Client ID e Client Secret para `GOOGLE_OAUTH_CLIENT_ID` e `GOOGLE_OAUTH_CLIENT_SECRET`.

### 7.6 Stripe
> Em dev, o `StripeService` opera em **modo mock** (`APP_STRIPE_ENABLED=false`), gerando URLs `/billing/webhooks/simulate` para testar o fluxo sem chave.

Para ativar em produção:
1. Criar conta em https://dashboard.stripe.com
2. Ativar Stripe Billing.
3. Criar **3 produtos** com **3 preços mensais recorrentes**:
   - `BASIC` → R$ 99/mês
   - `PRO` → R$ 249/mês
   - `PLATINUM` → R$ 599/mês
4. Para cada preço, copiar o `price_id` (ex: `price_1ABC...`) e atualizar `plans.stripe_price_id` via SQL:
   ```sql
   UPDATE plans SET stripe_price_id = 'price_xxx' WHERE code = 'PRO';
   ```
5. Configurar webhook em **Developers → Webhooks → Add endpoint**:
   - URL: `https://api.motorise.com.br/billing/webhooks/stripe`
   - Events: `checkout.session.completed`, `invoice.paid`, `invoice.payment_failed`, `customer.subscription.created`, `customer.subscription.updated`, `customer.subscription.deleted`, `customer.subscription.trial_will_end`
6. Copiar o **Signing secret** (`whsec_...`) para `APP_STRIPE_WEBHOOK_SECRET`.
7. Setar `APP_STRIPE_ENABLED=true` e `APP_STRIPE_API_KEY=sk_live_...`.

### 7.7 Meta (Facebook + Instagram + META Business)
> Requer **App Review** da Meta antes de ir para produção com usuários reais.

1. Criar app em https://developers.facebook.com/apps
2. Tipo: **Business**.
3. Adicionar produto **Instagram Graph API** + **Marketing API** (para META DPA).
4. Configurações → Básico: copiar `APP_ID` e `APP_SECRET`.
5. **Facebook Login for Business → Settings → Valid OAuth Redirect URIs:** adicionar `APP_META_REDIRECT_URI` (default: `http://localhost:3000/app/integracoes/instagram/callback`).
6. **App Review → Permissions and Features**, solicitar:
   - `instagram_basic`
   - `instagram_content_publish` (essencial para postar)
   - `pages_show_list`
   - `pages_read_engagement`
   - `business_management`
   - `catalog_management` (para META DPA — apenas Platinum)
7. Em dev, o `app_id`/`app_secret` podem ficar vazios e o backend retorna 400 ao tentar OAuth (esperado até App Review).

### 7.8 Google Merchant Center
> Atualmente o feed é **estático via XML** gerado on-the-fly. Para auto-update via API:

1. Google Cloud Console → API Library → **Content API for Shopping** → Enable.
2. Criar Service Account, baixar JSON key.
3. Em Merchant Center → Settings → API access, vincular a service account.
4. (TODO) `GoogleShoppingFeedService` precisa de:
   - `GOOGLE_CONTENT_API_KEY_PATH` (path pro JSON key)
   - `GOOGLE_CONTENT_MERCHANT_ID`
   - `GOOGLE_CONTENT_REFRESH_TOKEN` (via OAuth)
5. Atualizar feed via `accounts/{merchantId}/products` (Content API for Shopping).

### 7.9 Cloudflare
> Atualmente o `CloudflareService` é stub (modo dev apenas loga). Para ativar em produção:

1. Domínio `motorise.com.br` registrado na Cloudflare (DNS autoritativo).
2. **My Profile → API Tokens → Create Token** com permissão `Zone:DNS:Edit` (escopo: zona `motorise.com.br`).
3. Copiar token para `APP_CLOUDFLARE_API_TOKEN`.
4. Em **Overview**, copiar o **Zone ID** da zona `motorise.com.br` para `APP_CLOUDFLARE_ZONE_ID`.
5. `APP_CLOUDFLARE_ENABLED=true`.
6. **SSL:** a emissão automática do cert é `// TODO(fase-7-prod)`. Recomendação: **Cloudflare Origin CA** (cert de 15 anos, gerado pelo dashboard) instalado no load balancer. Alternativa: **Caddy on-demand TLS**.

---

## 8) LGPD / Privacidade

| Tópico | Implementação |
|--------|----------------|
| **Base legal** | Execução de contrato, cumprimento de obrigação legal, legítimo interesse, consentimento (cookies opcionais, marketing) |
| **Finalidades** | Operação da plataforma, cumprimento legal/fiscal, prevenção de fraude, comunicações operacionais e marketing (mediante consentimento) |
| **Compartilhamento** | Stripe (pagamentos), Magalu Cloud S3 (storage), Meta/Google (marketing, mediante consentimento) |
| **Bases legais dos dados** | Contrato (anúncios), legal (fiscal — 5 anos), legítimo interesse (segurança), consentimento (marketing) |
| **Direitos do titular (art. 18)** | Acesso, correção, anonimização, portabilidade, exclusão |
| **Export de dados (V)** | `POST /me/data-export` — JSON com tudo, link S3 presigned 7 dias |
| **Exclusão de conta** | `DELETE /me/account` — anonimiza PII + soft-delete |
| **Retenção** | Audit log: 5 anos · Payments: 5 anos (fiscal) · Exports: 7 dias · Leads: 2 anos (TODO) |
| **DPO** | `dpo@riser.com` |

---

## 9) Troubleshooting

| Sintoma | Causa | Solução | Onde olhar |
|---------|-------|---------|------------|
| Backend sobe mas Flyway falha com `Migration Vx is not synced` | Migration local divergente do estado do banco | Limpar volume: `docker compose down -v && docker compose up -d`, depois subir o backend | `docker compose logs postgres` |
| `Invalid value: APP_JWT_SECRET must be at least 32 bytes` | `APP_JWT_SECRET` ausente ou curto | `openssl rand -base64 48` → colar no `.env` | `backend/.env` |
| `Communications link failure` no startup | Postgres ainda não está healthy | Esperar o healthcheck ficar `healthy` (ver `docker compose ps`) | `docker compose logs postgres` |
| 401 no login do frontend | CORS bloqueando o cookie cross-origin | Verificar `SecurityConfig.corsConfigurationSource` — `http://localhost:9000` deve estar em `allowedOriginPatterns` | `backend/src/main/java/.../config/SecurityConfig.java` |
| `Tenants.custom_domain_status` fica em `FAILED` | CNAME não aponta para `slug.motorise.com.br`, ou DNS ainda não propagou | Conferir com `dig CNAME meucliente.com.br` no host. Aguardar TTL. | `/app/configuracoes` aba Domínio |
| Posts no Instagram falham com "Invalid OAuth access token" | Token expirado ou revogado | Desconectar e reconectar via `/app/integracoes`. Verificar `APP_META_APP_ID/SECRET`. | `InstagramService.completeOAuth` |
| Frontend em branco ou 401 | Backend não está rodando ou `VITE_API_URL` aponta para lugar errado | Subir backend (`./mvnw spring-boot:run`) e verificar `VITE_API_URL` | `frontend-motor/.env`, logs do backend |
| `ddl-auto: validate` falhou — schema não bate | Entidade JPA diverge de migration | Verificar se migration existe; se sim, ajustar entidade; nunca `ddl-auto: update` em prod | Hibernate logs |
| `429 Too Many Requests` em testes locais | Rate limit bateu (5/min em auth) | Esperar 60s ou desabilitar temporariamente via env `APP_RATELIMIT_*_CAPACITY=999999` | `RateLimitFilter` |

---

## 10) Comandos úteis

### 10.1 Backend
```bash
cd backend
./mvnw spring-boot:run              # sobe o app
./mvnw test                          # roda 83 testes JUnit
./mvnw -DskipTests compile           # só compila
./mvnw spring-boot:build-image       # build OCI image
```

### 10.2 Frontend
```bash
cd frontend-motor
pnpm install                        # ou: npm install
pnpm quasar dev                     # SPA em http://localhost:9000
pnpm quasar dev -m ssr              # SSR
pnpm build                           # build de produção
pnpm format                          # prettier write
npm run lint                         # eslint
npx vue-tsc --noEmit                 # typecheck
```

### 10.3 Docker
```bash
docker compose up -d                 # sobe Postgres + Redis + Redis Insight
docker compose down                 # para
docker compose down -v              # para e remove volumes
docker compose logs -f postgres     # tail logs
docker compose ps                   # lista serviços e health
```

### 10.4 Banco
```bash
docker exec -it rizer_postgres psql -U riser -d rizer_marketplaces
# dentro do psql:
\dt                                # lista tabelas
\d tenants                         # schema de tenants
SELECT * FROM plans;                # planos
```

---

## 11) Docker & Deploy

### Container Registry
```
container-registry.br-se1.magalu.cloud/rizer/
├── backend    # Spring Boot API
└── motorise   # Quasar SSR/PWA
```

### Login
```bash
docker login container-registry.br-se1.magalu.cloud
```

### Build
```bash
REGISTRY=container-registry.br-se1.magalu.cloud/rizer
VERSION=1.0.0

docker build -t $REGISTRY/backend:latest  -t $REGISTRY/backend:$VERSION  ./backend
docker build -t $REGISTRY/motorise:latest -t $REGISTRY/motorise:$VERSION ./frontend-motor
```

### Push
```bash
REGISTRY=container-registry.br-se1.magalu.cloud/rizer
VERSION=1.0.1

docker push $REGISTRY/backend:latest  && docker push $REGISTRY/backend:$VERSION
docker push $REGISTRY/motorise:latest && docker push $REGISTRY/motorise:$VERSION
```

### Pull & run production
```bash
docker login container-registry.br-se1.magalu.cloud
docker pull container-registry.br-se1.magalu.cloud/rizer/backend:latest
docker pull container-registry.br-se1.magalu.cloud/rizer/motorise:latest
docker compose -f docker-compose.prod.yml up -d
```

O `docker-compose.prod.yml` já sobe:
- **Backend:** `http://localhost:8080`
- **Frontend:** `http://localhost:3000`
- **Postgres+PostGIS:** `localhost:5432`
- **Redis Stack:** `localhost:6379`

> Backend lê o `backend/.env` (use **Docker secrets** ou **AWS SSM Parameter Store** em prod — não comite o `.env`).

---

## 12) Plano macro

Detalhes completos em **`docs/PLAN-saas-b2b.md`**.

**Status (jun/2026):** ✅ 9 fases concluídas — MVP completo.

| Fase | Entregue |
|------|----------|
| 1 | Fundações: V5-V7 (countries, users+addresses, tenants+tenant_users), JWT real, OAuth Google, contexto multi-tenant, layouts e auth flow |
| 2 | Admin de tenants + lojas: V8 (physical_stores), AdminTenantService, StoreLimitGuard (PRO=3), TenantMemberService |
| 3 | Anúncios: V9-V11 (categories ltree, attribute_schemas JSON, products+localizations+images), DynamicAttributeValidationService, wizard 5 passos |
| 4 | Páginas públicas de empresa parceira: `/parceiros`, `/parceiros/:slug`, `/parceiros/:slug/loja/:storeSlug`, tema injetado no DOM |
| 5 | Billing: V12-V13 (plans+subscriptions, payments+stripe_invoices), StripeService (dev mock + webhook simulado), ManualPaymentService, TrialService, /app/assinatura, /admin/payments |
| 6 | Integrações: V14 (tenant_integrations com tokens AES-GCM), InstagramService (OAuth+post), MetaCatalogService, GoogleShoppingFeedService (XML), IntegrationSyncJob |
| 7 | Custom domain: V15 (custom_domain_checks), DnsLookupService, CustomDomainService, CloudflareService stub, /tenant/settings com q-tabs |
| 8 | LGPD: V16 (consents+data_export_requests), ConsentBanner, /me/data-export, /me/account, DataRetentionJob, política de privacidade |
| 9 | Polish: V17 (audit_log), AuditService, ApiErrorAdvice, CorrelationIdFilter, RateLimitFilter, HealthController, logback JSON, 83 testes JUnit |

---

## 13) Contato

- **Backend:** `backend/HELP.md` (gerado pelo Spring Initializr)
- **Frontend:** `frontend-motor/README.md` (gerado pelo Quasar)
- **DPO / LGPD:** `dpo@riser.com`
