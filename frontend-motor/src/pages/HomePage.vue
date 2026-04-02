<template>
  <q-page class="home-page">
    <!-- Hero Section -->
    <section class="hero-section">
      <div class="hero-overlay" />
      <div class="hero-content container q-pa-xl">
        <h1 class="hero-title animate__animated animate__fadeInUp">
          Encontre o veículo<br /><span class="gradient-text">dos seus sonhos</span>
        </h1>
        <p class="hero-subtitle animate__animated animate__fadeInUp animate__delay-1s">
          Mais de {{ totalVehicles }} veículos verificados com as melhores condições do mercado.
        </p>
        <!-- Search bar -->
        <div class="hero-search animate__animated animate__fadeInUp animate__delay-2s">
          <SmartSearch @search-advanced="onSearchAdvanced" placeholder="Marca, modelo, ano..." />
        </div>
        <!-- Quick filters -->
        <div class="quick-filters row q-gutter-sm q-mt-md">
          <q-btn
            v-for="type in vehicleTypes"
            :key="type.label"
            outline
            color="white"
            :icon="type.icon"
            :label="type.label"
            rounded
            size="sm"
            @click="searchByType(type.label)"
          />
        </div>
      </div>
    </section>

    <!-- Featured Vehicles -->
    <section class="container q-pa-lg">
      <div class="section-header row items-center justify-between q-mb-lg">
        <h2 class="section-title">Destaques da semana</h2>
        <q-btn flat color="primary" label="Ver todos" icon-right="arrow_forward" to="/produtos" />
      </div>

      <LoadingSpinner v-if="loading" />

      <div v-else class="vehicles-grid">
        <VehicleCard
          v-for="vehicle in featured"
          :key="vehicle.id"
          :vehicle="vehicle"
          :is-wishlisted="wishlist.includes(vehicle.id)"
          @toggle-wishlist="toggleWishlist"
          @click="goToVehicle"
        />
      </div>
    </section>

    <!-- Stats banner -->
    <section class="stats-banner q-py-xl">
      <div class="container">
        <div class="row q-gutter-lg justify-center">
          <div v-for="stat in stats" :key="stat.label" class="stat-item text-center">
            <p class="stat-number">{{ stat.value }}</p>
            <p class="stat-label">{{ stat.label }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA - Partner -->
    <section class="cta-section container q-pa-xl text-center">
      <h2 class="section-title">É vendedor? Anuncie aqui</h2>
      <p class="text-grey-6 q-mb-lg">Alcance milhares de compradores com planos a partir de R$ 99/mês</p>
      <q-btn unelevated color="primary" size="lg" label="Seja Parceiro" to="/seja-parceiro" icon="storefront" />
    </section>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Vehicle } from 'src/data/types'
import type { VehicleFilters } from 'src/data/types'
import { api } from 'src/services/api'
import { MOCK_VEHICLES } from 'src/data/mock'
import SmartSearch from 'components/form/SmartSearch.vue'
import VehicleCard from 'components/vehicle/VehicleCard.vue'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const router = useRouter()
const featured = ref<Vehicle[]>([])
const loading = ref(true)
const wishlist = ref<string[]>([])
const totalVehicles = MOCK_VEHICLES.length

const vehicleTypes = [
  { label: 'Carros', icon: 'directions_car' },
  { label: 'Motos', icon: 'two_wheeler' },
  { label: 'SUVs', icon: 'commute' },
  { label: 'Pickups', icon: 'airport_shuttle' },
]

const stats = [
  { value: '4.200+', label: 'Veículos anunciados' },
  { value: '380+', label: 'Lojas parceiras' },
  { value: '98%', label: 'Clientes satisfeitos' },
  { value: '15k+', label: 'Negócios realizados' },
]

onMounted(async () => {
  featured.value = await api.getFeaturedVehicles()
  loading.value = false
})

function onSearchAdvanced(payload: { query: string; filters: Pick<VehicleFilters, 'type' | 'brand' | 'fuel' | 'priceMax'> }) {
  const queryParams: Record<string, string> = {}
  if (payload.query) queryParams.search = payload.query
  if (payload.filters.type) queryParams.type = payload.filters.type
  if (payload.filters.brand) queryParams.brand = payload.filters.brand
  if (payload.filters.fuel) queryParams.fuel = payload.filters.fuel
  if (payload.filters.priceMax) queryParams.priceMax = String(payload.filters.priceMax)

  void router.push({ path: '/produtos', query: queryParams })
}

function searchByType(type: string) {
  const typeMap: Record<string, string> = { Carros: 'Carro', Motos: 'Moto', SUVs: 'Carro', Pickups: 'Carro' }
  void router.push({ path: '/produtos', query: { type: typeMap[type] } })
}

function goToVehicle(v: Vehicle) {
  void router.push(`/produto/${v.id}`)
}

function toggleWishlist(id: string) {
  const idx = wishlist.value.indexOf(id)
  if (idx >= 0) wishlist.value.splice(idx, 1)
  else wishlist.value.push(id)
}
</script>

<style scoped lang="scss">
.hero-section {
  position: relative;
  min-height: 520px;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  align-items: center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background: url('https://images.unsplash.com/photo-1485291571150-772bcfc10da5?auto=format&fit=crop&w=1600&q=80') center/cover no-repeat;
  opacity: 0.15;
}

.hero-content { position: relative; z-index: 1; }

.hero-title {
  color: white;
  font-size: clamp(2rem, 5vw, 3.5rem);
  font-weight: 900;
  line-height: 1.2;
  margin: 0 0 16px;
}

.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  color: rgba(255, 255, 255, 0.75);
  font-size: 1.1rem;
  margin-bottom: 24px;
}

.hero-search { max-width: 600px; }

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.stats-banner {
  background: var(--gradient-primary);
}

.stat-number {
  font-size: 2.5rem;
  font-weight: 900;
  color: white;
  margin: 0;
}

.stat-label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin: 4px 0 0;
}

.section-title {
  font-size: 1.8rem;
  font-weight: 800;
  margin: 0;
}

.container { max-width: 1280px; margin: 0 auto; }
</style>
