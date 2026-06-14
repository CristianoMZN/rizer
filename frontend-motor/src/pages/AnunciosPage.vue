<template>
  <q-page class="anuncios-page container q-pa-md">
    <div class="row items-center justify-between q-mb-lg">
      <h1 class="page-title">Meus Anúncios</h1>
      <q-btn unelevated color="primary" icon="add" label="Novo anúncio" to="/anuncios/novo" />
    </div>

    <LoadingSpinner v-if="loading" />

    <div v-else class="vehicles-grid">
      <q-card
        v-for="vehicle in vehicles"
        :key="vehicle.id"
        flat
        bordered
        class="anuncio-card"
      >
        <q-img :src="vehicle.images[0]?.url" height="160px" fit="cover" />
        <q-card-section class="q-pb-xs">
          <p class="card-title ellipsis">{{ vehicle.title }}</p>
          <p class="card-price text-primary">{{ formatPrice(vehicle.price) }}</p>
          <p class="text-caption text-grey-5">{{ (vehicle.mileageKm ?? 0).toLocaleString('pt-BR') }} km · {{ vehicle.yearModel ?? vehicle.yearBuild }}</p>
        </q-card-section>
        <q-card-actions class="row justify-between q-pa-sm">
          <q-btn flat icon="edit" size="sm" color="primary" label="Editar" :to="`/anuncios/${vehicle.id}/editar`" />
          <q-btn flat icon="delete" size="sm" color="negative" label="Pausar" />
        </q-card-actions>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { ProductView } from 'src/services/api'
import { tenantProductApi } from 'src/services/api'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const vehicles = ref<ProductView[]>([])
const loading = ref(true)

onMounted(async () => {
  vehicles.value = await tenantProductApi.list()
  loading.value = false
})

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.page-title { font-size: 2rem; font-weight: 800; margin: 0; }
.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
.anuncio-card { border-radius: 12px; overflow: hidden; }
.card-title { font-weight: 600; font-size: 14px; margin: 0 0 4px; }
.card-price { font-weight: 800; font-size: 18px; margin: 0; }
</style>
