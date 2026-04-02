import { defineBoot } from '#q-app/wrappers'
import { useTenant } from 'src/composables/useTenant'

/**
 * Boot do sistema de multi-tenant.
 *
 * Resolução por prioridade (ver resolveTenant em tenants.ts):
 *   1. Subdomínio da plataforma  → slug.motorise.com.br
 *   2. Domínio próprio do cliente → top-motos-rj.com.br (via CNAME)
 *   3. Fallback                  → marketplace padrão
 *
 * No SSR o hostname vem do cabeçalho `Host` da requisição HTTP —
 * isso garante que o servidor já entrega o HTML com o tema correto,
 * sem flash de conteúdo após hidratação.
 *
 * No cliente, `window.location.hostname` é idêntico ao que o servidor viu,
 * portanto a hidratação é consistente sem mismatch de SSR.
 *
 * Tenants dinâmicos (vindos do backend) podem ser registrados em runtime
 * com `registerTenant()` antes de chamar `initFromHostname()`.
 */
export default defineBoot(({ ssrContext }) => {
  const { initFromHostname } = useTenant()

  let hostname: string

  if (ssrContext) {
    // Server side: extrai do Host header HTTP (sem a porta)
    const host = ssrContext.req.headers['host'] ?? ''
    hostname = host.split(':')[0] ?? ''
  } else {
    // Client side: lê do browser
    hostname = window.location.hostname
  }

  initFromHostname(hostname)
})
