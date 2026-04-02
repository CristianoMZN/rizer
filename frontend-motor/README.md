# Motorise Marketplace Frontend

Frontend do marketplace Motorise, construído com Vue 3 + Quasar 2 + TypeScript, com foco em:

- Catálogo de veículos com filtros inteligentes
- Página de produto com verificação e financiamento
- Estrutura multi-tenant / white-label por domínio
- Mock data com chave única para alternar backend
- Suporte aos modos SPA, SSR e PWA
- Resolução de tenant no SSR por `Host` header e domínio customizado via CNAME

## Stack

- Vue 3
- Quasar 2 (Vite)
- TypeScript (strict)
- Vue Router
- Vue I18n
- Axios
- animate.css

## Estrutura Principal

### Layout e navegação

- Header com busca e navegação desktop: src/components/layout/AppHeader.vue
- Drawer mobile (não aparece no desktop): src/layouts/MainLayout.vue
- Footer no fim do conteúdo (não fixo), adaptado por tenant: src/layouts/MainLayout.vue
- Splash/loading overlay no reload inicial: src/App.vue + index.html + src/boot/appReady.ts

### Componentes reutilizáveis

#### Layout

- src/components/layout/AppHeader.vue
- src/components/layout/ProgressStepper.vue
- src/components/layout/LoadingSpinner.vue

#### Formulários

- src/components/form/SmartSearch.vue
- src/components/form/FilterPanel.vue
- src/components/form/ImageUploader.vue
- src/components/form/TagInput.vue
- src/components/form/LocationPicker.vue
- src/components/form/PriceRangeSlider.vue

#### Veículos

- src/components/vehicle/VehicleCard.vue
- src/components/vehicle/VehicleVerification.vue
- src/components/vehicle/PriceChart.vue
- src/components/vehicle/FinancingSimulator.vue
- src/components/vehicle/ComparisonTable.vue

#### Negócio

- src/components/business/StoreProfile.vue
- src/components/business/LeadDashboard.vue
- src/components/business/NotificationBell.vue
- src/components/business/WishlistCard.vue

## Páginas e Rotas

### Públicas

- / → src/pages/HomePage.vue
- /produtos → src/pages/ProdutosPage.vue
- /produto/:id → src/pages/ProdutoPage.vue
- /comparar → src/pages/CompararPage.vue
- /lojas-parceiras → src/pages/LojasParceirasPage.vue
- /seja-parceiro → src/pages/SejaParceiroPage.vue
- /registro → src/pages/RegistroPage.vue

### Loja temática (dinâmica)

- /lojas/:slug → src/pages/LojaPage.vue
- /lojas/:slug/produtos → src/pages/LojaPage.vue

### Área autenticada (mock)

- /favoritos → src/pages/FavoritosPage.vue
- /leads → src/pages/LeadsPage.vue
- /anuncios → src/pages/AnunciosPage.vue

Configuração de rotas central: src/router/routes.ts

## Dados Mock e Integração com Backend

### Tipagem

- src/data/types.ts

Contém tipos como Vehicle, Store, Lead, Notification, User, VehicleFilters e catálogos auxiliares.

### Mock principal

- src/data/mock/index.ts

Inclui:

- Lojas, veículos, leads, notificações e usuário mock
- Imagens com URLs do Unsplash para veículos, lojas e logos

### Camada de API

- src/services/api.ts

Use a flag abaixo para alternar entre mock e backend:

```ts
export const MOCK_CONFIG = {
	useBackend: false,
	apiBase: import.meta.env.VITE_API_URL || '/api',
}
```

Quando useBackend = false, os dados vêm de src/data/mock/index.ts.
Quando useBackend = true, a camada passa a consumir endpoints reais.

### Escopo multi-tenant da API

A camada de API aplica escopo automático conforme o tenant resolvido:

- Tenant `default` (`mode: 'marketplace'`): retorna dados globais do marketplace
- Tenant de loja (`mode: 'store'`): restringe veículos, leads e lojas à `storeSlug` configurada

No backend real, as requisições enviam o header abaixo quando o tenant é uma loja:

```http
X-Tenant-Slug: top-motos-rj
```

O backend deve usar esse header para filtrar queries por loja.

## Sistema de Tema e Multi-tenant

### Tokens visuais globais

- src/css/app.scss
- src/css/quasar.variables.scss

Inclui gradientes, glassmorphism e paleta customizada.

### Resolução de tenant

Arquivos principais:

- src/data/tenants.ts
- src/composables/useTenant.ts
- src/boot/tenant.ts

O tenant é resolvido por prioridade:

1. Subdomínio da plataforma

```text
joao.motorise.com.br -> tenant "joao"
```

2. Domínio próprio do lojista via CNAME

```text
top-motos-rj.com.br -> tenant da loja configurada com customDomain
```

3. Fallback para o marketplace padrão

```text
motorise.com.br -> tenant "default"
```

### Estrutura do tenant

Cada tenant pode definir:

- `mode`: `marketplace` ou `store`
- `storeSlug`: slug da loja no backend/mock
- `storeName`
- `footerTagline`
- `logoUrl`
- `customDomain`
- `theme.primary`
- `theme.secondary`
- `theme.accent`
- `theme.dark`
- `theme.darkPage`
- `visibleMenuItems`

### Comportamento por modo

- `marketplace`: experiência da Motorise com todos os anúncios e navegação ampla
- `store`: experiência white-label, com tema, branding e escopo restrito à loja do cliente

### Exemplo de ativação de e-commerce white-label

Fluxo esperado para um lojista:

1. Lojista ativa o e-commerce em `Configurações -> Minha loja`
2. Backend persiste branding, domínio, cores, slogan, logo e `storeSlug`
3. Plataforma publica o subdomínio padrão:

```text
top-motos-rj.motorise.com.br
```

4. Se o lojista quiser domínio próprio, ele configura no DNS:

```text
top-motos-rj.com.br -> CNAME -> top-motos-rj.motorise.com.br
```

5. O SSR recebe o `Host` da requisição e resolve o mesmo tenant tanto no subdomínio da plataforma quanto no domínio customizado

### Menu e branding dinâmicos

Header, drawer e footer respeitam o tenant atual:

- nome da loja
- logo
- slogan do footer
- itens visíveis de navegação
- cores do tema aplicadas em runtime via CSS custom properties

## Financiamentos Aprovados

Componente: src/components/vehicle/FinancingSimulator.vue

Foi ajustado para exibir lista de ofertas aprovadas com visual de balanço financeiro:

- Verde: melhor custo
- Amarelo: intermediário
- Vermelho: maior custo

Também apresenta resumo de:

- Melhor parcela
- Média de juros
- Maior parcela

## Boot e Configuração Quasar

Arquivo: quasar.config.ts

Boot files ativos:

- i18n
- axios
- animate
- appReady
- tenant

Arquivo de boot de animações:

- src/boot/animate.ts

Arquivo de boot do tenant:

- src/boot/tenant.ts

Arquivo de boot do loading inicial:

- src/boot/appReady.ts

Router mode ativo:

- history (sem hash na URL)

## Como rodar localmente

### 1) Instalar dependências

```bash
npm install
```

ou

```bash
pnpm install
```

### 2) Rodar em modo dev (SPA padrão)

```bash
npm run dev
```

ou

```bash
npx quasar dev
```

Para testar tenant localmente, use um host customizado apontando para `127.0.0.1` e acesse o SSR com esse domínio.

Exemplo em `/etc/hosts`:

```text
127.0.0.1 joao.motorise.local
127.0.0.1 top-motos-rj.local
```

Depois ajuste a variável de ambiente `VITE_BASE_DOMAIN` para o domínio base usado no ambiente local.

## Desenvolvimento SSR e PWA

### Rodar SSR em desenvolvimento

```bash
npx quasar dev -m ssr
```

Alternativa via script npm:

```bash
npm run dev -- -m ssr
```

### Rodar SSR com PWA (integrado)

Com o `ssr.pwa = true` no `quasar.config.ts`, o modo SSR já incorpora os recursos PWA.

```bash
npx quasar dev -m ssr
```

ou

```bash
npm run dev -- -m ssr
```

### Rodar PWA em desenvolvimento

```bash
npx quasar dev -m pwa
```

Alternativa via script npm:

```bash
npm run dev -- -m pwa
```

## Build de produção

### SPA

```bash
npm run build
```

ou

```bash
npx quasar build -m spa
```

Saída: dist/spa

### PWA

```bash
npx quasar build -m pwa
```

Saída: dist/pwa

### SSR

```bash
npx quasar build -m ssr
```

Saída: dist/ssr

Quando `ssr.pwa = true`, este build SSR já inclui PWA integrado.

## Deploy em produção

### Deploy SPA/PWA (Nginx, CDN, Vercel, Netlify, S3+CloudFront)

1. Gere o build:

```bash
npx quasar build -m spa
```

ou

```bash
npx quasar build -m pwa
```

2. Publique o conteúdo da pasta dist/spa ou dist/pwa no provedor estático.
3. Configure fallback de rota para index.html (history fallback) quando aplicável, pois o app usa `vueRouterMode: 'history'`.

### Deploy SSR (Node.js)

1. Gere o build SSR:

```bash
npx quasar build -m ssr
```

2. No servidor, execute a aplicação SSR a partir da pasta gerada em dist/ssr.
3. Use processo gerenciado (PM2/systemd) e reverse proxy (Nginx/Caddy).
4. Exemplo com PM2 (ajuste o entrypoint conforme saída do seu build):

```bash
pm2 start dist/ssr/index.js --name motorise-ssr
pm2 save
```

Observação: dependendo da versão/configuração do Quasar, o entrypoint SSR pode variar. Use o arquivo de entrada indicado no log final do comando quasar build -m ssr.

## Qualidade e utilitários

### Type check

```bash
npx vue-tsc --noEmit
```

### Lint

```bash
npm run lint
```

### Format

```bash
npm run format
```

## Variáveis de ambiente úteis

- `VITE_API_URL`: base da API quando `useBackend = true`
- `VITE_BASE_DOMAIN`: domínio base da plataforma para resolução de subdomínios no `resolveTenant`

Exemplo:

```env
VITE_API_URL=https://api.motorise.com.br
VITE_BASE_DOMAIN=motorise.com.br
```

## Referências

- Quasar Config: https://v2.quasar.dev/quasar-cli-vite/quasar-config-file
- SSR: https://v2.quasar.dev/quasar-cli-vite/developing-ssr/introduction
- PWA: https://v2.quasar.dev/quasar-cli-vite/developing-pwa/introduction
