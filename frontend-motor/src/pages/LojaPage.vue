<template>
  <q-page class="loja-page">
    <LoadingSpinner v-if="loading" full-page />
    <div v-else-if="store">
      <div class="container q-pa-md">
        <StoreProfile :store="store" class="q-mb-lg" />

        <div class="row q-gutter-md">
          <div class="col-12 col-md-3">
            <FilterPanel v-model="filters" @update:model-value="loadVehicles" />
          </div>
          <div class="col">
            <div class="vehicles-grid">
              <VehicleCard
                v-for="v in vehicles"
                :key="v.id"
                :vehicle="v"
                @click="(vehicle) => $router.push(`/produto/${vehicle.id}`)"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import type { Store, Vehicle, VehicleFilters } from 'src/data/types'
import { api } from 'src/services/apiMock'
import StoreProfile from 'components/business/StoreProfile.vue'
import VehicleCard from 'components/vehicle/VehicleCard.vue'
import FilterPanel from 'components/form/FilterPanel.vue'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const route = useRoute()
const store = ref<Store | null>(null)
const vehicles = ref<Vehicle[]>([])
const loading = ref(true)
const filters = ref<VehicleFilters>({})

onMounted(async () => {
  store.value = (await api.getStoreBySlug(route.params.slug as string)) ?? null
  if (store.value) await loadVehicles({ storeId: store.value.id })
  loading.value = false
})

async function loadVehicles(extra: VehicleFilters = {}) {
  if (!store.value) return
  vehicles.value = await api.getVehicles({ storeId: store.value.id, ...filters.value, ...extra })
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
</style>
