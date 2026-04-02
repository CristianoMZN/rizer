import type { Vehicle, Store, Lead, Notification, User, VehicleFilters } from '../data/types'
import {
  MOCK_VEHICLES,
  MOCK_STORES,
  MOCK_LEADS,
  MOCK_NOTIFICATIONS,
  MOCK_USER,
} from '../data/mock'
import { getTenantScope } from 'src/composables/useTenant'

// ─── Config ───────────────────────────────────────────────────────────────────

export const MOCK_CONFIG = {
  useBackend: false,
  apiBase: import.meta.env.VITE_API_URL || '/api',
  delay: 300, // simulate network delay (ms)
}

const delay = (ms = MOCK_CONFIG.delay) => new Promise((r) => setTimeout(r, ms))

// ─── Helpers de escopo ────────────────────────────────────────────────────────

/**
 * Retorna headers HTTP para requisições ao backend.
 * Quando em modo `store`, injeta X-Tenant-Slug para que o servidor
 * filtre os recursos no contexto daquele tenant (loja).
 */
function getTenantHeaders(): Record<string, string> {
  const scope = getTenantScope()
  if (scope.mode === 'store') {
    return { 'X-Tenant-Slug': scope.storeSlug }
  }
  return {}
}

/**
 * Aplica o filtro de loja no mock quando em modo `store`.
 * No modo `marketplace`, retorna todos os recursos sem restrição.
 */
function applyStoreScope<T extends { store: Store }>(items: T[]): T[] {
  const scope = getTenantScope()
  if (scope.mode === 'store') {
    return items.filter((item) => item.store.slug === scope.storeSlug)
  }
  return items
}

/**
 * Aplica filtro de loja em coleções de leads (que referenciam storeId,
 * não diretamente store.slug). Resolve via mapa reverso de stores mock.
 */
function applyStoreScopeToLeads(leads: Lead[]): Lead[] {
  const scope = getTenantScope()
  if (scope.mode !== 'store') return leads
  const store = MOCK_STORES.find((s) => s.slug === scope.storeSlug)
  if (!store) return []
  return leads.filter((l) => l.storeId === store.id)
}

// ─── Vehicles ─────────────────────────────────────────────────────────────────

export const api = {
  async getVehicles(filters?: VehicleFilters): Promise<Vehicle[]> {
    if (MOCK_CONFIG.useBackend) {
      const params = new URLSearchParams(filters as Record<string, string>)
      const res = await fetch(`${MOCK_CONFIG.apiBase}/vehicles?${params}`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    let vehicles = applyStoreScope([...MOCK_VEHICLES])
    if (filters?.type) vehicles = vehicles.filter((v) => v.type === filters.type)
    if (filters?.brand) vehicles = vehicles.filter((v) => v.brand === filters.brand)
    if (filters?.priceMin) vehicles = vehicles.filter((v) => v.price >= filters.priceMin!)
    if (filters?.priceMax) vehicles = vehicles.filter((v) => v.price <= filters.priceMax!)
    if (filters?.fuel) vehicles = vehicles.filter((v) => v.fuel === filters.fuel)
    if (filters?.storeId) vehicles = vehicles.filter((v) => v.store.id === filters.storeId)
    if (filters?.search) {
      const q = filters.search.toLowerCase()
      vehicles = vehicles.filter((v) =>
        v.title.toLowerCase().includes(q) ||
        v.brand.toLowerCase().includes(q) ||
        v.model.toLowerCase().includes(q),
      )
    }
    return vehicles
  },

  async getVehicleById(id: string): Promise<Vehicle | undefined> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/vehicles/${id}`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    // Em modo store: garante que o veículo pertence à loja do tenant
    const scoped = applyStoreScope([...MOCK_VEHICLES])
    return scoped.find((v) => v.id === id)
  },

  async getFeaturedVehicles(): Promise<Vehicle[]> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/vehicles?featured=true`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    return applyStoreScope(MOCK_VEHICLES.filter((v) => v.featured))
  },

  // ─── Stores ─────────────────────────────────────────────────────────────────

  async getStores(): Promise<Store[]> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/stores`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    // Em modo store, exibe apenas a própria loja
    const scope = getTenantScope()
    if (scope.mode === 'store') {
      return MOCK_STORES.filter((s) => s.slug === scope.storeSlug)
    }
    return MOCK_STORES
  },

  async getStoreBySlug(slug: string): Promise<Store | undefined> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/stores/${slug}`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    return MOCK_STORES.find((s) => s.slug === slug)
  },

  // ─── Leads ──────────────────────────────────────────────────────────────────

  async getLeads(storeId?: string): Promise<Lead[]> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/leads?storeId=${storeId ?? ''}`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    const filtered = storeId
      ? MOCK_LEADS.filter((l) => l.storeId === storeId)
      : MOCK_LEADS
    return applyStoreScopeToLeads(filtered)
  },

  async createLead(lead: Omit<Lead, 'id' | 'status' | 'createdAt' | 'updatedAt'>): Promise<Lead> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/leads`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', ...getTenantHeaders() },
        body: JSON.stringify(lead),
      })
      return res.json()
    }
    await delay()
    return {
      ...lead,
      id: `l${Date.now()}`,
      status: 'new',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }
  },

  // ─── Notifications ──────────────────────────────────────────────────────────

  async getNotifications(): Promise<Notification[]> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/notifications`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    return MOCK_NOTIFICATIONS
  },

  // ─── Auth ────────────────────────────────────────────────────────────────────

  async getMe(): Promise<User> {
    if (MOCK_CONFIG.useBackend) {
      const res = await fetch(`${MOCK_CONFIG.apiBase}/me`, {
        headers: getTenantHeaders(),
      })
      return res.json()
    }
    await delay()
    return MOCK_USER
  },

  // ─── ViaCEP ──────────────────────────────────────────────────────────────────

  async lookupCep(cep: string): Promise<{ city: string; state: string; street: string; neighborhood: string } | null> {
    try {
      const cleaned = cep.replace(/\D/g, '')
      const res = await fetch(`https://viacep.com.br/ws/${cleaned}/json/`)
      const data = await res.json()
      if (data.erro) return null
      return {
        city: data.localidade,
        state: data.uf,
        street: data.logradouro,
        neighborhood: data.bairro,
      }
    } catch {
      return null
    }
  },
}
