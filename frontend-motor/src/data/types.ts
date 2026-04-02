// ─── Vehicle Types ────────────────────────────────────────────────────────────

export type VehicleType = 'Moto' | 'Carro' | 'Van/Furgão' | 'Ônibus' | 'Caminhão'
export type CarSubtype = 'Hatch' | 'Sedã' | 'SUV' | 'Cupê' | 'Perua' | 'Pickup'
export type BikeSubtype = 'Urban' | 'Naked' | 'Custom' | 'Sport' | 'Trail' | 'Scooter'
export type Fuel = 'Flex' | 'Gasolina' | 'Álcool' | 'Diesel' | 'Elétrico' | 'Híbrido'
export type Transmission = 'Manual' | 'Automático' | 'Automatizado'
export type Engine = '1.0' | '1.3' | '1.4' | '1.6' | '1.8' | '2.0' | '2.4' | '3.0+'

export interface VehicleVerification {
  hasDebts: boolean
  wasAuction: boolean
  wasRental: boolean
  wasStolen: boolean
  wasFlooded: boolean
  isFinanced: boolean
}

export interface FinancingOption {
  bank: string
  installments: number
  monthlyRate: number
  downPayment: number
  monthlyPayment: number
}

export interface VehicleLocation {
  city: string
  state: string
  zipCode?: string
}

export interface Vehicle {
  id: string
  title: string
  brand: string
  model: string
  year: number
  price: number
  mileage: number
  type: VehicleType
  subtype?: CarSubtype | BikeSubtype
  fuel: Fuel
  transmission: Transmission
  engine?: Engine
  color?: string
  doors?: number
  images: string[]
  description?: string
  location: VehicleLocation
  store: Store
  features: string[]
  verification: VehicleVerification
  financing: FinancingOption[]
  fipePrice?: number
  createdAt: string
  featured?: boolean
}

// ─── Store Types ──────────────────────────────────────────────────────────────

export interface StoreTheme {
  primaryColor: string
  secondaryColor: string
  logoUrl?: string
  bannerUrl?: string
}

export interface StoreAddress {
  street: string
  number: string
  complement?: string
  neighborhood: string
  city: string
  state: string
  zipCode: string
}

export interface Store {
  id: string
  slug: string
  name: string
  cnpj?: string
  phone: string
  email?: string
  website?: string
  description?: string
  logo?: string
  banner?: string
  theme: StoreTheme
  address: StoreAddress
  rating: number
  totalSales: number
  verified: boolean
  plan: 'basic' | 'pro' | 'enterprise'
}

// ─── User Types ───────────────────────────────────────────────────────────────

export type UserRole = 'buyer' | 'seller' | 'admin'

export interface User {
  id: string
  name: string
  email: string
  phone?: string
  avatar?: string
  role: UserRole
  storeId?: string
  wishlist: string[]
  createdAt: string
}

// ─── Lead Types ───────────────────────────────────────────────────────────────

export type LeadStatus = 'new' | 'contacted' | 'negotiating' | 'closed_won' | 'closed_lost'

export interface Lead {
  id: string
  vehicleId: string
  vehicle?: Vehicle
  buyerName: string
  buyerEmail: string
  buyerPhone: string
  message?: string
  status: LeadStatus
  storeId: string
  createdAt: string
  updatedAt: string
}

// ─── Notification Types ───────────────────────────────────────────────────────

export type NotificationType = 'price_drop' | 'new_lead' | 'message' | 'system'

export interface Notification {
  id: string
  type: NotificationType
  title: string
  message: string
  read: boolean
  vehicleId?: string
  createdAt: string
}

// ─── Filter Types ─────────────────────────────────────────────────────────────

export interface VehicleFilters {
  search?: string
  type?: VehicleType
  subtype?: string
  brand?: string
  model?: string
  yearMin?: number
  yearMax?: number
  priceMin?: number
  priceMax?: number
  mileageMax?: number
  fuel?: Fuel
  transmission?: Transmission
  features?: string[]
  city?: string
  state?: string
  storeId?: string
}

// ─── Optional Features Catalog ────────────────────────────────────────────────

export const OPTIONAL_FEATURES: Record<string, string[]> = {
  'Segurança': [
    'ABS', 'Airbag Frontal', 'Airbag Lateral', 'Airbag Cortina',
    'Controle de Tração', 'Frenagem de Emergência', 'Assistente de Rampa',
  ],
  'Conforto': [
    'Ar Condicionado', 'Ar Digital', 'Bancos de Couro', 'Bancos Aquecidos',
    'Volante Aquecido', 'Teto Solar', 'Teto Panorâmico',
  ],
  'Tecnologia': [
    'Central Multimídia', 'Android Auto', 'Apple CarPlay', 'GPS',
    'Câmera de Ré', 'Sensor 360°', 'Faróis LED', 'Faróis Xenon',
  ],
  'Performance': [
    'Turbo', 'Tração 4x4', 'Tração AWD', 'Start-Stop',
    'Piloto Automático', 'Suspensão Esportiva',
  ],
}

export const ALL_OPTIONAL_FEATURES = Object.values(OPTIONAL_FEATURES).flat()
