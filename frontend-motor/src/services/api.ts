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

// ─── Catálogo / Produtos ────────────────────────────────────────────────────

export type VehicleRealm = 'CAR' | 'MOTORCYCLE' | 'TRUCK' | 'NAUTICAL' | 'BUS'
export type ProductStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE' | 'ARCHIVED' | 'SOLD'

export interface CategoryView {
  id: string
  countryCode: string
  realm: VehicleRealm
  path: string
  name: string
  slug: string
  parentId?: string
  level: number
  sortOrder: number
  icon?: string
  imageUrl?: string
  description?: string
}

export interface VehicleBrandView {
  id: number
  vehicleType: string
  fipeId: number
  name: string
}

export interface VehicleModelView {
  id: number
  brandId: number
  fipeId: number
  name: string
}

export interface ProductImageView {
  id: string
  url: string
  contentType?: string
  sortOrder: number
  isCover: boolean
}

export interface ProductView {
  id: string
  tenantId: string
  physicalStoreId: string
  physicalStoreName?: string
  categoryId: string
  categoryName?: string
  brandId?: number
  brandName?: string
  modelId?: number
  modelName?: string
  realm: VehicleRealm
  yearModel?: number
  yearBuild?: number
  mileageKm?: number
  fuel?: string
  transmission?: string
  attributes: Record<string, unknown>
  status: ProductStatus
  title?: string
  description?: string
  price: number
  currency: string
  latitude?: number
  longitude?: number
  locationSource: 'STORE' | 'CUSTOM'
  images: ProductImageView[]
  createdAt: string
  updatedAt: string
}

export interface CreateProductRequest {
  physicalStoreId: string
  categoryId: string
  brandId?: number
  modelId?: number
  title: string
  description?: string
  price: number
  currency: string
  countryCode?: string
  yearModel?: number
  yearBuild?: number
  mileageKm?: number
  fuel?: string
  transmission?: string
  attributes?: Record<string, unknown>
  publish?: boolean
}

export interface UpdateProductRequest {
  physicalStoreId?: string
  categoryId?: string
  brandId?: number
  modelId?: number
  title?: string
  description?: string
  price?: number
  currency?: string
  yearModel?: number
  yearBuild?: number
  mileageKm?: number
  fuel?: string
  transmission?: string
  attributes?: Record<string, unknown>
  status?: ProductStatus
}

export interface UploadResponse {
  image: ProductImageView
  publicUrl: string
  s3Key: string
}

export const catalogApi = {
  async listCategories(countryCode = 'BR'): Promise<CategoryView[]> {
    const res = await http.get<CategoryView[]>(`/${countryCode}/public/categories`)
    return res.data
  },
  async listSubtypes(countryCode: string, realm: VehicleRealm): Promise<CategoryView[]> {
    const res = await http.get<CategoryView[]>(`/${countryCode}/public/categories/${realm}/subtypes`)
    return res.data
  },
  async listBrands(realm: VehicleRealm): Promise<VehicleBrandView[]> {
    const res = await http.get<VehicleBrandView[]>('/BR/public/brands', { params: { realm } })
    return res.data
  },
  async listModels(brandId: number): Promise<VehicleModelView[]> {
    const res = await http.get<VehicleModelView[]>(`/BR/public/brands/${brandId}/models`)
    return res.data
  },
  async searchProducts(params: {
    countryCode?: string
    tenantId?: string
    realm?: VehicleRealm
    categoryId?: string
    brandId?: number
    minYear?: number
    maxYear?: number
    limit?: number
    offset?: number
  } = {}): Promise<ProductView[]> {
    const cc = params.countryCode ?? 'BR'
    const res = await http.get<ProductView[]>(`/${cc}/public/products`, { params })
    return res.data
  },
}

export const tenantProductApi = {
  async list(): Promise<ProductView[]> {
    const res = await http.get<ProductView[]>('/tenant/products')
    return res.data
  },
  async get(id: string): Promise<ProductView> {
    const res = await http.get<ProductView>(`/tenant/products/${id}`)
    return res.data
  },
  async create(req: CreateProductRequest): Promise<ProductView> {
    const res = await http.post<ProductView>('/tenant/products', req)
    return res.data
  },
  async update(id: string, patch: UpdateProductRequest): Promise<ProductView> {
    const res = await http.patch<ProductView>(`/tenant/products/${id}`, patch)
    return res.data
  },
  async delete(id: string): Promise<void> {
    await http.delete(`/tenant/products/${id}`)
  },
  async uploadImage(id: string, file: File, isCover?: boolean): Promise<UploadResponse> {
    const fd = new FormData()
    fd.append('file', file)
    if (isCover) fd.append('isCover', 'true')
    const res = await http.post<UploadResponse>(`/tenant/products/${id}/images/upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },
  async listImages(id: string): Promise<ProductImageView[]> {
    const res = await http.get<ProductImageView[]>(`/tenant/products/${id}/images`)
    return res.data
  },
  async deleteImage(productId: string, imageId: string): Promise<void> {
    await http.delete(`/tenant/products/${productId}/images/${imageId}`)
  },
}

// ─── Settings (perfil do tenant + custom domain) ────────────────────────────

export type CustomDomainStatus = 'NONE' | 'PENDING' | 'VERIFIED' | 'FAILED'

export interface TenantSettingsView {
  id: string
  slug: string
  countryCode: string
  tradeName: string
  legalName?: string
  description?: string
  phone?: string
  whatsapp?: string
  email?: string
  website?: string
  logoUrl?: string
  bannerUrl?: string
  cnpj?: string
  status: string
  isPublic: boolean
  isPartnerPageEnabled: boolean
  hadTrial: boolean
  customDomain?: string
  customDomainStatus: CustomDomainStatus
  customDomainError?: string
  customDomainLastCheckAt?: string
  theme: Record<string, string>
}

export interface CustomDomainCheck {
  id: string
  tenantId: string
  domain: string
  expectedTarget: string
  cnameFound?: string
  resolvedIp?: string
  status: string
  errorMessage?: string
  checkedAt: string
}

export interface CustomDomainView {
  domain?: string
  status: string
  lastCheckAt?: string
  lastError?: string
  expectedCname: string
}

export interface UpdateProfileRequest {
  tradeName?: string
  legalName?: string
  description?: string
  phone?: string
  whatsapp?: string
  email?: string
  website?: string
  logoUrl?: string
  bannerUrl?: string
}

export const settingsApi = {
  async get(): Promise<TenantSettingsView> {
    const res = await http.get<TenantSettingsView>('/tenant/settings')
    return res.data
  },
  async updateProfile(req: UpdateProfileRequest): Promise<TenantSettingsView> {
    const res = await http.post<TenantSettingsView>('/tenant/settings/profile', req)
    return res.data
  },
  async getCustomDomain(): Promise<CustomDomainView> {
    const res = await http.get<CustomDomainView>('/tenant/settings/custom-domain')
    return res.data
  },
  async setCustomDomain(domain: string): Promise<CustomDomainView> {
    const res = await http.post<CustomDomainView>('/tenant/settings/custom-domain', { domain })
    return res.data
  },
  async verifyCustomDomain(): Promise<CustomDomainCheck> {
    const res = await http.post<CustomDomainCheck>('/tenant/settings/custom-domain/verify')
    return res.data
  },
  async customDomainHistory(): Promise<CustomDomainCheck[]> {
    const res = await http.get<CustomDomainCheck[]>('/tenant/settings/custom-domain/history')
    return res.data
  },
}

// ─── Integrações de marketing ────────────────────────────────────────────────

export type IntegrationProvider = 'INSTAGRAM' | 'META_BUSINESS' | 'GOOGLE_MERCHANT'
export type IntegrationStatus = 'CONNECTED' | 'REFRESHING' | 'EXPIRED' | 'REVOKED' | 'ERROR'

export interface IntegrationView {
  id: string
  provider: IntegrationProvider
  status: string
  externalAccountId?: string
  externalAccountName?: string
  tokenValid: boolean
  tokenExpiresAt?: string
  lastSyncAt?: string
  lastError?: string
  scopes: string[]
}

export interface AuthorizeResponse {
  authorizeUrl: string
  state?: string
}

export const integrationApi = {
  async list(): Promise<IntegrationView[]> {
    const res = await http.get<IntegrationView[]>('/tenant/integrations')
    return res.data
  },
  async authorize(provider: IntegrationProvider): Promise<AuthorizeResponse> {
    const res = await http.get<AuthorizeResponse>(`/tenant/integrations/${provider}/authorize`)
    return res.data
  },
  async callback(provider: IntegrationProvider, code: string, state: string): Promise<IntegrationView> {
    const res = await http.post<IntegrationView>(`/tenant/integrations/${provider}/callback`, { code, state })
    return res.data
  },
  async disconnect(provider: IntegrationProvider): Promise<void> {
    await http.delete(`/tenant/integrations/${provider}`)
  },
  async publishToInstagram(productId: string): Promise<{ mediaId: string }> {
    const res = await http.post<{ mediaId: string }>(`/tenant/integrations/instagram/publish/${productId}`)
    return res.data
  },
}

// ─── Billing ─────────────────────────────────────────────────────────────────

export type SubscriptionStatus =
  | 'trialing' | 'active' | 'past_due' | 'paused' | 'unpaid'
  | 'canceled' | 'incomplete' | 'incomplete_expired'

export type PaymentMethod =
  | 'stripe_card' | 'stripe_pix' | 'stripe_boleto'
  | 'manual_cash' | 'manual_bank_transfer' | 'manual_pix_external'
  | 'manual_bonus' | 'manual_courtesy' | 'manual_other'

export type PaymentStatus =
  | 'pending' | 'succeeded' | 'failed' | 'refunded' | 'voided' | 'chargeback'

export interface PlanView {
  code: string
  name: string
  description?: string
  maxPhysicalStores: number | null
  hasPartnerPage: boolean
  hasCustomDomain: boolean
  hasInstagram: boolean
  hasMetaDpa: boolean
  hasGoogleShopping: boolean
  price: number
  currency: string
  trialDays: number
  sortOrder: number
}

export interface SubscriptionView {
  id: string
  tenantId: string
  planCode: string
  planName: string
  status: SubscriptionStatus
  source: string
  price: number
  currency: string
  currentPeriodStart: string
  currentPeriodEnd: string
  trialStart?: string
  trialEnd?: string
  cancelAtPeriodEnd: boolean
  canceledAt?: string
  trialDaysRemaining: number | null
  daysUntilPeriodEnd: number | null
  isInGracePeriod: boolean
  stripeCustomerId?: string
  notes?: string
}

export interface CheckoutResponse {
  checkoutUrl: string
  sessionId: string
  usingManual: boolean
}

export interface PortalResponse {
  portalUrl: string
}

export interface PaymentView {
  id: string
  tenantId: string
  tenantName?: string
  subscriptionId?: string
  method: PaymentMethod
  status: PaymentStatus
  amount: number
  currency: string
  periodStart?: string
  periodEnd?: string
  description?: string
  externalReference?: string
  receiptUrl?: string
  paidAt?: string
  recordedByEmail?: string
  notes?: string
}

export interface ManualPaymentRequest {
  tenantId?: string
  amount: number
  currency: string
  method: PaymentMethod
  paidAt: string
  periodStart?: string
  periodEnd?: string
  description?: string
  externalReference?: string
  receiptUrl?: string
  notes?: string
  newPlanCode?: string
}

export interface AdminPaymentStats {
  activeTenants: number
  trialingTenants: number
  pastDueTenants: number
  mrrCents: number
  mrr: number
  currency: string
  succeededLast30d: number
  revenueLast30d: number
}

export const billingApi = {
  async listPlans(): Promise<PlanView[]> {
    const res = await http.get<PlanView[]>('/tenant/billing/plans')
    return res.data
  },
  async listPublicPlans(countryCode = 'BR'): Promise<PlanView[]> {
    const res = await http.get<PlanView[]>(`/${countryCode}/public/billing/plans`)
    return res.data
  },
  async getSubscription(): Promise<SubscriptionView> {
    const res = await http.get<SubscriptionView>('/tenant/billing/subscription')
    return res.data
  },
  async checkout(planCode: string): Promise<CheckoutResponse> {
    const res = await http.post<CheckoutResponse>(`/tenant/billing/checkout/${planCode}`)
    return res.data
  },
  async portal(): Promise<PortalResponse> {
    const res = await http.post<PortalResponse>('/tenant/billing/portal')
    return res.data
  },
  async cancel(): Promise<SubscriptionView> {
    const res = await http.post<SubscriptionView>('/tenant/billing/cancel')
    return res.data
  },
  async resume(): Promise<SubscriptionView> {
    const res = await http.post<SubscriptionView>('/tenant/billing/resume')
    return res.data
  },
  async startTrial(planCode: string): Promise<SubscriptionView> {
    const res = await http.post<SubscriptionView>(`/tenant/billing/trial/${planCode}`)
    return res.data
  },
  async listMyPayments(page = 0, size = 20): Promise<{ content: PaymentView[]; totalElements: number }> {
    const res = await http.get<{ content: PaymentView[]; totalElements: number }>(
      '/tenant/billing/payments', { params: { page, size } }
    )
    return res.data
  },
  async recordManualPayment(req: ManualPaymentRequest): Promise<PaymentView> {
    const res = await http.post<PaymentView>('/tenant/billing/payments', req)
    return res.data
  },
  async listAllPayments(page = 0, size = 20): Promise<{ content: PaymentView[]; totalElements: number }> {
    const res = await http.get<{ content: PaymentView[]; totalElements: number }>(
      '/admin/billing/payments', { params: { page, size } }
    )
    return res.data
  },
  async adminStats(): Promise<AdminPaymentStats> {
    const res = await http.get<AdminPaymentStats>('/admin/billing/stats')
    return res.data
  },
  async adminRecordPayment(tenantId: string, req: Omit<ManualPaymentRequest, 'tenantId'>): Promise<PaymentView> {
    const res = await http.post<PaymentView>(`/admin/billing/tenants/${tenantId}/payments`, req)
    return res.data
  },
  async adminChangeStatus(tenantId: string, status: SubscriptionStatus, notes?: string): Promise<SubscriptionView> {
    const res = await http.patch<SubscriptionView>(
      `/admin/billing/tenants/${tenantId}/subscription/status`,
      null,
      { params: { status, notes } }
    )
    return res.data
  },
  async adminChangePlan(tenantId: string, planCode: string): Promise<SubscriptionView> {
    const res = await http.post<SubscriptionView>(
      `/admin/billing/tenants/${tenantId}/subscription/${planCode}`
    )
    return res.data
  },
  async adminStartTrial(tenantId: string, planCode: string): Promise<SubscriptionView> {
    const res = await http.post<SubscriptionView>(
      `/admin/billing/tenants/${tenantId}/trial/${planCode}`
    )
    return res.data
  },
}

// ─── Público · Parceiros ────────────────────────────────────────────────────

export interface PublicPartnerView {
  id: string
  slug: string
  tradeName: string
  description?: string
  logoUrl?: string
  bannerUrl?: string
  website?: string
  stores: PublicPartnerStoreSummary[]
  activeProductsCount: number
  realms: string[]
}

export interface PublicPartnerStoreSummary {
  id: string
  name: string
  slug: string
  city?: string
  state?: string
  isMain: boolean
}

export interface PublicTenantView {
  id: string
  slug: string
  tradeName: string
  legalName?: string
  description?: string
  logoUrl?: string
  bannerUrl?: string
  phone?: string
  whatsapp?: string
  email?: string
  website?: string
  theme: Record<string, string>
  stores: PublicTenantStoreView[]
  activeProductsCount: number
  realms: string[]
}

export interface PublicTenantStoreView {
  id: string
  name: string
  slug: string
  phone?: string
  whatsapp?: string
  email?: string
  city?: string
  state?: string
  latitude?: number
  longitude?: number
  isMain: boolean
}

export interface PublicProductView {
  id: string
  title?: string
  description?: string
  price: number
  currency: string
  realm: VehicleRealm
  yearModel?: number
  yearBuild?: number
  mileageKm?: number
  fuel?: string
  transmission?: string
  brandName?: string
  modelName?: string
  categoryName?: string
  physicalStoreId?: string
  physicalStoreName?: string
  physicalStoreCity?: string
  physicalStoreState?: string
  attributes: Record<string, unknown>
  images: PublicProductImageView[]
  createdAt?: string
}

export interface PublicProductImageView {
  id: string
  url: string
  isCover: boolean
}

export const partnerApi = {
  async listPartners(countryCode = 'BR'): Promise<PublicPartnerView[]> {
    const res = await http.get<PublicPartnerView[]>(`/${countryCode}/public/tenants/partner`)
    return res.data
  },
  async getPartner(slug: string, countryCode = 'BR'): Promise<PublicTenantView> {
    const res = await http.get<PublicTenantView>(`/${countryCode}/public/tenants/${slug}`)
    return res.data
  },
  async listProducts(slug: string, countryCode = 'BR', limit = 60): Promise<PublicProductView[]> {
    const res = await http.get<PublicProductView[]>(
      `/${countryCode}/public/tenants/${slug}/products`,
      { params: { limit } }
    )
    return res.data
  },
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
