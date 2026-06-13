# AGENTS.md — rizer-marketplaces

## Visão geral

Plataforma **SaaS B2B white-label** de anúncios de veículos (B2B, similar a iCarros / Webmotors).
Empresas parceiras (garagistas, concessionárias, redes) cadastram suas lojas, publicam
anúncios de veículos e ganham um site dedicado no subdomínio `slug.motorise.com.br`
ou em domínio próprio.

Planos: **Básico**, **PRO** (até 3 lojas, página de empresa parceira, Instagram), **Platinum** (META DPA + Google Shopping). Billing via Stripe + pagamentos manuais (livro-caixa em `payments`).

Detalhamento completo em `docs/PLAN-saas-b2b.md`.

## Hierarquia do domínio

```
Country (region context: BR, US)
  └── Tenant (empresa/rede — slug.motorise.com.br, domínio custom opcional)
        ├── PhysicalStore 1..N  (filiais com endereço e geolocalização)
        │     └── Product 1..N  (anúncio sempre vinculado a uma loja)
        └── TenantUser 1..N     (OWNER | MANAGER | SELLER)
User  (plataforma: sys_admin | sys_manager | agency_owner | agency_admin | agency_employee | user)
Plan + Subscription + Payment     (billing; plano é por tenant, não por loja)
```

- **Multi-tenant**: tenant ativo vem de JWT > header `X-Tenant-Slug` > path `/BR/public/tenants/{slug}` > Host (subdomínio/CNAME). Veja `TenantContextHolder` no backend.
- **RBAC duplo**: papel na plataforma (`users.system_role`) + papel dentro do tenant (`tenant_users.role`).
- **Multi-currency-ready** desde o dia 1 (CHAR(3) em `plans`, `payments`); BRL é o único seed nesta fase.
- **i18n**: pt-BR único nesta fase, mas `i18n/index.ts` mantém a estrutura.

## Repository Structure

Monorepo com dois projetos independentes:

- `backend/` — Spring Boot API (Java 25, Maven)
- `frontend-motor/` — Vue 3 + Quasar 2 frontend (SPA/SSR/PWA)
- `docs/PLAN-saas-b2b.md` — plano macro end-to-end (fonte de verdade para arquitetura)

## Backend

### Stack
- Spring Boot 4.0.5, Java 25, Spring Security + OAuth2 Client
- JPA/Hibernate + Hibernate Spatial (PostGIS), Flyway (migrations)
- jjwt 0.12.6 (HS256), Stripe SDK (planejado Fase 5)
- SpringDoc OpenAPI 3.0.2 (Scalar UI em `/docs`)
- AWS SDK S3 2.33.3 (Magalu Cloud) — `S3StorageService` já implementado

### Layout de pacotes
```
br.com.rizermarketplaces.core.marketplace
├── CoreMarketplaceApplication
├── config/         SecurityConfig, OpenApiConfig, aws/
├── context/        CountryContextHolder, TenantContextHolder + filters
├── auth/           AuthService, JwtTokenProvider, JwtAuthenticationFilter,
│                   PasswordEncoderConfig, OAuth2SuccessHandler, CurrentUser
├── controller/     públicos (auth, media, billing/webhooks)
├── controller/admin/   (a criar)
├── controller/tenant/  (a criar)
├── controller/public/  (a criar)
├── admin/          (a criar)
├── tenant/         (a criar)
├── catalog/        (a criar)
├── product/        (a criar)
├── lead/           (a criar)
├── billing/        (a criar)
├── integration/    (a criar)
├── audit/          (a criar)
├── rules/          (a criar)
├── tools/          (a criar)
├── model/          entidades JPA
├── repository/     Spring Data
├── dto/            DTOs de entrada/saída
└── service/        serviços de aplicação
```

### Migrations existentes
- `V1` — extensões PostGIS/ltree
- `V2-V4` — marcas e modelos FIPE (Carro, Moto, Caminhão, Náutico, Ônibus)
- `V5` — `countries` (region context; substitui `store` ambíguo)
- `V6` — `users` (com password_hash, system_role, soft delete) + `addresses` (PostGIS)
- `V7` — `tenants` (com theme JSONB, custom_domain, had_trial) + `tenant_users` (OWNER/MANAGER/SELLER, physical_store_ids)

### Migrations planejadas (não criadas ainda)
V8 physical_stores · V9 categories · V10 attribute_schemas · V11 products · V12 plans + subscriptions · V13 payments · V14 integrations · V15 leads + audit_log · V16 consents.

### Endpoints atuais
- `POST /auth/login` — email/senha → JWT + refresh cookie
- `POST /auth/login/refresh` — refresh
- `POST /auth/logout`
- `GET /auth/me` — user + memberships
- `POST /auth/switch-tenant` — seleciona tenant ativo
- `GET /oauth2/authorization/google` — inicia OAuth Google
- `GET /login/oauth2/code/google` — callback
- `POST /media/upload/image|document`, `GET /media/presign`, `DELETE /media` — já existiam

### Auth & segurança
- JWT HS256 com secret em `APP_JWT_SECRET` (mín. 32 bytes)
- Cookies HttpOnly `motorise_access` (60min) e `motorise_refresh` (30 dias)
- `JwtAuthenticationFilter` lê do header `Authorization: Bearer ...` ou do cookie
- `CountryContextFilter` popula `CountryContextHolder` a partir de `/{cc}/...` ou `X-Country-Code`
- `TenantContextFilter` popula `TenantContextHolder` a partir de header/path/host
- `ddl-auto: validate` (Flyway é a fonte de verdade do schema)
- `session: STATELESS`, CSRF desabilitado (API)

### Env vars
Veja `backend/.env.example`. Os principais:
- `APP_JWT_SECRET` (obrigatório)
- `APP_JWT_ACCESS_TTL_MINUTES` (default 60)
- `APP_JWT_REFRESH_TTL_DAYS` (default 30)
- `APP_OAUTH2_SUCCESS_REDIRECT` (default `http://localhost:3000/auth/callback`)

### Comandos
```bash
cd backend
cp .env.example .env  # preencher credenciais
./mvnw spring-boot:run
./mvnw test
```

## Frontend

### Stack
- Vue 3.5 + Quasar 2.16 (SPA/SSR/PWA)
- Vue Router 5, Vue I18n 11
- Axios 1.13 (instância `boot/axios.ts` injeta `X-Tenant-Slug` e `Authorization`)
- TypeScript strict mode
- Composables manuais (sem Pinia nesta fase) — `useAuthStore` em `src/stores/authStore.ts`

### Estrutura
- `src/boot/` — `i18n, axios, auth, animate, appReady, tenant`
- `src/stores/authStore.ts` — `useAuthStore` (singleton reativo)
- `src/composables/useTenant.ts` — multi-tenant por subdomínio/CNAME
- `src/data/tenants.ts` — registro estático de tenants (em migração para API)
- `src/data/types.ts` — tipos do domínio (Vehicle, Store, User, Lead...)
- `src/data/legalVersions.ts` — versão dos termos/privacidade/cookies
- `src/pages/`:
  - públicas: `HomePage, ProdutosPage, ProdutoPage, CompararPage, LojasParceirasPage, LojaPage, SejaParceiroPage, EntrarPage, RegistroPage, FavoritosPage, LeadsPage, AnunciosPage`
  - legais: `legal/TermosDeUsoPage, PoliticaPrivacidadePage, PoliticaCookiesPage`
  - app autenticado: `app/DashboardPage, StoresPage, ProductsPage, LeadsPage, MembersPage, BillingPage, IntegrationsPage, SettingsPage`
  - admin: `admin/AdminDashboardPage, AdminTenantsPage, AdminUsersPage, AdminPlansPage, AdminPaymentsPage`
- `src/layouts/`:
  - `MainLayout` (público) — header com login + drawer lateral
  - `AppLayout` (autenticado, tenant-scoped)
  - `AdminLayout` (sys_admin/manager)
- `src/router/routes.ts` — rotas nomeadas, meta `requiresAuth` / `requiredRoles`
- `src/router/guards.ts` — `authGuard` global (redireciona para `/entrar?redirect=...`)

### Auth flow (frontend)
1. `boot/auth.ts` chama `auth.refreshMe()` na inicialização (SSR-safe).
2. `authGuard` espera `initialized` antes de decidir; redireciona para `/entrar` se rota pedir auth.
3. `pages/EntrarPage.vue` chama `auth.login(email, password)`; em sucesso vai para `/app` (ou `?redirect=`).
4. `boot/axios.ts` lê cookie `motorise_access_token` e injeta `Authorization`; em 401 tenta refresh uma vez.

### Comandos
```bash
cd frontend-motor
pnpm install
pnpm quasar dev          # SPA
pnpm quasar dev -m ssr   # SSR
npm run lint
npx vue-tsc --noEmit
```

## Gotchas

- **VITE_API_URL**: defina no `.env` (ou `quasar.config.ts`) antes de ligar `MOCK_CONFIG.useBackend = true` em `src/services/api.ts`. Em dev local com Spring Boot na 8080 e Quasar na 9000, use `VITE_API_URL=http://localhost:8080`.
- **Cookie `motorise_access` é HttpOnly**: o JS do browser não consegue ler o valor. O navegador envia automaticamente em requests same-origin. Para cross-origin (Quasar 9000 → API 8080 em dev), é necessário configurar CORS no backend (ainda não configurado nesta fase — implementar antes de testar login real).
- **Custom domain**: gerenciado via Cloudflare API em fase futura. `tenants.custom_domain` + `custom_domain_status` já existem. SSL é **TODO fase futura**.
- **JWT secret**: nunca commitar. Gerar com `openssl rand -base64 48`.
- **Multi-currency**: nada de `enum BRL` no código. Use `countries.currency_code_iso` e formate com `Intl.NumberFormat(locale, { style: 'currency', currency })`.
- **LGPD**: `consents` (V16) ainda não existe. Banner de consentimento é fase 8.
- **Stripe**: não implementado. Use `manual_courtesy` no admin quando quiser liberar tenants sem Stripe.

## Estado atual (jun/2026)

✅ **Fase 1 concluída** — Fundações de dados e auth:
- V5/V6/V7 aplicadas; entidades JPA alinhadas; arquivos legados `SellerUser*` removidos.
- JWT real (jjwt 0.12.6), `AuthService`, `OAuth2SuccessHandler`, `/auth/me`, `/auth/switch-tenant`.
- `CountryContextFilter` + `TenantContextFilter` populam contexto por path/header/host.
- `SecurityConfig` STATELESS, sem CSRF, com matchers públicos.
- Frontend: `useAuthStore`, `EntrarPage`, layouts `AppLayout`/`AdminLayout`, guard global, páginas placeholder para `/app` e `/admin`, páginas legais (rascunho modelo).
- pt-BR único; i18n estruturado para ativação futura.
- Typecheck e lint limpos em ambos os projetos.

⏭ **Próximas fases** (resumo; detalhes em `docs/PLAN-saas-b2b.md`):
- **Fase 2** — Admin de tenants + lojas físicas (V8, `AdminTenantService`, `PhysicalStoreService`, `StoreLimitGuard`)
- **Fase 3** — Anúncios completos (V9-V11, wizard, validação JSONB)
- **Fase 4** — Páginas públicas de empresa parceira
- **Fase 5** — Planos e billing (V12-V13, Stripe + manual + trial)
- **Fase 6** — Integrações de marketing (Instagram, META, Google)
- **Fase 7** — Custom domain + SSL placeholder
- **Fase 8** — LGPD (V16, consent banner, retenção)
- **Fase 9** — Polish, auditoria, rate limit, testes básicos

## Docker & Deploy

Inalterado. Imagens:
```
container-registry.br-se1.magalu.cloud/rizer/
├── backend    # Spring Boot API
└── motorise   # Quasar SSR/PWA
```

Comandos de build/push em `docs/PLAN-saas-b2b.md` ou AGENTS.md anterior.
