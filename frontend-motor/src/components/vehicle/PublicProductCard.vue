<template>
  <q-card flat bordered class="vehicle-card-public" @click="$router.push(`/produto/${product.id}`)">
    <q-img :src="cover" :ratio="16/10" class="card-image" no-spinner>
      <div class="absolute-top-right q-pa-xs">
        <q-btn flat round color="white" :icon="isFav ? 'favorite' : 'favorite_border'" size="sm" @click.stop="onToggleFav" />
      </div>
    </q-img>

    <q-separator />

    <div class="mini-header row items-center q-px-sm q-py-xs">
      <q-avatar v-if="product.tenantLogoUrl" size="28px" class="q-mr-sm">
        <img :src="product.tenantLogoUrl" :alt="product.tenantTradeName">
      </q-avatar>
      <q-avatar v-else color="primary" text-color="white" size="28px" class="q-mr-sm">
        {{ (product.tenantTradeName || product.physicalStoreName || 'L').charAt(0).toUpperCase() }}
      </q-avatar>
      <div class="col text-caption text-grey-8 ellipsis">
        <span class="text-weight-bold">{{ product.tenantTradeName }}</span>
        <span v-if="product.physicalStoreName"> | {{ product.physicalStoreName }}</span>
      </div>
      <q-btn
        v-if="whatsapp"
        flat round dense
        icon="fab fa-whatsapp"
        color="green"
        size="sm"
        @click.stop="openWhatsapp"
      >
        <q-tooltip>Falar com vendedor</q-tooltip>
      </q-btn>
    </div>

    <q-card-section class="q-pt-sm q-pb-xs q-px-sm">
      <p class="card-title ellipsis-2-lines">{{ product.title || 'Veículo' }}</p>
      <p class="card-price">{{ price }}</p>
      <div class="row q-gutter-xs q-mt-xs vehicle-specs">
        <q-chip v-if="product.yearModel" dense outline size="sm" icon="calendar_today" :label="product.yearModel" />
        <q-chip v-if="product.mileageKm" dense outline size="sm" icon="speed" :label="`${product.mileageKm.toLocaleString('pt-BR')} km`" />
        <q-chip v-if="product.fuel" dense outline size="sm" icon="local_gas_station" :label="product.fuel" />
        <q-chip v-if="product.transmission" dense outline size="sm" icon="settings" :label="product.transmission" />
      </div>
      <p v-if="location" class="text-caption text-grey-6 q-mt-xs q-mb-none">
        <q-icon name="location_on" size="14px" /> {{ location }}
      </p>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useQuasar } from 'quasar'
import { useFavorites } from 'src/composables/useFavorites'
import { useAuthStore } from 'src/stores/authStore'
import type { PublicProductView } from 'src/services/api'

const props = defineProps<{ product: PublicProductView }>()

const $q = useQuasar()
const auth = useAuthStore()
const favs = useFavorites()

const cover = computed(() => {
  const imgs = props.product.images ?? []
  const c = imgs.find((i) => i.isCover) ?? imgs[0]
  return c?.url ?? 'https://placehold.co/600x400/1a1a2e/ffffff?text=Motorise'
})

const isFav = computed(() => favs.isFavorite(props.product.id))

const price = computed(() => {
  const p = props.product.price ?? 0
  if (!p) return 'Consulte'
  return p.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
})

const location = computed(() => {
  const c = props.product.physicalStoreCity
  const s = props.product.physicalStoreState
  if (!c && !s) return ''
  return `${c ?? ''}/${s ?? ''}`
})

const whatsapp = computed(() => {
  return props.product.sellerWhatsapp || props.product.tenantWhatsapp || null
})

function openWhatsapp() {
  const phone = whatsapp.value?.replace(/\D/g, '')
  if (!phone) return
  const msg = encodeURIComponent(`Olá! Vi o anúncio ${props.product.title || 'deste veículo'} no Motorise e tenho interesse.`)
  window.open(`https://wa.me/${phone}?text=${msg}`, '_blank')
}

async function onToggleFav() {
  if (!auth.isAuthenticated.value) {
    $q.notify({ message: 'Faça login para favoritar.', color: 'warning' })
    return
  }
  try { await favs.toggle(props.product.id) }
  catch { $q.notify({ message: 'Falha ao favoritar.', color: 'negative' }) }
}
</script>

<style scoped lang="scss">
.vehicle-card-public {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.08);
  }
}
.card-image { background: #f5f5f5; }
.mini-header {
  min-height: 40px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.card-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-price {
  font-size: 20px;
  font-weight: 800;
  color: var(--q-primary);
  margin: 0;
}
.vehicle-specs :deep(.q-chip) { font-size: 11px; }
</style>
