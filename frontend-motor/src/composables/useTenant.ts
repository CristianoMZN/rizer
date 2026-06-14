import { ref, computed } from 'vue'
import { resolveTenant, registerTenant, type TenantConfig, type MenuItemKey, type TenantTheme } from 'src/data/tenants'
import type { PublicTenantView } from 'src/services/api'

// ─── Singleton reativo ────────────────────────────────────────────────────────

const _tenant = ref<TenantConfig>(resolveTenant(''))

// ─── Escopo da API ────────────────────────────────────────────────────────────

/**
 * Retorna o escopo atual para a camada de API.
 * Chamado de forma síncrona, sem reatividade — serve para ser importado
 * diretamente no api.ts sem criar dependência circular com o composable.
 *
 * - marketplace → sem restrição de loja
 * - store       → { storeSlug } para filtrar recursos da loja
 */
export function getTenantScope(): { mode: 'marketplace' } | { mode: 'store'; storeSlug: string } {
  const t = _tenant.value
  if (t.mode === 'store' && t.storeSlug) {
    return { mode: 'store', storeSlug: t.storeSlug }
  }
  return { mode: 'marketplace' }
}

// ─── Register from backend ────────────────────────────────────────────────────

/**
 * Mapeia PublicTenantView (vindo do backend) para TenantConfig
 * e registra no registro estático para resolução subsequente.
 */
export function registerFromBackend(data: PublicTenantView): TenantConfig {
  const theme: TenantTheme = {
    primary: data.theme?.primary ?? '#667eea',
    secondary: data.theme?.secondary ?? '#11998e',
    accent: data.theme?.accent ?? '#764ba2',
    dark: data.theme?.dark ?? '#1a1a2e',
    darkPage: data.theme?.darkPage ?? '#0f0f1a',
  }
  const config: TenantConfig = {
    slug: data.slug,
    mode: 'store',
    storeSlug: data.slug,
    storeName: data.tradeName,
    ...(data.logoUrl ? { logoUrl: data.logoUrl } : {}),
    theme,
    footerTagline: `${data.tradeName} — Qualidade em veículos.`,
  }
  registerTenant(config)
  return config
}

// ─── Composable ───────────────────────────────────────────────────────────────

export function useTenant() {
  const initFromHostname = (hostname: string) => {
    const resolved = resolveTenant(hostname)
    _tenant.value = resolved
    _applyThemeToDom(resolved)
  }

  const isMenuVisible = (key: MenuItemKey): boolean => {
    const visible = _tenant.value.visibleMenuItems
    if (!visible) return true
    return visible.includes(key)
  }

  const isStoreTenant = computed(() => _tenant.value.mode === 'store')
  const storeName = computed(() => _tenant.value.storeName)
  const logoUrl = computed(() => _tenant.value.logoUrl)
  const footerTagline = computed(() => _tenant.value.footerTagline)
  // `tenant` é exposto como ref mutável para permitir override em runtime
  // (ex.: página de parceiro injeta tema vindo do backend).
  const tenant = _tenant

  return {
    tenant,
    isStoreTenant,
    storeName,
    logoUrl,
    footerTagline,
    isMenuVisible,
    initFromHostname,
  }
}

// ─── Aplicar tema no DOM ──────────────────────────────────────────────────────

function _applyThemeToDom(config: TenantConfig) {
  if (typeof document === 'undefined') return

  const { primary, secondary, accent, dark, darkPage } = config.theme
  const root = document.documentElement

  root.style.setProperty('--q-primary', primary)
  root.style.setProperty('--q-secondary', secondary)
  root.style.setProperty('--q-accent', accent)
  root.style.setProperty('--q-dark', dark)
  root.style.setProperty('--q-dark-page', darkPage)

  root.style.setProperty(
    '--gradient-primary',
    `linear-gradient(135deg, ${primary} 0%, ${accent} 100%)`,
  )
}
