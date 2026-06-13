import { ref, computed } from 'vue'
import axios, { type AxiosInstance } from 'axios'
import { MOCK_CONFIG } from 'src/services/api'

// ─── Tipos públicos ────────────────────────────────────────────────────────────

export type SystemRole =
  | 'sys_admin'
  | 'sys_manager'
  | 'sys_employee'
  | 'agency_owner'
  | 'agency_admin'
  | 'agency_employee'
  | 'user'

export interface TenantMembership {
  tenantId: string
  tenantSlug?: string
  tenantName?: string
  role: 'OWNER' | 'MANAGER' | 'SELLER'
  isOwner: boolean
  isManagerOrOwner: boolean
}

export interface AuthUser {
  id: string
  email: string
  name: string
  avatarUrl?: string
  systemRole: SystemRole
  phone?: string
  memberships: TenantMembership[]
  currentTenantId?: string
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: AuthUser
}

// ─── Singleton reativo ────────────────────────────────────────────────────────

const user = ref<AuthUser | null>(null)
const accessToken = ref<string | null>(null)
const initialized = ref(false)

function readCookie(name: string): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match && match[1] ? decodeURIComponent(match[1]) : null
}

// Instância axios dedicada para chamadas de auth (sempre com credentials).
// Não depende do boot/axios para evitar ciclo de imports.
const http: AxiosInstance = axios.create({
  baseURL: MOCK_CONFIG.apiBase,
  withCredentials: true,
})

let _bootApi: AxiosInstance | null = null
export function bindBootApi(api: AxiosInstance) {
  _bootApi = api
}
void _bootApi // reservado para uso futuro em outros stores que dependam da instância boot

// ─── Composable ───────────────────────────────────────────────────────────────

export function useAuthStore() {
  const isAuthenticated = computed(() => user.value !== null)
  const isPlatformAdmin = computed(() => {
    const r = user.value?.systemRole
    return r === 'sys_admin' || r === 'sys_manager' || r === 'sys_employee'
  })
  const currentTenantId = computed(() => user.value?.currentTenantId ?? null)
  const currentMembership = computed<TenantMembership | null>(() => {
    const tid = currentTenantId.value
    if (!tid || !user.value) return null
    return user.value.memberships.find((m) => m.tenantId === tid) ?? null
  })

  async function login(email: string, password: string): Promise<AuthUser> {
    if (!MOCK_CONFIG.useBackend) {
      throw new Error('Backend desabilitado (MOCK_CONFIG.useBackend = false)')
    }
    const res = await http.post<LoginResult>('/auth/login', { email, password })
    applyLogin(res.data)
    return res.data.user
  }

  async function loginWithGoogle(): Promise<void> {
    if (!MOCK_CONFIG.useBackend) {
      throw new Error('Backend desabilitado (MOCK_CONFIG.useBackend = false)')
    }
    if (typeof window === 'undefined') {
      await Promise.resolve()
      return
    }
    window.location.href = `${MOCK_CONFIG.apiBase}/oauth2/authorization/google`
    await Promise.resolve()
  }

  async function refreshMe(): Promise<AuthUser | null> {
    if (!MOCK_CONFIG.useBackend) {
      initialized.value = true
      return null
    }
    try {
      const res = await http.get<AuthUser>('/auth/me')
      user.value = res.data
      initialized.value = true
      return res.data
    } catch {
      user.value = null
      initialized.value = true
      return null
    }
  }

  async function logout(): Promise<void> {
    if (MOCK_CONFIG.useBackend) {
      try { await http.post('/auth/logout') } catch { /* ignore */ }
    }
    user.value = null
    accessToken.value = null
  }

  async function switchTenant(tenantId: string): Promise<void> {
    if (!MOCK_CONFIG.useBackend) return
    const res = await http.post<LoginResult>('/auth/switch-tenant', { tenantId })
    applyLogin(res.data)
  }

  function getAccessToken(): string | null {
    return accessToken.value ?? readCookie('motorise_access')
  }

  function applyLogin(result: LoginResult) {
    accessToken.value = result.accessToken
    user.value = result.user
    initialized.value = true
  }

  return {
    user,
    accessToken,
    initialized,
    isAuthenticated,
    isPlatformAdmin,
    currentTenantId,
    currentMembership,
    login,
    loginWithGoogle,
    refreshMe,
    logout,
    switchTenant,
    getAccessToken,
  }
}
