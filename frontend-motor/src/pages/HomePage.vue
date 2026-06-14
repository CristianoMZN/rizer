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
        <PublicProductCard
          v-for="product in featured"
          :key="product.id"
          :product="product"
        />
      </div>
    </section>

    <!-- Categories -->
    <section class="container q-pa-lg">
      <h2 class="section-title q-mb-md">Categorias</h2>
      <div class="categories-grid">
        <button
          v-for="cat in categories"
          :key="cat.label"
          class="category-card"
          @click="searchByType(cat.label)"
        >
          <q-icon :name="cat.icon" size="42px" color="primary" />
          <p class="cat-name">{{ cat.label }}</p>
        </button>
      </div>
    </section>

    <!-- Below FIPE (placeholder enquanto a view ainda não existe) -->
    <section class="below-fipe-section q-py-xl">
      <div class="container text-center">
        <q-icon name="trending_down" size="64px" color="positive" />
        <h2 class="section-title q-mt-md">Abaixo da FIPE</h2>
        <p class="text-grey-6 q-mb-lg">
          Veículos com preço abaixo da tabela FIPE. Em breve você verá as melhores oportunidades aqui.
        </p>
        <q-btn outline color="primary" label="Ver veículos" to="/produtos" />
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
import type { VehicleFilters } from 'src/data/types'
import { catalogApi, type PublicProductView } from 'src/services/api'
import SmartSearch from 'components/form/SmartSearch.vue'
import PublicProductCard from 'components/vehicle/PublicProductCard.vue'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const router = useRouter()
const featured = ref<PublicProductView[]>([])
const loading = ref(true)
const totalVehicles = '4.200+'

const vehicleTypes = [
  { label: 'Carros', icon: 'directions_car' },
  { label: 'Motos', icon: 'two_wheeler' },
  { label: 'SUVs', icon: 'commute' },
  { label: 'Pickups', icon: 'airport_shuttle' },
]

const categories = [
  { label: 'Carros', icon: 'directions_car' },
  { label: 'Motos', icon: 'two_wheeler' },
  { label: 'SUVs', icon: 'commute' },
  { label: 'Pickups', icon: 'airport_shuttle' },
  { label: 'Caminhões', icon: 'local_shipping' },
  { label: 'Ônibus', icon: 'directions_bus' },
  { label: 'Náuticos', icon: 'sailing' },
  { label: 'Vans/Furgões', icon: 'airport_shuttle' },
]

const stats = [
  { value: '4.200+', label: 'Veículos anunciados' },
  { value: '380+', label: 'Lojas parceiras' },
  { value: '98%', label: 'Clientes satisfeitos' },
  { value: '15k+', label: 'Negócios realizados' },
]

onMounted(async () => {
  try {
    featured.value = await catalogApi.searchProducts({ limit: 6 })
  } catch {
    featured.value = []
  }
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
  const typeMap: Record<string, string> = {
    Carros: 'Carro',
    Motos: 'Moto',
    SUVs: 'Carro',
    Pickups: 'Carro',
    Caminhões: 'Caminhão',
    Ônibus: 'Ônibus',
    Náuticos: 'Náutico',
    'Vans/Furgões': 'Van/Furgão',
  }
  void router.push({ path: '/produtos', query: { type: typeMap[type] || 'Carro' } })
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

.categories-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.category-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: white;
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 14px;
  padding: 18px 12px;
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease;
  text-align: center;
  font: inherit;
  color: inherit;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.06);
    border-color: var(--q-primary);
  }
}

.cat-name {
  font-size: 13px;
  font-weight: 700;
  margin: 4px 0 0;
}

.below-fipe-section {
  background: linear-gradient(180deg, #ffffff 0%, #f6f8ff 100%);
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
