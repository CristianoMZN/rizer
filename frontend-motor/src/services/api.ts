// ─── API Real (Axios) ─────────────────────────────────────────────────────────

import axios, { type AxiosInstance } from 'axios'

const http: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
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
  adminPhone?: string
  cnpj?: string
  legalName?: string
  bannerUrl?: string
  isBranch: boolean
  isMain: boolean
  isActive: boolean
  addressZipCode?: string
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressNeighborhood?: string
  addressCity?: string
  addressState?: string
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
  whatsapp?: string
  avatarUrl?: string
  role: TenantUserRole
  physicalStoreIds: string[]
  isActive: boolean
  passwordMustChange?: boolean
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
  adminPhone?: string
  cnpj?: string
  legalName?: string
  bannerUrl?: string
  isBranch?: boolean | null
  isMain?: boolean | null
  addressZipCode?: string
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressNeighborhood?: string
  addressCity?: string
  addressState?: string
  latitude?: number | null
  longitude?: number | null
}

export interface UpdateStoreRequest {
  name?: string
  phone?: string
  whatsapp?: string
  email?: string
  adminPhone?: string
  cnpj?: string
  legalName?: string
  bannerUrl?: string
  isBranch?: boolean | null
  isMain?: boolean | null
  isActive?: boolean | null
  addressZipCode?: string
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressNeighborhood?: string
  addressCity?: string
  addressState?: string
  latitude?: number | null
  longitude?: number | null
}

export interface InviteMemberRequest {
  tenantId?: string
  email: string
  name: string
  role: TenantUserRole
  physicalStoreIds?: string[]
  whatsapp?: string
  avatarUrl?: string
  password?: string
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
  sellerUserId?: string
  sellerName?: string
  sellerWhatsapp?: string
  sellerAvatarUrl?: string
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
  sellerUserId?: string
  latitude?: number
  longitude?: number
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
  sellerUserId?: string
  latitude?: number
  longitude?: number
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
    fuel?: string
    transmission?: string
    transmissionDetail?: string
    color?: string
    bodyType?: string
    drivetrain?: string
    steering?: string
    condition?: string
    engine?: string
    cylinders?: number
    armored?: boolean
    abs?: boolean
    limit?: number
    offset?: number
  } = {}): Promise<PublicProductView[]> {
    const cc = params.countryCode ?? 'BR'
    const res = await http.get<PublicProductView[]>(`/${cc}/public/products`, { params })
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
  async createDraft(physicalStoreId: string): Promise<ProductView> {
    const res = await http.post<ProductView>('/tenant/products/draft', { physicalStoreId })
    return res.data
  },
  async update(id: string, patch: UpdateProductRequest): Promise<ProductView> {
    const res = await http.patch<ProductView>(`/tenant/products/${id}`, patch)
    return res.data
  },
  async changeStatus(id: string, status: ProductStatus): Promise<ProductView> {
    const res = await http.patch<ProductView>(`/tenant/products/${id}/status`, { status })
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
  partnerOwnerName?: string
  partnerOwnerCpf?: string
  adminPhone?: string
  addressZipCode?: string
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressNeighborhood?: string
  addressCity?: string
  addressState?: string
  addressLatitude?: number
  addressLongitude?: number
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
  slug?: string
  tradeName?: string
  legalName?: string
  cnpj?: string
  partnerOwnerName?: string
  partnerOwnerCpf?: string
  description?: string
  phone?: string
  whatsapp?: string
  adminPhone?: string
  email?: string
  website?: string
  logoUrl?: string
  bannerUrl?: string
  addressZipCode?: string
  addressStreet?: string
  addressNumber?: string
  addressComplement?: string
  addressNeighborhood?: string
  addressCity?: string
  addressState?: string
  addressLatitude?: number | null
  addressLongitude?: number | null
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

// ─── Galeria (tenant e loja) ────────────────────────────────────────────────

export interface GalleryImageView {
  id: string
  url: string
  caption?: string
  sortOrder: number
  isCover: boolean
  createdAt: string
}

export const tenantGalleryApi = {
  async list(): Promise<GalleryImageView[]> {
    const res = await http.get<GalleryImageView[]>('/tenant/settings/gallery')
    return res.data
  },
  async upload(file: File, caption?: string): Promise<GalleryImageView> {
    const fd = new FormData()
    fd.append('file', file)
    if (caption) fd.append('caption', caption)
    const res = await http.post<GalleryImageView>('/tenant/settings/gallery/upload', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },
  async setCover(id: string): Promise<GalleryImageView> {
    const res = await http.patch<GalleryImageView>(`/tenant/settings/gallery/${id}/cover`)
    return res.data
  },
  async reorder(ids: string[]): Promise<void> {
    await http.patch('/tenant/settings/gallery/reorder', { ids })
  },
  async delete(id: string): Promise<void> {
    await http.delete(`/tenant/settings/gallery/${id}`)
  },
}

export const storeGalleryApi = {
  async list(storeId: string): Promise<GalleryImageView[]> {
    const res = await http.get<GalleryImageView[]>(`/tenant/stores/${storeId}/gallery`)
    return res.data
  },
  async upload(storeId: string, file: File, caption?: string): Promise<GalleryImageView> {
    const fd = new FormData()
    fd.append('file', file)
    if (caption) fd.append('caption', caption)
    const res = await http.post<GalleryImageView>(`/tenant/stores/${storeId}/gallery/upload`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },
  async setCover(storeId: string, id: string): Promise<GalleryImageView> {
    const res = await http.patch<GalleryImageView>(`/tenant/stores/${storeId}/gallery/${id}/cover`)
    return res.data
  },
  async reorder(storeId: string, ids: string[]): Promise<void> {
    await http.patch(`/tenant/stores/${storeId}/gallery/reorder`, { ids })
  },
  async delete(storeId: string, id: string): Promise<void> {
    await http.delete(`/tenant/stores/${storeId}/gallery/${id}`)
  },
}

// ─── Utilitários (VIACEP / CepAberto) ──────────────────────────────────────

export interface CepLookupView {
  cep: string
  street?: string
  complement?: string
  neighborhood?: string
  city?: string
  state?: string
  ibge?: string
  ddd?: string
  latitude?: number
  longitude?: number
}

export const utilApi = {
  async cepLookup(cep: string): Promise<CepLookupView | null> {
    try {
      const res = await http.get<CepLookupView>(`/tenant/util/cep/${cep}`)
      return res.data
    } catch (e: unknown) {
      const status = (e as { response?: { status?: number } })?.response?.status
      if (status === 204) return null
      throw e
    }
  },
}

// ─── LGPD ───────────────────────────────────────────────────────────────────────

export type ConsentPurpose =
  | 'terms_of_use' | 'privacy_policy' | 'cookies_essential'
  | 'cookies_analytics' | 'cookies_marketing' | 'marketing_emails'
  | 'data_sharing_integrations'

export type DataExportStatus = 'pending' | 'processing' | 'ready' | 'expired' | 'failed'

export interface ConsentView {
  id: string
  purpose: ConsentPurpose
  granted: boolean
  documentVersion: string
  ip?: string
  userAgent?: string
  createdAt: string
}

export interface DataExportRequestView {
  id: string
  status: DataExportStatus
  storageKey?: string
  downloadUrl?: string
  urlExpiresAt?: string
  errorMessage?: string
  requestedAt: string
  completedAt?: string
}

export const lgpdApi = {
  async recordConsent(purpose: ConsentPurpose, granted: boolean, documentVersion: string): Promise<ConsentView> {
    const res = await http.post<ConsentView>('/me/consents', { purpose, granted, documentVersion })
    return res.data
  },
  async myConsents(): Promise<ConsentView[]> {
    const res = await http.get<ConsentView[]>('/me/consents')
    return res.data
  },
  async requestDataExport(): Promise<DataExportRequestView> {
    const res = await http.post<DataExportRequestView>('/me/data-export')
    return res.data
  },
  async myExports(): Promise<DataExportRequestView[]> {
    const res = await http.get<DataExportRequestView[]>('/me/data-export')
    return res.data
  },
  async deleteAccount(reason?: string): Promise<{ status: string; message: string }> {
    const res = await http.delete<{ status: string; message: string }>('/me/account', {
      data: { reason: reason ?? '' },
    })
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
  gallery: GalleryImageView[]
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
  bannerUrl?: string
  isBranch: boolean
  isMain: boolean
  gallery: GalleryImageView[]
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
  categoryId?: string
  categoryName?: string
  physicalStoreId?: string
  physicalStoreName?: string
  physicalStoreCity?: string
  physicalStoreState?: string
  physicalStoreBannerUrl?: string
  attributes: Record<string, unknown>
  images: PublicProductImageView[]
  createdAt?: string
  tenantSlug?: string
  tenantTradeName?: string
  tenantLogoUrl?: string
  tenantWhatsapp?: string
  tenantPhone?: string
  sellerUserId?: string
  sellerName?: string
  sellerWhatsapp?: string
  sellerAvatarUrl?: string
  latitude?: number
  longitude?: number
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

// ─── Me (consumidor final) ──────────────────────────────────────────────────

export interface MediaUploadResponse {
  url: string
  key: string
  bucket: string
  contentType?: string
  size: number
}

export const mediaApi = {
  async uploadImage(file: File, context = 'user-avatar'): Promise<MediaUploadResponse> {
    const fd = new FormData()
    fd.append('file', file)
    const res = await http.post<MediaUploadResponse>('/media/upload/image', fd, {
      params: { context },
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return res.data
  },
}

export interface ConsumerProfile {
  id: string
  email: string
  name: string
  phone?: string
  cpf?: string
  birthDate?: string
  avatarUrl?: string
  systemRole: string
  profileCompleted: boolean
  createdAt: string
  updatedAt: string
}

export interface AddressView {
  id: string
  label?: string
  zipCode?: string
  street: string
  number?: string
  complement?: string
  neighborhood?: string
  city: string
  state: string
  countryCode: string
  country?: string
  isPrimary: boolean
  createdAt: string
  updatedAt: string
}

export interface AddressInput {
  label?: string
  zipCode?: string
  street: string
  number?: string
  complement?: string
  neighborhood?: string
  city: string
  state: string
  countryCode: string
  country?: string
  isPrimary?: boolean
}

export interface FavoriteView {
  id: string
  productId: string
  createdAt: string
  product: PublicProductView | null
}

export const meApi = {
  async getProfile(): Promise<ConsumerProfile> {
    const res = await http.get<ConsumerProfile>('/me/profile')
    return res.data
  },
  async updateProfile(patch: Partial<{
    name: string; phone: string; cpf: string; birthDate: string; avatarUrl: string
  }>): Promise<ConsumerProfile> {
    const res = await http.patch<ConsumerProfile>('/me/profile', patch)
    return res.data
  },
  async listAddresses(): Promise<AddressView[]> {
    const res = await http.get<AddressView[]>('/me/addresses')
    return res.data
  },
  async createAddress(input: AddressInput): Promise<AddressView> {
    const res = await http.post<AddressView>('/me/addresses', input)
    return res.data
  },
  async updateAddress(id: string, patch: Partial<AddressInput>): Promise<AddressView> {
    const res = await http.patch<AddressView>(`/me/addresses/${id}`, patch)
    return res.data
  },
  async deleteAddress(id: string): Promise<void> {
    await http.delete(`/me/addresses/${id}`)
  },
  async setPrimaryAddress(id: string): Promise<AddressView> {
    const res = await http.post<AddressView>(`/me/addresses/${id}/primary`)
    return res.data
  },
  async listFavorites(): Promise<FavoriteView[]> {
    const res = await http.get<FavoriteView[]>('/me/favorites')
    return res.data
  },
  async listFavoriteIds(): Promise<string[]> {
    const res = await http.get<{ ids: string[] }>('/me/favorites/ids')
    return res.data.ids ?? []
  },
  async addFavorite(productId: string): Promise<void> {
    await http.post(`/me/favorites/${productId}`)
  },
  async removeFavorite(productId: string): Promise<void> {
    await http.delete(`/me/favorites/${productId}`)
  },
}

// ─── Leads ──────────────────────────────────────────────────────────────────

export type LeadStatus = 'NEW' | 'CONTACTED' | 'NEGOTIATING' | 'CLOSED_WON' | 'CLOSED_LOST'

export interface LeadView {
  id: string
  tenantId: string
  productId?: string
  physicalStoreId?: string
  buyerName: string
  buyerEmail?: string
  buyerPhone: string
  message?: string
  status: LeadStatus
  createdAt: string
  updatedAt: string
}

export interface CreateLeadRequest {
  productId?: string
  storeId?: string
  buyerName: string
  buyerEmail?: string
  buyerPhone: string
  message?: string
}

export const leadApi = {
  async create(req: CreateLeadRequest): Promise<LeadView> {
    const res = await http.post<LeadView>('/BR/public/leads', req)
    return res.data
  },
  async list(): Promise<LeadView[]> {
    const res = await http.get<LeadView[]>('/tenant/leads')
    return res.data
  },
  async updateStatus(id: string, status: LeadStatus): Promise<LeadView> {
    const res = await http.patch<LeadView>(`/tenant/leads/${id}/status`, { status })
    return res.data
  },
}

export { http }
