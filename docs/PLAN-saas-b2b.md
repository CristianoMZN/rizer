# Plano SaaS B2B — Rizer Marketplaces

> Plataforma white-label de anúncios de veículos (B2B SaaS, similar a iCarros / Webmotors).
> Stack: Spring Boot 4 + Java 25 (backend) · Vue 3 + Quasar 2 SSR/PWA (frontend) · PostgreSQL/PostGIS · Redis · Magalu S3 · Stripe · Cloudflare API.

## Índice

1. [Princípios de modelagem](#1-princípios-de-modelagem)
2. [Modelo de dados (migrations)](#2-modelo-de-dados)
3. [Planos, billing e trial](#3-planos-billing-e-trial)
4. [RBAC e tenancy](#4-rbac-e-tenancy)
5. [Backend — estrutura e endpoints](#5-backend)
6. [Frontend — rotas, páginas, componentes](#6-frontend)
7. [White-label e subdomínios](#7-white-label-e-subdomínios)
8. [Integrações de marketing](#8-integrações-de-marketing)
9. [LGPD, termos, cookies](#9-lgpd-termos-cookies)
10. [Fases de implementação](#10-fases-de-implementação)
11. [Riscos e decisões em aberto](#11-riscos-e-decisões-em-aberto)

---

## 1. Princípios de modelagem

- **Tenant = a empresa/rede** (CNPJ, marca, subdomínio `slug.motorise.com.br`, domínio customizado opcional). Tenant tem 1..N **PhysicalStore** (filiais) com endereço próprio.
- **Plano é por tenant**, não por loja. PRO limita a **3 lojas ativas**. Platinum libera **META DPA + Google Shopping** e remove limite de lojas.
- **Anúncio pertence a uma loja** (`products.physical_store_id NOT NULL`). Geolocalização do anúncio pode herdar da loja (default) ou ser customizada.
- **Tenant público vs privado**: Basic nasce `is_public=false`; PRO/Platinum nasce `is_public=true` e aparece em `/parceiros`.
- **Custom domain**: gerenciado via Cloudflare API (DNS). Emissão de SSL é **TODO fase futura** (placeholder no código).
- **Integrações Meta/Google**: cada tenant tem sua própria conta OAuth, tokens criptografados em repouso.
- **Multi-currency-ready** desde o dia 1: `plans.currency CHAR(3)`, `payments.currency CHAR(3)`, `countries.currency_code_iso`. Nenhum `enum BRL` no código. Default é BRL.
- **i18n**: pt-BR único nesta fase, mas estrutura mantida para ativação futura.

---

## 2. Modelo de dados

### 2.1 Migrations (ordem importa)

| Migration | Conteúdo |
|---|---|
| `V5__create_countries.sql` | Tabela `countries` (region context, ex.: BR, US). Substitui o conceito ambíguo de `store` no `esquema.tabelas.md`. Seed do Brasil. |
| `V6__create_users_and_addresses.sql` | `users` (uuid, email único, phone, password_hash, attributes JSONB, system_role, soft delete). `addresses` (geography(Point,4326)). |
| `V7__create_tenants_and_tenant_users.sql` | `tenants` (uuid, country_code FK, slug único por country, cnpj, name, logo_url, banner_url, description, theme JSONB, custom_domain, custom_domain_status, status, is_public, is_partner_page_enabled, had_trial). `tenant_users` (tenant_id, user_id, role OWNER/MANAGER/SELLER, is_active, expire_at, physical_store_ids uuid[]). |
| `V8__create_physical_stores.sql` | `physical_stores` (uuid, tenant_id FK, name, slug, address_id FK, phone, whatsapp, email, opening_hours JSONB, is_main, is_active, location geography(Point,4326)). Limite de 3 lojas PRO enforçado no service. |
| `V9__create_categories.sql` | `categories` ltree (até 3 níveis). Seed das 5 reinos (Carro, Moto, Caminhão, Náutico, Ônibus). |
| `V10__create_attribute_schemas.sql` | `attribute_schemas` (entity_type, country_code, category_path ltree, version, is_active, schema_definition JSONB). Seed veículo. |
| `V11__create_products.sql` | `products` (uuid, tenant_id, physical_store_id, category_id, brand_id, model_id, vehicle_type, year_model, year_build, mileage_km, fuel, transmission, attributes JSONB, status, created_by_user_id). `product_localizations` (country_code, title, description, price_cents BIGINT, currency CHAR(3), location geography(Point,4326) opcional, location_source enum). `product_images` (product_id, key S3, sort, is_cover). |
| `V12__create_plans_and_subscriptions.sql` | `plans` (code PK, name, max_physical_stores, flags de features, price_cents, currency, trial_days, stripe_price_id). `subscriptions` (tenant_id UNIQUE, plan_code, status enum, current_period_*, trial_*, source enum, stripe_customer_id, stripe_subscription_id, grace_period_days). Seed: BASIC, PRO, PLATINUM. |
| `V13__create_payments.sql` | `payments` (livro-caixa: tenant_id, subscription_id, method enum, status enum, amount_cents, currency, period_*, description, external_reference, receipt_url, paid_at, recorded_by_user_id, notes). `stripe_invoices` (payment_id FK, stripe_invoice_id, raw_payload JSONB). |
| `V14__create_integrations.sql` | `tenant_integrations` (tenant_id, provider enum INSTAGRAM/META_BUSINESS/GOOGLE_MERCHANT, access_token_encrypted, refresh_token_encrypted, expires_at, account_id, account_name, scopes, status, last_sync_at, error_message). Tokens com pgcrypto/KMS. |
| `V15__create_leads_and_audit.sql` | `leads` (uuid, tenant_id, physical_store_id, product_id, buyer_*, status enum, assigned_user_id, consent_at). `audit_log` (actor_user_id, tenant_id, action, resource, payload JSONB, ip, ua). |
| `V16__create_consents.sql` | `consents` (user_id opcional, anonymous_id, purpose enum, granted, ip, user_agent, document_version). |

---

## 3. Planos, billing e trial

### 3.1 Seeds de planos

| code | max_stores | partner_page | custom_domain | instagram | meta_dpa | google_shopping | price_cents | currency | trial_days |
|---|---|---|---|---|---|---|---|---|---|
| `BASIC` | 1 | false | false | false | false | false | 9900 | BRL | 7 |
| `PRO` | 3 | true | true | true | false | false | 24900 | BRL | 14 |
| `PLATINUM` | null | true | true | true | true | true | 59900 | BRL | 14 |

### 3.2 Subscription status (máquina de estados)

```
TRIALING  → ACTIVE | CANCELED
ACTIVE    → PAST_DUE | CANCELED | PAUSED
PAST_DUE  → ACTIVE | UNPAID | CANCELED
UNPAID    → ACTIVE | CANCELED
PAUSED    → ACTIVE
CANCELED  → (terminal)
```

Enforcements (centralizados em `SubscriptionStateMachine`):

| status | criar loja | postar IG | META DPA | Google Shopping | publicar anúncio |
|---|---|---|---|---|---|
| `trialing` | ✓ (se plano cobre) | ✓ | ✓ | ✓ | ✓ |
| `active` | ✓ | ✓ | ✓ | ✓ | ✓ |
| `past_due` | ✓ (banner) | ✓ | ✓ | ✓ | ✓ |
| `paused` | ✗ (read-only) | ✗ | ✗ | ✗ | ✗ |
| `unpaid` / `canceled` | ✗ | ✗ | ✗ | ✗ | ✗ |

### 3.3 Trial

- Por tenant, **uma única vez** (`tenants.had_trial`). Mesmo CNPJ não ganha novo trial.
- Oferecido quando admin cria tenant ou quando há upgrade de plano (trial do novo plano).
- `TrialExpirationJob` (1×/hora) cancela trials expirados.

### 3.4 Pagamentos manuais

- `ManualPaymentService` registra pagamentos fora do Stripe (dinheiro, transferência, Pix externo, bônus, cortesia).
- `POST /tenant/payments/manual` (owner) ou `POST /admin/tenants/{id}/payments` (sys_admin).
- Cria `payment(succeeded)` e renova/atualiza `subscription`.
- `amount_cents = 0` é aceito para cortesia/bônus.
- `payments` é o livro-caixa — saldo e histórico sempre consultáveis.

### 3.5 Stripe

- 3 produtos (BASIC, PRO, PLATINUM), preço mensal em BRL.
- Webhook handler: `checkout.session.completed`, `invoice.paid`, `invoice.payment_failed`, `customer.subscription.updated/deleted`, `customer.subscription.trial_will_end`.
- `stripe_customer_id` salvo em `subscriptions`.
- Customer Portal para gerenciar cartão.

### 3.6 Multi-currency (preparação)

- `plans.currency` e `payments.currency` são `CHAR(3)`.
- País define moeda via `countries.currency_code_iso`.
- Quando expandir para US, criar `plan_prices(plan_code, currency, price_cents, stripe_price_id)`.

---

## 4. RBAC e tenancy

### 4.1 `users.system_role` (papel na plataforma)

```
sys_admin       → tudo na plataforma
sys_manager     → admin, mas não pode mudar planos/preços
sys_employee    → leitura no admin
agency_owner    → dono de tenant (criado quando admin registra o tenant)
agency_admin    → gerente (convidado pelo owner)
agency_employee → vendedor (convidado pelo owner/admin)
user            → buyer/anônimo
```

### 4.2 `tenant_users.role` (papel dentro do tenant)

```
OWNER     → tudo no tenant
MANAGER   → gerencia lojas, membros, vê assinatura, edita produtos
SELLER    → cria/edita produtos das lojas que ele representa
```

`tenant_users.physical_store_ids uuid[]` limita o SELLER a filiais específicas.

### 4.3 Matriz de ações

| Ação | sys_admin | owner | manager | seller |
|---|---|---|---|---|
| CRUD tenants | ✓ | ✗ | ✗ | ✗ |
| Editar plano do tenant | ✓ | ✗ | ✗ | ✗ |
| Editar tenant próprio | ✓ (impersonate) | ✓ | parcial | ✗ |
| CRUD filiais | ✓ | ✓ | ✓ | ✗ |
| Convidar sellers | ✓ | ✓ | ✓ | ✗ |
| Criar/editar anúncio | ✓ | ✓ | ✓ | ✓ (apenas suas filiais) |
| Publicar no Instagram | ✓ | ✓ | ✓ | ✗ |
| Conectar integração | ✓ | ✓ | ✗ | ✗ |
| Ver leads | ✓ | ✓ (todas) | ✓ (todas) | ✓ (suas filiais) |
| Ver assinatura | ✓ | ✓ | ✗ | ✗ |
| Lançar pagamento manual próprio | ✓ | ✓ | ✗ | ✗ |
| Lançar pagamento manual em qualquer tenant | ✓ | ✗ | ✗ | ✗ |

### 4.4 Multi-tenant (resolução)

- **Subdomínio** (`slug.motorise.com.br`) → `tenants.slug`.
- **Domínio customizado** (CNAME) → `tenants.custom_domain`.
- **Header** `X-Tenant-Slug` (SSR, chamadas internas).
- **JWT claim** `tenantId` (escolhido no login).
- `TenantContextHolder` (ThreadLocal) é populado pelo `TenantContextFilter` (lê JWT > header > path) e validado contra `tenant_users`.

---

## 5. Backend

### 5.1 Estrutura de pacotes

```
br.com.rizermarketplaces.core.marketplace
├── CoreMarketplaceApplication
├── config/        SecurityConfig, OpenApiConfig, DocsRedirectController, aws/
├── context/       CountryContextHolder, TenantContextHolder + filters
├── auth/          AuthService, JwtTokenProvider, JwtAuthenticationFilter,
│                  PasswordEncoderConfig, OAuth2SuccessHandler, CurrentUser
├── controller/    públicos (auth, media, billing/webhooks)
├── controller/admin/  sys_admin
├── controller/tenant/ autenticado com tenant
├── controller/public/ /{cc}/public/** (sem auth)
├── admin/         AdminTenantService, AdminUserService, AdminPaymentService
├── tenant/        TenantService, TenantMemberService, PhysicalStoreService,
│                  StoreLimitGuard, CustomDomainService
├── catalog/       CategoryService, BrandService, ModelService, AttributeSchemaService
├── product/       ProductService, ProductSearchService, ProductImageService
├── lead/          LeadService
├── billing/       PlanService, SubscriptionService, SubscriptionStateMachine,
│                  StripeService, BillingWebhookService, ManualPaymentService,
│                  TrialService, jobs
├── integration/   InstagramService, MetaCatalogService, GoogleShoppingService,
│                  CloudflareService, EncryptionService
├── audit/         AuditService, AuditAspect
├── rules/         DynamicAttributeValidationService
└── tools/         SlugGenerator, CnpjValidator, PhoneNormalizer
```

### 5.2 Endpoints principais

#### Auth
- `POST /auth/login` — email/senha → JWT + refresh cookie
- `POST /auth/login/refresh` — refresh
- `POST /auth/logout`
- `GET /auth/me` — user + memberships
- `GET /auth/oauth2/authorization/google` — inicia OAuth
- `GET /login/oauth2/code/google` — callback

#### Admin (sys_admin)
- `GET/POST /admin/tenants`
- `GET/PATCH/DELETE /admin/tenants/{id}`
- `POST /admin/tenants/{id}/impersonate`
- `POST /admin/tenants/{id}/payments` — pagamento manual
- `GET /admin/payments` — livro-caixa global
- `GET /admin/payments/export.csv`
- `GET/PATCH /admin/plans`
- `GET/POST /admin/users`

#### Tenant (autenticado)
- `GET /tenant/me`
- `GET/POST /tenant/stores` (valida limite)
- `PATCH/DELETE /tenant/stores/{id}`
- `GET/POST /tenant/members`
- `PATCH/DELETE /tenant/members/{id}`
- `GET /tenant/subscription`
- `POST /tenant/subscription/checkout` — Stripe
- `POST /tenant/subscription/portal` — Customer Portal
- `POST /tenant/subscription/cancel` — `cancel_at_period_end`
- `GET/POST /tenant/payments` — histórico + manual
- `GET/POST/PATCH/DELETE /tenant/products`
- `POST /tenant/products/{id}/images`
- `POST /tenant/products/{id}/publish-instagram`
- `GET /tenant/integrations`
- `GET/POST /tenant/integrations/{provider}/connect`
- `GET /tenant/integrations/{provider}/callback`
- `DELETE /tenant/integrations/{provider}`
- `GET /tenant/custom-domain`
- `POST /tenant/custom-domain`
- `POST /tenant/custom-domain/verify`

#### Público
- `GET /{cc}/public/plans`
- `GET /{cc}/public/tenants/partner`
- `GET /{cc}/public/tenants/{slug}`
- `GET /{cc}/public/tenants/{slug}/products`
- `GET /{cc}/public/tenants/{slug}/feed.xml` — Google Shopping
- `GET /{cc}/public/tenants/{slug}/feed-meta.xml` — Meta Catalog
- `GET /legal/{termos-de-uso|politica-de-privacidade|politica-de-cookies}` — texto estático

#### Webhooks
- `POST /billing/webhooks/stripe`

### 5.3 Jobs agendados

- `TrialExpirationJob` (1×/hora)
- `InstagramSyncJob` (5 min)
- `FeedRegenerateJob` (diário)
- `SubscriptionReconcileJob` (1×/dia)
- `DataRetentionJob` (1×/dia) — anonimiza leads antigos, mantém payments 5 anos
- `CustomDomainSyncJob` (placeholder, fase futura) — `// TODO(fase-2): SSL via Cloudflare Origin CA`

---

## 6. Frontend

### 6.1 Rotas

```
Público (PublicLayout)
  /
  /produtos
  /produto/:id
  /comparar
  /parceiros                  lista de empresas parceiras
  /parceiros/:slug            perfil e-commerce da empresa
  /parceiros/:slug/loja/:storeSlug
  /seja-parceiro              planos
  /entrar                     login
  /legal/termos-de-uso
  /legal/politica-de-privacidade
  /legal/politica-de-cookies

Autenticado (AppLayout, guard: tenantMembership)
  /app                        dashboard
  /app/lojas                  lista filiais
  /app/lojas/nova
  /app/lojas/:id
  /app/membros
  /app/membros/convite
  /app/anuncios
  /app/anuncios/novo
  /app/anuncios/:id/editar
  /app/leads
  /app/assinatura
  /app/assinatura/checkout
  /app/integracoes
  /app/integracoes/instagram/callback
  /app/configuracoes          perfil tenant + custom domain
  /favoritos                  (público para logado)
  /anuncios                   LEGADO → redireciona para /app/anuncios

Admin (AdminLayout, guard: systemRole in [sys_admin, sys_manager])
  /admin/dashboard
  /admin/tenants
  /admin/tenants/novo
  /admin/tenants/:id
  /admin/users
  /admin/planos
  /admin/payments
  /admin/financeiro           MRR, churn, gráfico simples
```

### 6.2 Layouts

- `PublicLayout` — atual `MainLayout` renomeado.
- `AppLayout` — autenticado, sidebar tenant-scoped.
- `AdminLayout` — sidebar admin (Tenants, Usuários, Planos, Pagamentos, Auditoria).

### 6.3 Componentes

- `tenant/TenantForm.vue`, `tenant/TenantCard.vue`, `tenant/TenantBadge.vue`
- `store/PhysicalStoreForm.vue`, `store/PhysicalStoreCard.vue`, `store/StoreHoursEditor.vue`
- `plan/PlanCard.vue`, `plan/PlanComparison.vue`, `plan/PriceDisplay.vue`, `plan/SubscriptionStatusBadge.vue`, `plan/StripeCheckoutButton.vue`
- `payment/PaymentHistoryTable.vue`, `payment/ManualPaymentDialog.vue`, `payment/PaymentMethodIcon.vue`, `payment/ReceiptUploader.vue`
- `integration/InstagramConnectButton.vue`, `integration/MetaConnectButton.vue`, `integration/GoogleMerchantConnectButton.vue`
- `product/ProductWizard.vue`, `product/LocationSourceToggle.vue`
- `admin/AdminTable.vue`, `admin/SubscriptionAdminPanel.vue`, `admin/AdminPaymentsTable.vue`
- `common/CnpjInput.vue`, `common/PhoneInput.vue`, `common/ConsentBanner.vue`, `common/PriceGuardBanner.vue`, `common/LegalFooter.vue`

### 6.4 Stores Pinia

- `authStore` — user, token, memberships, switchTenant
- `tenantStore` — currentTenant, plan, subscription, stores, members
- `billingStore` — payments, ações de assinatura
- `adminBillingStore` — visão sys_admin
- `catalogStore` — categories, brands, models
- `legalStore` — versionamento de termos + consentimento

### 6.5 API layer

- `src/services/api.ts` mantém compat com mock; `MOCK_CONFIG.useBackend` flag.
- Interceptor axios injeta `Authorization` (do cookie ou store) e `X-Tenant-Slug`.
- Interceptor de erro: 401 → refresh; 402 → `/app/assinatura`; 403 → toast; 5xx → log.

---

## 7. White-label e subdomínios

### Fase atual
- `tenants.custom_domain` e `tenants.custom_domain_status` (PENDING/VERIFIED/FAILED).
- Painel `/app/configuracoes` mostra subdomínio + instruções de CNAME.
- `POST /tenant/custom-domain/verify`: backend faz `dig +short CNAME <domain>` e confere se aponta para `slug.motorise.com.br`. **Não emite SSL**.
- Ingress: nginx com `server_name *.motorise.com.br motorise.com.br` + `proxy_pass` no backend.
- Marcação explícita: `// TODO(fase-2): SSL via Cloudflare Origin CA`.

### Fase futura
- Cloudflare API: `client.zones.dnsRecords.create({type:'CNAME', name:slug, content:'proxy.motorise.com.br', proxied:true})`.
- SSL via Cloudflare Origin CA ou Caddy on-demand TLS.

---

## 8. Integrações de marketing

### Instagram
- App review Meta Business: `instagram_basic`, `instagram_content_publish`, `pages_show_list`, `business_management`.
- OAuth por tenant; tokens criptografados em `tenant_integrations`.
- `InstagramSyncJob` (5 min) posta produtos ativos que ainda não postaram.

### META DPA
- OAuth (`catalog_management`, `business_management`) ou feed público (`/feed-meta.xml`).
- Limitar por raio geográfico via `physical_store.location`.

### Google Shopping
- `GET /{cc}/public/tenants/{slug}/feed.xml` (formato GMC).
- Platinum-only.
- Para Platinum, também via Content API OAuth (auto-update).

---

## 9. LGPD, termos, cookies

### Páginas legais
- `/legal/termos-de-uso`
- `/legal/politica-de-privacidade`
- `/legal/politica-de-cookies`

### Consentimento
- `<ConsentBanner />` aparece na 1ª visita (e quando `documentVersion` muda).
- Persiste em cookie `motorise_consent` (uuid + JSON) e em `consents` quando logado.
- Bloqueia analytics/marketing até consent granted.
- Botão "Privacidade" no rodapé sempre disponível para revisar.

### Direitos do titular
- `GET /me/data-export` — JSON com todos os dados, link S3 válido 7 dias.
- `DELETE /me/account` — soft delete + anonimização de leads após 30 dias; payments retidos 5 anos (fiscal).

### Retenção
- `audit_log`, `payments`: 5 anos.
- `leads` sem interação: 2 anos.
- `consents`: 5 anos (prova de consentimento).

---

## 10. Fases de implementação

### Fase 1 — Fundações de dados e auth
- V5, V6, V7 + entidades JPA
- JWT real, OAuth Google, `/auth/me`
- `CountryContextHolder` + `TenantContextHolder` + filters
- Frontend: `authStore`, login real, escolha de tenant

### Fase 2 — Admin de tenants + lojas
- V8 + entidade
- `AdminTenantService`, `PhysicalStoreService`, `StoreLimitGuard`
- Frontend: `/admin/tenants`, `/app/lojas`, `/app/membros`

### Fase 3 — Anúncios completos
- V9–V11 + entidades
- Categorias seed
- `ProductService` com validação JSONB
- `ProductImageService` via `/media`
- Frontend: wizard de anúncio

### Fase 4 — Páginas públicas de empresa parceira
- `/parceiros` e `/parceiros/:slug`

### Fase 5 — Planos e billing
- V12 + V13
- Integração Stripe (Checkout + Webhook + Portal)
- `ManualPaymentService`
- `TrialService`
- Frontend: `/seja-parceiro` real, `/app/assinatura`

### Fase 6 — Integrações de marketing
- V14 + criptografia
- Instagram OAuth + sync
- Feeds Google Shopping + Meta Catalog
- OAuth META Business e Google Content API

### Fase 7 — Custom domain + SSL (placeholder)
- V7 ganha custom_domain fields
- UI + verificação CNAME
- `// TODO(fase-2): SSL` marcado

### Fase 8 — LGPD, termos, cookies
- V16
- Páginas legais
- `<ConsentBanner />`
- Retenção + export/delete

### Fase 9 — Polish e hardening
- Auditoria visível
- Rate limit
- Testes básicos
- OpenAPI revisado
- AGENTS.md atualizado

---

## 11. Riscos e decisões em aberto

- **Stripe em BR** funciona com cartão; Pix/boleto via Stripe Brasil ou gateway alternativo (PagSeguro/Mercado Pago) em fase posterior.
- **LGPD**: texto das páginas legais pode ser rascunho modelo ou contrato com advogado (decisão do produto).
- **FIPE sync**: V2/V3/V4 não atualizam preços. Job semanal (Paralela API) previsto fora do escopo desta entrega.
- **i18n**: pt-BR único nesta fase, mas `i18n/index.ts` mantém estrutura para ativação futura.
- **Testes**: JUnit 5 no backend (já disponível). Frontend sem framework configurado — não adicionar Vitest nesta fase a menos que solicitado.
