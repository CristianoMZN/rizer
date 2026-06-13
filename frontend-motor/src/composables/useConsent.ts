import { ref, readonly } from 'vue'
import { TERMS_VERSION, PRIVACY_VERSION, COOKIES_VERSION } from 'src/data/legalVersions'
import { MOCK_CONFIG } from 'src/services/api'
import { lgpdApi, type ConsentPurpose } from 'src/services/api'

// ─── Estado singleton ───────────────────────────────────────────────────────

const COOKIE_NAME = 'motorise_consent'
const ANON_ID_COOKIE = 'motorise_anon_id'

const visible = ref(false)
const configOpen = ref(false)
const consents = ref<Record<string, boolean>>({})
const loaded = ref(false)

// ─── Helpers ────────────────────────────────────────────────────────────────

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match && match[1] ? decodeURIComponent(match[1]) : null
}

function writeCookie(name: string, value: string, days: number) {
  if (typeof document === 'undefined') return
  document.cookie = `${name}=${encodeURIComponent(value)}; Path=/; SameSite=Lax; Max-Age=${days * 86400}`
}

function ensureAnonId(): string {
  let id = readCookie(ANON_ID_COOKIE)
  if (!id) {
    id = crypto.randomUUID?.() ?? Math.random().toString(36).slice(2)
    writeCookie(ANON_ID_COOKIE, id, 365)
  }
  return id
}

function readStored(): Record<string, { v: boolean; version: string }> | null {
  const raw = readCookie(COOKIE_NAME)
  if (!raw) return null
  try { return JSON.parse(raw) } catch { return null }
}

function persist(map: Record<string, { v: boolean; version: string }>) {
  writeCookie(COOKIE_NAME, JSON.stringify(map), 365)
}

function versionsMatch(stored: { version: string } | undefined, current: string): boolean {
  return !!stored && stored.version === current
}

// ─── Composable ──────────────────────────────────────────────────────────────

export function useConsent() {

  function init() {
    if (loaded.value) return
    loaded.value = true
    const stored = readStored()
    const allOk =
      stored &&
      versionsMatch(stored['terms_of_use'], TERMS_VERSION) &&
      versionsMatch(stored['privacy_policy'], PRIVACY_VERSION) &&
      versionsMatch(stored['cookies_essential'], COOKIES_VERSION) &&
      (stored['cookies_essential']?.v ?? false)
    if (allOk && stored) {
      // Restaura preferências para o banner de configurar reabrir
      consents.value = {
        terms_of_use: stored['terms_of_use']?.v ?? false,
        privacy_policy: stored['privacy_policy']?.v ?? false,
        cookies_essential: stored['cookies_essential']?.v ?? false,
        cookies_analytics: stored['cookies_analytics']?.v ?? false,
        cookies_marketing: stored['cookies_marketing']?.v ?? false,
        marketing_emails: stored['marketing_emails']?.v ?? false,
        data_sharing_integrations: stored['data_sharing_integrations']?.v ?? false,
      }
    } else {
      visible.value = true
    }
  }

  function acceptAll() {
    const map: Record<string, { v: boolean; version: string }> = {
      terms_of_use: { v: true, version: TERMS_VERSION },
      privacy_policy: { v: true, version: PRIVACY_VERSION },
      cookies_essential: { v: true, version: COOKIES_VERSION },
      cookies_analytics: { v: true, version: COOKIES_VERSION },
      cookies_marketing: { v: true, version: COOKIES_VERSION },
      marketing_emails: { v: true, version: PRIVACY_VERSION },
      data_sharing_integrations: { v: true, version: PRIVACY_VERSION },
    }
    persist(map)
    visible.value = false
    configOpen.value = false
    void recordToServer(map)
  }

  function rejectNonEssential() {
    const map: Record<string, { v: boolean; version: string }> = {
      terms_of_use: { v: true, version: TERMS_VERSION },
      privacy_policy: { v: true, version: PRIVACY_VERSION },
      cookies_essential: { v: true, version: COOKIES_VERSION },
      cookies_analytics: { v: false, version: COOKIES_VERSION },
      cookies_marketing: { v: false, version: COOKIES_VERSION },
      marketing_emails: { v: false, version: PRIVACY_VERSION },
      data_sharing_integrations: { v: false, version: PRIVACY_VERSION },
    }
    persist(map)
    visible.value = false
    configOpen.value = false
    void recordToServer(map)
  }

  function saveCustom(map: Record<string, boolean>) {
    const stored: Record<string, { v: boolean; version: string }> = {}
    for (const [k, v] of Object.entries(map)) {
      const ver = versionOf(k as ConsentPurpose)
      stored[k] = { v, version: ver }
    }
    persist(stored)
    consents.value = map
    configOpen.value = false
    visible.value = false
    void recordToServer(stored)
  }

  function versionOf(purpose: ConsentPurpose): string {
    if (purpose === 'terms_of_use') return TERMS_VERSION
    if (purpose === 'privacy_policy') return PRIVACY_VERSION
    return COOKIES_VERSION
  }

  async function recordToServer(stored: Record<string, { v: boolean; version: string }>) {
    if (!MOCK_CONFIG.useBackend) return
    try {
      // Para usuários logados, podemos enviar para /me/consents.
      // Visitantes anônimos não persistem (LGPD art. 7° — consentimento inequívoco).
      const hasUser = !!readCookie('motorise_access') // heurística simples
      if (!hasUser) return
      for (const [purpose, entry] of Object.entries(stored)) {
        await lgpdApi.recordConsent(purpose as ConsentPurpose, entry.v, entry.version)
      }
    } catch { /* silencioso: o cookie local é a fonte de verdade para o consent */ }
  }

  return {
    visible: readonly(visible),
    configOpen: readonly(configOpen),
    consents: readonly(consents),
    init,
    acceptAll,
    rejectNonEssential,
    saveCustom,
    openConfig: () => { configOpen.value = true },
    closeConfig: () => { configOpen.value = false },
    ensureAnonId,
  }
}
