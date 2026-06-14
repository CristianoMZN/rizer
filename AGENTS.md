# AGENTS.md — rizer-marketplaces

> **tl;dr para IAs:** Plataforma **SaaS B2B white-label multi-tenant** de anúncios de veículos (estilo iCarros / Webmotors).
> Empresas parceiras (garagistas, concessionárias, redes) assinam um dos planos (Básico R$99, PRO R$249, Platinum R$599), cadastram suas lojas, publicam anúncios e ganham um site dedicado em `slug.motorise.com.br` ou em domínio próprio.
>
> **Status (jun/2026):** MVP completo. 9 fases concluídas (ver `docs/PLAN-saas-b2b.md`).
> 17 migrations, ~50 entidades JPA, 21 controllers REST, 7 services de domínio, 8 jobs agendados, 5 filtros (JWT, CORS, Country, Tenant, RateLimit, CorrelationId), 23 testes JUnit passando.
>
> **Stack:** Spring Boot 4 + Java 25 + Postgres/PostGIS + Redis + Magalu S3 (Spring SDK) + jjwt 0.12.6 (HS256) + AES-GCM + Stripe (TODO prod) + Meta Graph API + Google Shopping + Cloudflare (TODO prod) · Vue 3 + Quasar 2 + Axios + TypeScript strict.

---

## Visão geral

### Hierarquia do domínio

```
Country (region context: BR, US, …)
  └── Tenant (empresa/rede — slug.motorise.com.br, custom domain opcional)
        ├── PhysicalStore 1..N  (filiais com endereço e geolocalização)
        │     └── Product 1..N  (anúncio sempre vinculado a uma loja)
        └── TenantUser 1..N     (OWNER | MANAGER | SELLER)

User                          (sys_admin | sys_manager | agency_owner | agency_admin | agency_employee | user)
Plan + Subscription + Payment (billing por tenant, não por loja)
TenantIntegration             (Instagram, META Business, Google Merchant — tokens encriptados)
Consent + DataExportRequest   (LGPD: art. 7° consentimento, art. 18 export/exclusão)
AuditEntry                    (audit log de ações sensíveis, retido 5 anos)
CustomDomainCheck             (log de verificações CNAME)
```

- **Multi-tenant:** tenant ativo vem de JWT claim `tenantId` > `X-Tenant-Slug` > path `/{cc}/public/tenants/{slug}` > Host (subdomínio/CNAME). `TenantContextHolder` é ThreadLocal populado por `TenantContextFilter`.
- **RBAC duplo:** papel na plataforma (`users.system_role`) + papel dentro do tenant (`tenant_users.role`).
- **Multi-currency-ready:** `plans.currency`, `payments.currency` são `CHAR(3)`. BRL é o único seed.
- **i18n:** pt-BR único nesta fase; `i18n/index.ts` mantém estrutura.

### Planos e billing

| code      | max_stores | partner_page | custom_domain | instagram | meta_dpa | google_shopping | price_cents | trial_days |
|-----------|------------|--------------|---------------|-----------|----------|-----------------|-------------|------------|
| `BASIC`   | 1          | false        | false         | false     | false    | false           | 9900        | 7          |
| `PRO`     | 3          | true         | true          | true      | false    | false           | 24900       | 14         |
| `PLATINUM` | null      | true         | true          | true      | true     | true            | 59900       | 14         |

`null` = ilimitado. `isInGracePeriod` libera `past_due` durante `grace_period_days` (default 7).

---

## Repository Structure

```
rizer-marketplaces/
├── backend/                  # Spring Boot API (Java 25, Maven)
│   ├── src/main/java/br/com/rizermarketplaces/core/marketplace/
│   ├── src/main/resources/
│   │   ├── application.yaml
│   │   ├── logback-spring.xml
│   │   └── db/migration/      # V1-V17 Flyway
│   ├── src/test/java/         # 23 testes JUnit
│   └── .env.example
├── frontend-motor/            # Vue 3 + Quasar 2 (SPA/SSR/PWA)
│   ├── src/
│   │   ├── boot/             # i18n, axios, auth, animate, appReady, tenant
│   │   ├── composables/      # useAuthStore (em stores), useTenant, useConsent, useLoading
│   │   ├── stores/           # useAuthStore
│   │   ├── services/         # api.ts (real), apiMock.ts (legado)
│   │   ├── layouts/          # MainLayout, AppLayout, AdminLayout
│   │   ├── pages/            # public, app, admin, partner, legal
│   │   ├── router/           # routes.ts, guards.ts
│   │   ├── data/             # types.ts, tenants.ts, legalVersions.ts
│   │   ├── i18n/             # pt-BR
│   │   └── components/       # layout, common (ConsentBanner, LoadingOverlay), etc.
│   └── package.json
├── docs/
│   ├── PLAN-saas-b2b.md      # plano macro (fonte de verdade da arquitetura)
│   ├── esquema.tabelas.md     # design inicial do schema (referência histórica)
│   └── AGENTS.md (este arquivo)
├── docker-compose.yml         # dev: postgres+postgis+redis+redis-insight
├── docker-compose.prod.yml    # prod: backend+frontend+postgres+redis (com imagens do Magalu Registry)
├── docker/postgres/init/      # SQL de inicialização
├── README.md                  # guia de setup, env vars, integrações
└── AGENTS.md                  # este arquivo
```

---

## Backend

### Stack
- Spring Boot 4.0.5, Java 25, Spring Security + OAuth2 Client + Resource Server
- JPA/Hibernate + Hibernate Spatial (PostGIS), Flyway (migrations)
- jjwt 0.12.6 (HS256), SpringDoc OpenAPI 3.0.2 (Scalar UI em `/docs`)
- AWS SDK S3 2.33.3 (Magalu Cloud) — `S3StorageService`
- AES-GCM 256 (criptografia de tokens) — `EncryptionService`
- Spring AI 2.0.0-M4 + Vector Store Redis (preparado, ainda não usado)
- Logback + LogstashEncoder (JSON structured logs, MDC `correlationId`)

### Layout de pacotes (17 packages, todos implementados)

```
br.com.rizermarketplaces.core.marketplace
├── CoreMarketplaceApplication
├── config/         SecurityConfig, OpenApiConfig, AwsS3Config, ApiErrorAdvice, CorrelationIdFilter, RateLimitFilter
├── context/        CountryContextHolder, TenantContextHolder + filters (CountryContextFilter, TenantContextFilter)
├── auth/           AuthService, JwtTokenProvider, JwtAuthenticationFilter, PasswordEncoderConfig, OAuth2SuccessHandler, CurrentUser, AuthenticatedUser
├── controller/     AuthController, HomeController, MediaController, BillingWebhookController, LgpdController, HealthController
├── controller/admin/   AdminTenantController, AdminBillingController
├── controller/tenant/  TenantStoreController, TenantMemberController, TenantProductController, TenantBillingController, TenantSettingsController, TenantIntegrationController, TenantTrialController
├── controller/publicapi/ PublicCatalogController, PublicPartnerController, PublicPlansController, PublicFeedsController
├── admin/          AdminTenantService
├── tenant/         PhysicalStoreService, CustomDomainService, DnsLookupService, StoreLimitGuard, CloudflareService, TenantExceptions, TenantMapper
├── catalog/        CatalogService (categorias, marcas, modelos FIPE)
├── product/        ProductService, ProductImageService
├── rules/          DynamicAttributeValidationService (JSON Schema validator subset draft-07)
├── billing/        PlanService, SubscriptionService, SubscriptionStateMachine, StripeService, BillingWebhookService, ManualPaymentService, TrialService, TrialExpirationJob
├── integration/    InstagramService, MetaCatalogService, GoogleShoppingFeedService, MetaGraphClient, EncryptionService, IntegrationAccessGuard, IntegrationSyncJob
├── lgpd/           ConsentService, DataExportService, DataRetentionJob
├── audit/          AuditService
├── tools/          CnpjValidator, SlugGenerator, PhoneNormalizer
├── model/          entidades JPA
├── repository/     Spring Data
├── dto/            DTOs de entrada/saída
└── service/        S3StorageService
```

### Migrations (V1-V17, todas aplicadas)

| Migration | Conteúdo |
|-----------|----------|
| V1 | Extensões Postgres: `postgis`, `postgis_topology`, `ltree` |
| V2 | `vehicle_brands` (FIPE) — schema |
| V3 | Seed marcas FIPE (CAR, MOTORCYCLE, TRUCK, NAUTICAL, BUS) |
| V4 | Seed modelos FIPE |
| V5 | `countries` (region context, substitui `store` do `esquema.tabelas.md`) — seed Brasil |
| V6 | `users` (uuid, email único, password_hash, system_role, soft delete) + `addresses` (geography) |
| V7 | `tenants` (theme JSONB, custom_domain, had_trial) + `tenant_users` (OWNER/MANAGER/SELLER, physical_store_ids uuid[]) |
| V8 | `physical_stores` (geolocalização, is_main, opening_hours JSONB) |
| V9 | `categories` ltree (até 3 níveis, seed dos 5 reinos) |
| V10 | `attribute_schemas` (JSON Schema por país + categoria) — seed veículo |
| V11 | `products` + `product_localizations` (price_cents BIGINT, location opcional) + `product_images` |
| V12 | `plans` (seed BASIC/PRO/PLATINUM) + `subscriptions` (status enum, source enum, grace_period_days) |
| V13 | `payments` (livro-caixa) + `stripe_invoices` |
| V14 | `tenant_integrations` (tokens encriptados para Instagram/META/Google) |
| V15 | `custom_domain_checks` (log de auditoria das verificações CNAME) |
| V16 | `consents` (LGPD) + `data_export_requests` (JSON + S3 presigned 7d) |
| V17 | `audit_log` (ação, recurso, severidade, payload, correlation_id) |

### Endpoints (por área)

| Área | Endpoint | Quem |
|------|----------|------|
| **Auth** | `POST /auth/login`, `POST /auth/login/refresh`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/switch-tenant` | público + autenticado |
| **OAuth Google** | `GET /oauth2/authorization/google`, `GET /login/oauth2/code/google` | público |
| **Media (S3)** | `POST /media/upload/image`, `POST /media/upload/document`, `GET /media/presign`, `DELETE /media?key=…&bucket=…` | autenticado |
| **Health** | `GET /health`, `GET /health/live`, `GET /health/ready` | público |
| **Admin · Tenants** | `GET /admin/tenants`, `GET/POST/PATCH/DELETE /admin/tenants/{id}` | `sys_admin`, `sys_manager` |
| **Admin · Billing** | `GET /admin/billing/plans`, `GET /admin/billing/payments`, `GET /admin/billing/stats`, `POST /admin/billing/tenants/{id}/payments`, `POST /admin/billing/tenants/{id}/trial/{planCode}`, `POST /admin/billing/tenants/{id}/subscription/{planCode}`, `PATCH /admin/billing/tenants/{id}/subscription/status` | `sys_admin`, `sys_manager` |
| **Tenant · Stores** | `GET/POST /tenant/stores`, `PATCH/DELETE /tenant/stores/{id}` | owner/manager |
| **Tenant · Members** | `GET/POST /tenant/members`, `PATCH/DELETE /tenant/members/{id}` | owner/manager |
| **Tenant · Products** | `GET/POST /tenant/products`, `GET/PATCH/DELETE /tenant/products/{id}`, `POST /tenant/products/{id}/images/upload` (multipart), `POST /tenant/products/{id}/images` (attach), `GET /tenant/products/{id}/images`, `DELETE /tenant/products/{id}/images/{imageId}` | owner/manager/seller |
| **Tenant · Integrations** | `GET /tenant/integrations`, `GET /tenant/integrations/{provider}/authorize`, `POST /tenant/integrations/{provider}/callback`, `DELETE /tenant/integrations/{provider}`, `POST /tenant/integrations/instagram/publish/{productId}` | owner |
| **Tenant · Billing** | `GET /tenant/billing/plans`, `GET /tenant/billing/subscription`, `POST /tenant/billing/checkout/{planCode}`, `POST /tenant/billing/portal`, `POST /tenant/billing/cancel`, `POST /tenant/billing/resume`, `GET/POST /tenant/billing/payments`, `POST /tenant/billing/trial/{planCode}` | owner |
| **Tenant · Settings** | `GET /tenant/settings`, `POST /tenant/settings/profile`, `GET/POST /tenant/settings/custom-domain`, `POST /tenant/settings/custom-domain/verify`, `GET /tenant/settings/custom-domain/history` | owner |
| **Public · Catalog** | `GET /{cc}/public/categories`, `GET /{cc}/public/categories/{realm}/subtypes`, `GET /{cc}/public/brands`, `GET /{cc}/public/brands/{id}/models`, `GET /{cc}/public/products` | público |
| **Public · Partners** | `GET /{cc}/public/tenants/partner`, `GET /{cc}/public/tenants/{slug}`, `GET /{cc}/public/tenants/{slug}/products` | público |
| **Public · Feeds** | `GET /{cc}/public/tenants/{slug}/feed.xml` (GMC), `GET /{cc}/public/tenants/{slug}/feed-meta.csv` (Meta) | público |
| **Public · Plans** | `GET /{cc}/public/billing/plans` | público |
| **Me · LGPD** | `POST /me/consents`, `GET /me/consents`, `POST /me/data-export`, `GET /me/data-export`, `DELETE /me/account` | autenticado |
| **Webhooks** | `POST /billing/webhooks/stripe` (real), `POST /billing/webhooks/simulate` (dev) | Stripe (com assinatura em prod) |

### Auth & segurança

- **JWT HS256** com secret em `APP_JWT_SECRET` (mín. 32 bytes, recomendado 48).
- **Cookies HttpOnly:** `motorise_access` (60min) + `motorise_refresh` (30d).
- **`JwtAuthenticationFilter`** lê do header `Authorization: Bearer …` ou do cookie.
- **`CountryContextFilter`** popula `CountryContextHolder` a partir de `/{cc}/...` ou `X-Country-Code`.
- **`TenantContextFilter`** popula `TenantContextHolder` a partir de JWT > header `X-Tenant-Slug` > path `/{cc}/public/tenants/{slug}` > Host.
- **`CorrelationIdFilter`** (HIGHEST_PRECEDENCE): gera/propaga `X-Correlation-Id` no response e no MDC.
- **`RateLimitFilter`:** in-memory, por IP+rota. 5/min em `/auth/**`, 60/min em `/admin/**`, 300/min no resto. Bypassa docs, openapi, health, feeds públicos. Responde 429 com `Retry-After: 60`.
- **CORS:** habilitado para `http://localhost:*` e `*.motorise.com.br`.
- **`ddl-auto: validate`** (Flyway é a fonte de verdade do schema).
- **`session: STATELESS`**, CSRF desabilitado.
- **`@PreAuthorize`** para RBAC: `hasAnyRole('sys_admin', 'sys_manager')` em `/admin/**`, `isAuthenticated()` em `/tenant/**`.
- **`ApiErrorAdvice`** (`@RestControllerAdvice`): converte exceções em `ProblemDetail` padronizado (RFC 7807) com `code` (machine-readable) e `detail`.

### Env vars (referência rápida)

> Detalhes completos e obrigatoriedades no `README.md` §5. Aqui só os agrupamentos.

| Grupo | Vars principais |
|-------|-----------------|
| **Banco/Redis** | `SPRING_DATASOURCE_URL/USER/PASSWORD`, `SPRING_DATA_REDIS_HOST/PORT/PASSWORD` |
| **JWT** | `APP_JWT_SECRET` (obrigatório), `APP_JWT_ACCESS_TTL_MINUTES=60`, `APP_JWT_REFRESH_TTL_DAYS=30`, `APP_JWT_ISSUER=motorise` |
| **Google OAuth** | `GOOGLE_OAUTH_CLIENT_ID/SECRET` |
| **S3 Magalu** | `APP_S3_REGION=br-se1`, `APP_S3_ENDPOINT`, `APP_S3_PUBLIC_BUCKET=rizer-pic`, `APP_S3_PRIVATE_BUCKET=rizer-storage`, `AWS_ACCESS_KEY_ID/SECRET` |
| **Criptografia** | `APP_ENCRYPTION_KEY` (base64, 32 bytes para AES-256) |
| **Plataforma** | `APP_TENANT_PLATFORM_DOMAIN=motorise.com.br`, `APP_OAUTH2_SUCCESS_REDIRECT` |
| **Rate limit** | `APP_RATELIMIT_AUTH_CAPACITY=5`, `APP_RATELIMIT_ADMIN_CAPACITY=60`, `APP_RATELIMIT_GLOBAL_CAPACITY=300` |
| **LGPD** | `APP_LGPD_RETENTION_JOB_ENABLED=true` |
| **Stripe** | `APP_STRIPE_ENABLED=false` (dev), `APP_STRIPE_API_KEY`, `APP_STRIPE_WEBHOOK_SECRET` |
| **Meta** | `APP_META_APP_ID/SECRET/REDIRECT_URI` |
| **Cloudflare** | `APP_CLOUDFLARE_ENABLED=false` (dev), `APP_CLOUDFLARE_API_TOKEN`, `APP_CLOUDFLARE_ZONE_ID` |

### Comandos
```bash
cd backend
cp .env.example .env       # preencher
./mvnw spring-boot:run      # sobe
./mvnw test                 # roda 23 testes JUnit
```

### Testes (23 JUnit, todos passando)
- `CnpjValidatorTest` (5) — algoritmo oficial Receita Federal (DV1 em `charAt(12)`, DV2 em `charAt(13)`, pesos da esquerda para a direita).
- `SlugGeneratorTest` (5) — normaliza acentos, colapsa hífens, trunca em 80, trata nulos.
- `PhoneNormalizerTest` (3) — `onlyDigits` remove tudo não-dígito.
- `SubscriptionStateMachineTest` (10) — `canCreateStore`/`canPublishAds` por status, `isInGracePeriod`, `assertFeatureEnabled` por feature.

### Jobs agendados (`@Scheduled` em `@EnableScheduling`)

| Job | Intervalo | Função |
|-----|-----------|--------|
| `TrialExpirationJob` | 1 hora | Cancela trials vencidos |
| `IntegrationSyncJob` | 5 min | Posta produtos novos no Instagram (até 3) e sincroniza META Catalog (até 20) |
| `DataRetentionJob` | 1 dia | Marca `data_export_requests` expirados como `expired` e remove arquivos do S3 |

(TODO em prod: `SubscriptionReconcileJob` para reconciliar com Stripe, `FeedRegenerateJob` para pré-gerar feeds.)

### Integrações — onde cada env var é consumida

| Service | Envs |
|---------|------|
| `S3StorageService` | `APP_S3_*`, `AWS_*` |
| `JwtTokenProvider` | `APP_JWT_*` |
| `EncryptionService` | `APP_ENCRYPTION_KEY` |
| `StripeService` | `APP_STRIPE_*` |
| `InstagramService` + `MetaGraphClient` | `APP_META_*` |
| `GoogleShoppingFeedService` | (sem env direto — Platinum-only via plan.hasGoogleShopping) |
| `CustomDomainService` | `APP_TENANT_PLATFORM_DOMAIN` |
| `CloudflareService` | `APP_CLOUDFLARE_*` |
| `DnsLookupService` | (usa JNDI puro, sem env) |
| `RateLimitFilter` | `APP_RATELIMIT_*` |
| `DataRetentionJob` | `APP_LGPD_*` |

---

## Frontend

### Stack
- Vue 3.5 + Quasar 2.16 (SPA/SSR/PWA)
- Vue Router 5, Vue I18n 11 (pt-BR único nesta fase)
- Axios 1.13 (instância `boot/axios.ts` injeta `X-Tenant-Slug` e `Authorization`, retry 1x em 401)
- TypeScript 5.9 strict mode
- Composables manuais (sem Pinia nesta fase)

### Estrutura
- `src/boot/` — `i18n, axios, auth, animate, appReady, tenant`
  - `axios.ts` injeta `X-Tenant-Slug` + tenta refresh em 401
  - `auth.ts` chama `auth.refreshMe()` na inicialização (SSR-safe)
  - `tenant.ts` resolve tenant pelo subdomínio
- `src/stores/authStore.ts` — `useAuthStore` (singleton reativo: `user`, `memberships`, `currentTenantId`, `login/logout/refresh/switchTenant`)
- `src/composables/`
  - `useTenant.ts` — multi-tenant por subdomínio/CNAME + `applyTheme` (injeta CSS variables no DOM)
  - `useConsent.ts` — LGPD banner state (cookie `motorise_consent`)
  - `useLoading.ts` — loading overlay state (singleton)
- `src/services/`
  - `api.ts` — cliente HTTP real (`adminApi`, `tenantApi`, `tenantProductApi`, `billingApi`, `integrationApi`, `partnerApi`, `lgpdApi`, `settingsApi`, `catalogApi`, `leadApi`)
- `src/data/`
  - `tenants.ts` — registro estático (em migração para API)
  - `types.ts` — Vehicle, Store, User, Lead, Notification, FinancingOption, etc.
  - `legalVersions.ts` — `TERMS_VERSION`, `PRIVACY_VERSION`, `COOKIES_VERSION` (`v1.0`)

### Layouts
- `MainLayout` (público) — header com login + drawer lateral
- `AppLayout` (autenticado, tenant-scoped) — sidebar Dashboard/Lojas/Anúncios/etc.
- `AdminLayout` (sys_admin/manager) — sidebar Tenants/Usuários/Planos/Pagamentos

### Pages (~40) por layout

- **MainLayout (~14):** `Home, Produtos, Produto, Comparar, LojasParceiras (/parceiros redirect), LojaPage, SejaParceiro, Entrar, Registro, Favoritos, Leads, Anuncios, ErrorNotFound`
- **Partner (3):** `PartnerListPage, PartnerPage, PartnerStorePage` (em `pages/partner/`)
- **Legal (3):** `TermosDeUso, PoliticaPrivacidade, PoliticaCookies`
- **AppLayout (10):** `Dashboard, Stores, Products, ProductWizard, ProductEdit, Leads, Members, Billing, Integrations, Settings`
- **AdminLayout (5):** `Dashboard, Tenants, Users, Plans, Payments`

### Auth flow (frontend)
1. `boot/auth.ts` chama `auth.refreshMe()` na inicialização (SSR-safe).
2. `authGuard` espera `initialized` antes de decidir; redireciona para `/entrar?redirect=…` se rota pedir auth.
3. `EntrarPage` chama `auth.login(email, password)`; em sucesso vai para `/app` (ou `?redirect=`).
4. `auth.switchTenant(tenantId)` renova o token com claim `tenantId`.
5. `boot/axios.ts` injeta `Authorization: Bearer <accessToken>` (cookie `motorise_access_token` em dev); em 401 tenta refresh.

### Comandos
```bash
cd frontend-motor
pnpm install
pnpm quasar dev             # SPA em http://localhost:9000
pnpm quasar dev -m ssr      # SSR
pnpm build                  # build de produção
npm run lint                # ESLint
npx vue-tsc --noEmit        # typecheck (0 erros)
```

---

## Estado atual (jun/2026)

✅ **9 fases concluídas** — MVP completo. Detalhes em `docs/PLAN-saas-b2b.md`.

**Métricas finais:**
- 17 migrations Flyway (V1-V17)
- ~50 entidades JPA
- 21 controllers REST (em 4 grupos: raiz, admin, tenant, publicapi)
- 7 services de domínio + 6 services de aplicação
- 8 jobs/schedulers
- 5 filtros HTTP (JWT, Country, Tenant, CorrelationId, RateLimit)
- 5 DTOs principais + 8 services de DTO
- 23 testes JUnit passando
- ~40 pages no frontend, 4 layouts, 1 store (authStore), 3 composables, 1 service (api.ts)
- Typecheck + lint limpos em ambos os projetos
- **Mocks removidos** — frontend depende exclusivamente da API real

## Próximos passos (fora do MVP)

Itens intencionalmente fora do escopo macro. Marcados como `TODO(fase-*-prod)` no código:

1. **Stripe SDK real** — ativar `APP_STRIPE_ENABLED=true`, plugar `stripe-java`, validar assinatura do webhook.
2. **Cloudflare API real** — `APP_CLOUDFLARE_ENABLED=true`, criar CNAMEs na zona via `client.zones.dnsRecords.create`.
3. **SSL automático** — Cloudflare Origin CA no LB ou Caddy on-demand TLS quando `custom_domain_status=VERIFIED`.
4. **Google Content API** — auto-update de produtos no Merchant Center via `accounts/{merchantId}/products`.
5. **DNS-over-HTTPS** — substituir JNDI do `DnsLookupService` por Cloudflare 1.1.1.1 ou Google 8.8.8.8.
6. **Meta App Review** — submeter scopes (`instagram_basic`, `instagram_content_publish`, `business_management`, `catalog_management`) para aprovação antes de produção.
7. **Tests E2E** — Testcontainers + WebMvcTest no backend; Vitest no frontend.
8. **CI/CD pipeline** — GitHub Actions ou GitLab CI com build, test, push, deploy.
9. **Notifications WebSocket** — Spring WebSocket está no pom mas não usado.
10. **Frontend i18n real** — adicionar en-US, es-AR, etc.
11. **Melhorias no marketplace search** — busca full-text + filtros avançados (ano, km, preço).
12. **Mobile app (React Native ou Flutter)** — para anunciantes gerenciarem anúncios em campo.

## Docker & Deploy

### Container Registry
```
container-registry.br-se1.magalu.cloud/rizer/
├── backend    # Spring Boot API
└── motorise   # Quasar SSR/PWA
```

Comandos completos no `README.md` §11 (login, build, push, pull, run prod).
