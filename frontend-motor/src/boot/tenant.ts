import { defineBoot } from '#q-app/wrappers'
import { useTenant, registerFromBackend } from 'src/composables/useTenant'
import { resolveTenant } from 'src/data/tenants'

const BASE_DOMAIN = (import.meta.env.VITE_BASE_DOMAIN as string | undefined) ?? 'motorise.com.br'

/**
 * Boot do sistema de multi-tenant.
 *
 * Resolução por prioridade:
 *   1. Subdomínio da plataforma  → slug.motorise.com.br (registro estático)
 *   2. Domínio próprio do cliente → via API /by-host (busca dinâmica)
 *   3. Fallback                  → marketplace padrão
 *
 * No SSR o hostname vem do cabeçalho `Host` da requisição HTTP.
 * No cliente, `window.location.hostname` é idêntico.
 *
 * Para domínios customizados, faz uma chamada HTTP ao backend para
 * resolver o tenant e registrar dinamicamente antes de iniciar a app.
 */
export default defineBoot(async ({ ssrContext }) => {
  const { initFromHostname } = useTenant()

  let hostname: string

  if (ssrContext) {
    const host = ssrContext.req.headers['host'] ?? ''
    hostname = host.split(':')[0] ?? ''
  } else {
    hostname = window.location.hostname
  }

  // Se já resolve localmente (subdomínio conhecido ou customDomain estático),
  // não precisa chamar API
  const local = resolveTenant(hostname)
  if (local.slug !== 'default') {
    initFromHostname(hostname)
    return
  }

  // Tenta resolver via backend — só para domínios que não são a plataforma
  const host = hostname.toLowerCase()
  if (host === BASE_DOMAIN || host === `www.${BASE_DOMAIN}` || host.endsWith(`.${BASE_DOMAIN}`)) {
    initFromHostname(hostname)
    return
  }

  // Em desenvolvimento local, não tenta resolver via API
  if (host === 'localhost' || host === '127.0.0.1' || host === '0.0.0.0') {
    initFromHostname(hostname)
    return
  }

  const apiBase = import.meta.env.VITE_API_URL || '/api'
  try {
    const res = await fetch(`${apiBase}/BR/public/tenants/by-host?host=${encodeURIComponent(hostname)}`)
    if (res.ok) {
      const data = await res.json()
      registerFromBackend(data)
    }
  } catch {
    // Falha silenciosa — fallback para default
  }

  initFromHostname(hostname)
})
