<template>
  <q-card v-if="favorite.product" flat bordered class="fav-card rounded-borders" @click="$emit('open', favorite.productId)">
    <q-img
      :src="coverImage"
      :ratio="16/10"
      class="fav-image"
      no-spinner
    >
      <div class="absolute-top-right q-pa-xs">
        <q-btn
          flat
          round
          color="white"
          icon="favorite"
          size="sm"
          @click.stop="$emit('remove', favorite.productId)"
        >
          <q-tooltip>Remover dos favoritos</q-tooltip>
        </q-btn>
      </div>
    </q-img>

    <q-card-section class="q-pa-md">
      <p class="fav-title ellipsis-2-lines">{{ favorite.product.title || 'Veículo' }}</p>
      <p class="fav-price">{{ price }}</p>
      <p class="text-caption text-grey-6 q-mb-none">
        <span v-if="favorite.product.yearModel">{{ favorite.product.yearModel }} · </span>
        <span v-if="favorite.product.mileageKm">{{ favorite.product.mileageKm.toLocaleString('pt-BR') }} km · </span>
        <span>{{ favorite.product.fuel || '—' }}</span>
      </p>
      <p class="text-caption text-grey-6 q-mb-none">
        <q-icon name="location_on" size="14px" />
        {{ favorite.product.physicalStoreCity || '—' }}/{{ favorite.product.physicalStoreState || '—' }}
      </p>
    </q-card-section>
  </q-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FavoriteView } from 'src/services/api'

const props = defineProps<{ favorite: FavoriteView }>()
defineEmits<{ remove: [string]; open: [string] }>()

const coverImage = computed(() => {
  const imgs = props.favorite.product?.images ?? []
  const cover = imgs.find((i) => i.isCover) ?? imgs[0]
  return cover?.url ?? 'https://placehold.co/600x400/1a1a2e/ffffff?text=Motorise'
})

const price = computed(() => {
  const p = props.favorite.product?.price ?? 0
  if (!p) return 'Consulte'
  return p.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
})
</script>

<style scoped lang="scss">
.fav-card {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
  overflow: hidden;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
  }
}
.fav-image { background: #f5f5f5; }
.fav-title {
  font-size: 14px;
  font-weight: 700;
  margin: 0 0 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.fav-price {
  font-size: 18px;
  font-weight: 800;
  color: var(--q-primary);
  margin: 0 0 4px;
}
.rounded-borders { border-radius: 14px; }
.ellipsis-2-lines {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
