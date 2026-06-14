<template>
  <q-page class="produtos-page">
    <div class="container q-pa-md">
      <div class="row q-gutter-md">
        <!-- Sidebar filters -->
        <div class="col-12 col-md-3">
          <FilterPanel v-model="filters" @update:model-value="onFiltersChange" />
        </div>

        <!-- Results -->
        <div class="col">
          <div class="row items-center justify-between q-mb-md">
            <div>
              <h1 class="results-title">Veículos</h1>
              <p class="text-grey-6 q-mb-none">{{ vehicles.length }} resultado(s) encontrado(s)</p>
            </div>
            <div class="row q-gutter-sm">
              <q-btn-toggle
                v-model="viewMode"
                :options="[{ icon: 'grid_view', value: 'grid' }, { icon: 'view_list', value: 'list' }]"
                flat
                toggle-color="primary"
              />
            </div>
          </div>

          <LoadingSpinner v-if="loading" full-page />

          <div v-else-if="!vehicles.length" class="empty-state flex flex-center column q-py-xl">
            <q-icon name="search_off" size="80px" color="grey-3" />
            <p class="text-h6 text-grey-5 q-mt-md">Nenhum veículo encontrado</p>
            <q-btn flat color="primary" label="Limpar filtros" @click="clearFilters" />
          </div>

          <div v-else :class="viewMode === 'grid' ? 'vehicles-grid' : 'vehicles-list'">
            <PublicProductCard
              v-for="v in vehicles"
              :key="v.id"
              :product="v"
            />
          </div>

          <div v-if="vehicles.length && hasMore" class="flex flex-center q-mt-xl">
            <q-btn
              unelevated
              color="primary"
              label="Carregar mais"
              :loading="loadingMore"
              @click="loadMore"
            />
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import type { VehicleFilters, VehicleType } from 'src/data/types'
import { catalogApi, type PublicProductView } from 'src/services/api'
import FilterPanel from 'components/form/FilterPanel.vue'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'
import PublicProductCard from 'components/vehicle/PublicProductCard.vue'

const route = useRoute()
const vehicles = ref<PublicProductView[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(false)
const viewMode = ref<'grid' | 'list'>('grid')

const filters = ref<VehicleFilters>(parseFiltersFromRoute())

function parseFiltersFromRoute(): VehicleFilters {
  const f: VehicleFilters = {}
  if (route.query.search) f.search = route.query.search as string
  if (route.query.type) f.type = route.query.type as VehicleType
  if (route.query.brand) f.brand = route.query.brand as string
  if (route.query.fuel) f.fuel = route.query.fuel as NonNullable<VehicleFilters['fuel']>
  if (route.query.priceMax) {
    const value = Number(route.query.priceMax)
    if (!Number.isNaN(value)) f.priceMax = value
  }
  return f
}

onMounted(async () => {
  await loadVehicles()
})

watch(() => route.query, () => {
  filters.value = parseFiltersFromRoute()
  void loadVehicles()
})

async function loadVehicles() {
  loading.value = true
  try {
    const params = buildBackendParams(filters.value)
    const result = await catalogApi.searchProducts({ ...params, limit: 60 })
    vehicles.value = result
    hasMore.value = result.length >= 60
  } catch {
    vehicles.value = []
    hasMore.value = false
  }
  loading.value = false
}

function buildBackendParams(f: VehicleFilters): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (f.type) params.realm = toRealm(f.type)
  if (f.fuel) params.fuel = f.fuel
  if (f.transmission) {
    params.transmission = f.transmission
    params.transmissionDetail = f.transmission
  }
  if (f.color) params.color = f.color
  if (f.bodyType) params.bodyType = f.bodyType
  if (f.drivetrain) params.drivetrain = f.drivetrain
  if (f.steering) params.steering = f.steering
  if (f.condition) params.condition = f.condition
  if (f.engine) params.engine = f.engine
  if (f.cylinders !== undefined) params.cylinders = f.cylinders
  if (f.armored !== undefined) params.armored = f.armored
  if (f.abs !== undefined) params.abs = f.abs
  return params
}

function toRealm(t: VehicleType): 'CAR' | 'MOTORCYCLE' | 'TRUCK' | 'NAUTICAL' | 'BUS' {
  switch (t) {
    case 'Moto': return 'MOTORCYCLE'
    case 'Caminhão': return 'TRUCK'
    case 'Ônibus': return 'BUS'
    case 'Van/Furgão': return 'CAR'
    case 'Carro':
    default: return 'CAR'
  }
}

function onFiltersChange(f: VehicleFilters) {
  filters.value = f
  void loadVehicles()
}

function clearFilters() {
  filters.value = {}
  void loadVehicles()
}

async function loadMore() {
  loadingMore.value = true
  const params = buildBackendParams(filters.value)
  const more = await catalogApi.searchProducts({ ...params, limit: 60, offset: vehicles.value.length }).catch(() => [])
  if (more.length === 0) {
    hasMore.value = false
  } else {
    vehicles.value = [...vehicles.value, ...more]
  }
  loadingMore.value = false
}
</script>

<style scoped lang="scss">
.results-title { font-size: 1.5rem; font-weight: 800; margin: 0; }
.container { max-width: 1280px; margin: 0 auto; }

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.vehicles-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
