<template>
  <q-card flat bordered class="similar-card rounded-borders" @click="$router.push(`/produto/${product.id}`)">
    <q-img :src="cover" :ratio="16/10" class="similar-image" no-spinner>
      <div class="absolute-top-right q-pa-xs">
        <q-btn flat round color="white" :icon="isFav ? 'favorite' : 'favorite_border'" size="sm" @click.stop="toggleFav" />
      </div>
    </q-img>

    <div class="similar-mini-header row items-center q-px-sm q-py-xs">
      <q-avatar v-if="product.tenantLogoUrl" size="20px" class="q-mr-xs">
        <img :src="product.tenantLogoUrl" :alt="product.tenantTradeName">
      </q-avatar>
      <div class="col text-caption text-grey-7 ellipsis text-weight-medium">
        {{ product.tenantTradeName || product.physicalStoreName || '' }}
      </div>
      <q-btn
        v-if="whatsapp"
        flat round dense
        icon="fab fa-whatsapp"
        color="green"
        size="xs"
        @click.stop="openWhatsapp"
      />
    </div>

    <q-card-section class="q-pa-sm">
      <p class="similar-title ellipsis-2-lines">{{ product.title || 'Veículo' }}</p>
      <p class="similar-price">{{ price }}</p>
      <p class="text-caption text-grey-6 q-mb-none">
        {{ product.yearModel || '—' }} ·
        <span v-if="product.mileageKm">{{ product.mileageKm.toLocaleString('pt-BR') }} km · </span>
        {{ product.fuel || '—' }}
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

const whatsapp = computed(() => {
  return props.product.sellerWhatsapp || props.product.tenantWhatsapp || null
})

function openWhatsapp() {
  const phone = whatsapp.value?.replace(/\D/g, '')
  if (!phone) return
  const msg = encodeURIComponent(`Olá! Vi o anúncio ${props.product.title || 'deste veículo'} no Motorise e tenho interesse.`)
  window.open(`https://wa.me/${phone}?text=${msg}`, '_blank')
}

async function toggleFav() {
  if (!auth.isAuthenticated.value) {
    $q.notify({ message: 'Faça login para favoritar.', color: 'warning' })
    return
  }
  try { await favs.toggle(props.product.id) }
  catch { $q.notify({ message: 'Falha ao favoritar.', color: 'negative' }) }
}
</script>

<style scoped lang="scss">
.similar-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  overflow: hidden;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0,0,0,0.08);
  }
}
.similar-image { background: #f5f5f5; }
.similar-mini-header {
  min-height: 32px;
  background: #fafafa;
  border-bottom: 1px solid rgba(0,0,0,0.04);
}
.similar-title {
  font-size: 13px;
  font-weight: 700;
  margin: 0 0 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.similar-price {
  font-size: 16px;
  font-weight: 800;
  color: var(--q-primary);
  margin: 0 0 2px;
}
.rounded-borders { border-radius: 12px; }
</style>
