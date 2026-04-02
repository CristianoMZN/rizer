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
          <!-- Header -->
          <div class="row items-center justify-between q-mb-md">
            <div>
              <h1 class="results-title">Veículos</h1>
              <p class="text-grey-6 q-mb-none">{{ vehicles.length }} resultado(s) encontrado(s)</p>
            </div>
            <div class="row q-gutter-sm">
              <q-select
                v-model="sortBy"
                :options="sortOptions"
                label="Ordenar"
                outlined
                dense
                emit-value
                map-options
                style="min-width: 180px"
                @update:model-value="sortVehicles"
              />
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
            <VehicleCard
              v-for="vehicle in vehicles"
              :key="vehicle.id"
              :vehicle="vehicle"
              :is-wishlisted="wishlist.includes(vehicle.id)"
              @toggle-wishlist="toggleWishlist"
              @click="(v) => $router.push(`/produto/${v.id}`)"
            />
          </div>

          <!-- Load more -->
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
import type { Vehicle, VehicleFilters, VehicleType } from 'src/data/types'
import { api } from 'src/services/api'
import VehicleCard from 'components/vehicle/VehicleCard.vue'
import FilterPanel from 'components/form/FilterPanel.vue'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const route = useRoute()
const vehicles = ref<Vehicle[]>([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const viewMode = ref<'grid' | 'list'>('grid')
const sortBy = ref('createdAt')
const wishlist = ref<string[]>([])

function parseFiltersFromRoute(): VehicleFilters {
  const f: VehicleFilters = {}
  if (route.query.search) f.search = route.query.search as string
  if (route.query.type) f.type = route.query.type as VehicleType
  if (route.query.brand) f.brand = route.query.brand as string
  if (route.query.fuel) f.fuel = route.query.fuel as Vehicle['fuel']
  if (route.query.priceMax) {
    const value = Number(route.query.priceMax)
    if (!Number.isNaN(value)) f.priceMax = value
  }
  return f
}

const filters = ref<VehicleFilters>(parseFiltersFromRoute())

const sortOptions = [
  { label: 'Mais recentes', value: 'createdAt' },
  { label: 'Menor preço', value: 'price_asc' },
  { label: 'Maior preço', value: 'price_desc' },
  { label: 'Menor KM', value: 'mileage' },
]

onMounted(async () => {
  await loadVehicles()
})

watch(() => route.query, () => {
  filters.value = parseFiltersFromRoute()
  void loadVehicles()
})

async function loadVehicles() {
  loading.value = true
  vehicles.value = await api.getVehicles(filters.value)
  sortVehicles()
  loading.value = false
  hasMore.value = vehicles.value.length >= 6
}

function onFiltersChange(f: VehicleFilters) {
  filters.value = f
  void loadVehicles()
}

function sortVehicles() {
  if (sortBy.value === 'price_asc') vehicles.value.sort((a, b) => a.price - b.price)
  else if (sortBy.value === 'price_desc') vehicles.value.sort((a, b) => b.price - a.price)
  else if (sortBy.value === 'mileage') vehicles.value.sort((a, b) => a.mileage - b.mileage)
  else vehicles.value.sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}

function clearFilters() {
  filters.value = {}
  void loadVehicles()
}

function toggleWishlist(id: string) {
  const idx = wishlist.value.indexOf(id)
  if (idx >= 0) wishlist.value.splice(idx, 1)
  else wishlist.value.push(id)
}

async function loadMore() {
  loadingMore.value = true
  await new Promise((r) => setTimeout(r, 800))
  hasMore.value = false
  loadingMore.value = false
}
</script>

<style scoped lang="scss">
.results-title { font-size: 1.5rem; font-weight: 800; margin: 0; }
.container { max-width: 1280px; margin: 0 auto; }

.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}

.vehicles-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
