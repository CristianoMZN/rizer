// ─── Multi-Tenant System ──────────────────────────────────────────────────────
//
// O tenant é resolvido a partir do subdomínio do Host header da requisição.
// Exemplo: "joao.motorise.com.br"  →  slug "joao"  →  TenantConfig
//
// Quando o subdomínio não existe no registro, usa-se o tenant "default" (plataforma Motorise).

// ─── Menu Items ───────────────────────────────────────────────────────────────

export type MenuItemKey =
  | 'home'
  | 'vehicles'
  | 'partners'
  | 'compare'
  | 'favorites'
  | 'partner-cta'
  | 'register'
  | 'ads'
  | 'leads'

// ─── Theme ────────────────────────────────────────────────────────────────────

export interface TenantTheme {
  /** Cor primária (botões, destaques) */
  primary: string
  /** Cor secundária */
  secondary: string
  /** Cor de acento */
  accent: string
  /** Cor escura principal (componentes dark) */
  dark: string
  /** Cor de fundo em modo escuro */
  darkPage: string
}

// ─── Tenant ───────────────────────────────────────────────────────────────────

/**
 * Modo de operação do tenant.
 *
 * - `marketplace`: plataforma aberta — exibe anúncios de todas as lojas.
 * - `store`:       white-label — escopo restrito à loja cadastrada (`storeSlug`).
 */
export type TenantMode = 'marketplace' | 'store'

export interface TenantConfig {
  slug: string
  mode: TenantMode
  /**
   * Slug da loja no backend/mock. Obrigatório quando mode = 'store'.
   * Usado para filtrar veículos, leads e outros recursos ao domínio da loja.
   */
  storeSlug?: string
  storeName: string
  logoUrl?: string
  faviconUrl?: string
  /**
   * Domínio próprio do cliente (ex: "top-motos-rj.com.br").
   * Quando preenchido, o tenant é resolvido também por esse domínio via CNAME.
   * O cliente configura: top-motos-rj.com.br → CNAME → slug.motorise.com.br
   */
  customDomain?: string
  theme: TenantTheme
  /**
   * Itens visíveis no menu/drawer.
   * Se undefined → mostra todos (comportamento da plataforma padrão).
   * Se definido → apenas os itens listados aparecem.
   */
  visibleMenuItems?: MenuItemKey[]
  footerTagline: string
}

// ─── Default (Plataforma Motorise) ───────────────────────────────────────────

const DEFAULT_THEME: TenantTheme = {
  primary: '#667eea',
  secondary: '#11998e',
  accent: '#764ba2',
  dark: '#1a1a2e',
  darkPage: '#0f0f1a',
}

// ─── Registro de Tenants ─────────────────────────────────────────────────────
//
// Adicione novos tenants aqui. A chave é o subdomínio.

export const TENANT_REGISTRY: Record<string, TenantConfig> = {
  // ── Plataforma base (sem subdomínio reconhecido) ──
  default: {
    slug: 'default',
    mode: 'marketplace',
    storeName: 'Motorise',
    theme: DEFAULT_THEME,
    // undefined → todos os itens visíveis
    footerTagline: 'O marketplace de veículos mais completo do Brasil.',
  },

  // ── Exemplo: João Veículos (joao.motorise.com.br) ──
  joao: {
    slug: 'joao',
    mode: 'store',
    storeSlug: 'motorise-sp', // slug da Store no backend/mock
    storeName: 'João Veículos',
    // customDomain: 'joaoveiculos.com.br', // exemplo quando cliente cadastrar domínio próprio
    theme: {
      primary: '#e63946',
      secondary: '#457b9d',
      accent: '#1d3557',
      dark: '#1d3557',
      darkPage: '#0d1b2a',
    },
    visibleMenuItems: ['home', 'vehicles', 'compare', 'favorites'],
    footerTagline: 'Os melhores veículos esperando por você.',
  },

  // ── Exemplo: Maria Motos (maria.motorise.com.br / top-motos-rj.com.br) ──
  maria: {
    slug: 'maria',
    mode: 'store',
    storeSlug: 'top-motos-rj', // slug da Store no backend/mock
    storeName: 'Maria Motos',
    customDomain: 'top-motos-rj.com.br', // domínio próprio, com CNAME apontando para maria.motorise.com.br
    theme: {
      primary: '#f77f00',
      secondary: '#d62828',
      accent: '#fcbf49',
      dark: '#2d1b00',
      darkPage: '#1a1000',
    },
    visibleMenuItems: ['home', 'vehicles', 'compare', 'favorites', 'leads', 'ads'],
    footerTagline: 'Paixão por motos desde 2010.',
  },
}

// ─── Resolver ────────────────────────────────────────────────────────────────

/**
 * Domínio base da plataforma. Usado para identificar subdomínios no formato
 * slug.motorise.com.br. Configurável via variável de ambiente VITE_BASE_DOMAIN.
 */
const BASE_DOMAIN = (import.meta.env?.VITE_BASE_DOMAIN as string | undefined) ?? 'motorise.com.br'

/**
 * Mapa reverso: customDomain → TenantConfig.
 * Construído uma única vez em módulo para lookup O(1).
 * Atualizado automaticamente se o TENANT_REGISTRY for modificado em runtime.
 */
const CUSTOM_DOMAIN_MAP: Map<string, TenantConfig> = new Map(
  Object.values(TENANT_REGISTRY)
    .filter((t): t is TenantConfig & { customDomain: string } => !!t.customDomain)
    .map((t) => [t.customDomain.toLowerCase(), t]),
)

/**
 * Resolve o TenantConfig a partir do hostname da requisição.
 *
 * Estratégia de resolução (em ordem de prioridade):
 *
 * 1. Subdomínio da plataforma — `slug.motorise.com.br`
 *    Remove o sufixo BASE_DOMAIN, extrai o subdomínio restante e busca no registro.
 *
 * 2. Domínio próprio do cliente — `top-motos-rj.com.br`
 *    Busca no mapa reverso CUSTOM_DOMAIN_MAP (construído a partir de `customDomain`).
 *    Funciona para qualquer domínio apontado via CNAME para o subdomínio da plataforma.
 *
 * 3. Fallback — retorna o tenant `default` (marketplace aberto).
 */
export function resolveTenant(hostname: string): TenantConfig {
  // Remove a porta caso presente (ex: "localhost:9000" → "localhost")
  const host = hostname.split(':')[0]?.toLowerCase() ?? ''

  // ── 1. Subdomínio da plataforma ──────────────────────────────────────────
  // Verifica se o host termina com ".<BASE_DOMAIN>" (ex: "joao.motorise.com.br")
  const subdomainSuffix = `.${BASE_DOMAIN}`
  if (host.endsWith(subdomainSuffix)) {
    const subdomain = host.slice(0, host.length - subdomainSuffix.length)
    // Garante que é um nível só (evita "a.b.motorise.com.br")
    if (subdomain && !subdomain.includes('.')) {
      const tenant = TENANT_REGISTRY[subdomain]
      if (tenant) return tenant
    }
  }

  // ── 2. Domínio próprio do cliente (via CNAME) ────────────────────────────
  const byCustomDomain = CUSTOM_DOMAIN_MAP.get(host)
  if (byCustomDomain) return byCustomDomain

  // ── 3. Fallback: marketplace padrão ─────────────────────────────────────
  return TENANT_REGISTRY['default']!
}

/**
 * Registra um tenant em runtime (ex: após o lojista salvar as configurações)
 * e atualiza o mapa de domínios customizados.
 * Útil quando os tenants são carregados dinamicamente do backend em vez de
 * estar hard-coded neste arquivo.
 */
export function registerTenant(config: TenantConfig): void {
  TENANT_REGISTRY[config.slug] = config
  if (config.customDomain) {
    CUSTOM_DOMAIN_MAP.set(config.customDomain.toLowerCase(), config)
  }
}
