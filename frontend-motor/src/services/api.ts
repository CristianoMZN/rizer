// ─── API Real (Axios) ─────────────────────────────────────────────────────────
// Clientes tipados para os endpoints do backend. Quando o backend não
// está acessível, o caller trata o erro. Mantemos isto isolado dos
// mocks de `api` (legado), para que a UI possa alternar via MOCK_CONFIG.

import axios, { type AxiosInstance } from 'axios'

export const MOCK_CONFIG = {
  useBackend: false,
  apiBase: import.meta.env.VITE_API_URL || '/api',
  delay: 300,
}

const http: AxiosInstance = axios.create({
  baseURL: MOCK_CONFIG.apiBase,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  if (typeof document !== 'undefined' && config.headers) {
    const match = document.cookie.match(/(?:^|; )motorise_access_token=([^;]*)/)
    if (match && match[1]) {
      config.headers['Authorization'] = `Bearer ${decodeURIComponent(match[1])}`
    }
  }
  return config
})

export type TenantStatus = 'pending' | 'active' | 'paused' | 'suspended' | 'canceled'
export type TenantUserRole = 'OWNER' | 'MANAGER' | 'SELLER'

export interface TenantView {
  id: string
  slug: string
  countryCode: string
  tradeName: string
  legalName?: string
  cnpj?: string
  description?: string
  logoUrl?: string
  bannerUrl?: string
  phone?: string
  whatsapp?: string
  email?: string
  website?: string
  status: TenantStatus
  isPublic: boolean
  isPartnerPageEnabled: boolean
  hadTrial: boolean
  customDomain?: string
  customDomainStatus?: 'NONE' | 'PENDING' | 'VERIFIED' | 'FAILED'
  ownerUserId?: string
  ownerEmail?: string
  ownerName?: string
  activeStoresCount: number
  membersCount: number
  stores: StoreView[]
  createdAt: string
  updatedAt: string
}

export interface StoreView {
  id: string
  tenantId: string
  name: string
  slug: string
  phone?: string
  whatsapp?: string
  email?: string
  isMain: boolean
  isActive: boolean
  latitude?: number
  longitude?: number
  createdAt: string
  updatedAt: string
}

export interface MemberView {
  id: string
  tenantId: string
  tenantSlug?: string
  tenantName?: string
  userId: string
  name?: string
  email?: string
  role: TenantUserRole
  physicalStoreIds: string[]
  isActive: boolean
  acceptedAt?: string
  expireAt?: string
}

export interface CreateTenantRequest {
  slug: string
  tradeName: string
  legalName?: string
  cnpj?: string
  countryCode: string
  description?: string
  phone?: string
  whatsapp?: string
  email?: string
  website?: string
  ownerName: string
  ownerEmail: string
  ownerPhone?: string
  ownerPassword: string
  startWithTrial: boolean
}

export interface CreateStoreRequest {
  tenantId?: string
  name: string
  slug?: string
  phone?: string
  whatsapp?: string
  email?: string
  isMain?: boolean
  latitude?: number
  longitude?: number
}

export interface UpdateStoreRequest {
  name?: string
  phone?: string
  whatsapp?: string
  email?: string
  isMain?: boolean
  isActive?: boolean
  latitude?: number
  longitude?: number
}

export interface InviteMemberRequest {
  tenantId?: string
  email: string
  name: string
  role: TenantUserRole
  physicalStoreIds?: string[]
}

// ─── Admin (sys_admin) ───────────────────────────────────────────────────────

export const adminApi = {
  async listTenants(): Promise<TenantView[]> {
    const res = await http.get<TenantView[]>('/admin/tenants')
    return res.data
  },
  async getTenant(id: string): Promise<TenantView> {
    const res = await http.get<TenantView>(`/admin/tenants/${id}`)
    return res.data
  },
  async createTenant(req: CreateTenantRequest): Promise<TenantView> {
    const res = await http.post<TenantView>('/admin/tenants', req)
    return res.data
  },
  async updateTenant(id: string, patch: Partial<CreateTenantRequest>): Promise<TenantView> {
    const res = await http.patch<TenantView>(`/admin/tenants/${id}`, patch)
    return res.data
  },
  async deleteTenant(id: string): Promise<void> {
    await http.delete(`/admin/tenants/${id}`)
  },
}

// ─── Tenant (autenticado) ────────────────────────────────────────────────────

export const tenantApi = {
  async listStores(): Promise<StoreView[]> {
    const res = await http.get<StoreView[]>('/tenant/stores')
    return res.data
  },
  async createStore(req: CreateStoreRequest): Promise<StoreView> {
    const res = await http.post<StoreView>('/tenant/stores', req)
    return res.data
  },
  async updateStore(id: string, patch: UpdateStoreRequest): Promise<StoreView> {
    const res = await http.patch<StoreView>(`/tenant/stores/${id}`, patch)
    return res.data
  },
  async deleteStore(id: string): Promise<void> {
    await http.delete(`/tenant/stores/${id}`)
  },

  async listMembers(): Promise<MemberView[]> {
    const res = await http.get<MemberView[]>('/tenant/members')
    return res.data
  },
  async inviteMember(req: InviteMemberRequest): Promise<MemberView> {
    const res = await http.post<MemberView>('/tenant/members', req)
    return res.data
  },
  async updateMember(id: string, patch: { role?: TenantUserRole; physicalStoreIds?: string[] }): Promise<MemberView> {
    const res = await http.patch<MemberView>(`/tenant/members/${id}`, patch)
    return res.data
  },
  async removeMember(id: string): Promise<void> {
    await http.delete(`/tenant/members/${id}`)
  },
}

export { http }
