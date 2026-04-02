<template>
  <q-card
    class="vehicle-card"
    :class="{ 'vehicle-card--featured': vehicle.featured }"
    flat
    bordered
    @click="$emit('click', vehicle)"
  >
    <!-- Image carousel -->
    <q-carousel
      v-model="activeSlide"
      animated
      arrows
      infinite
      height="200px"
      class="card-carousel"
      @click.stop
    >
      <q-carousel-slide
        v-for="(img, idx) in vehicle.images"
        :key="idx"
        :name="idx"
        :img-src="img"
      >
        <div class="carousel-badges absolute-top-left q-pa-sm row q-gutter-xs">
          <q-badge v-if="vehicle.featured" color="amber" label="Destaque" />
          <q-badge v-if="vehicle.verification.wasRental" color="orange" label="Locadora" />
        </div>
      </q-carousel-slide>
    </q-carousel>

    <!-- Wishlist button -->
    <q-btn
      fab-mini
      flat
      :icon="isWishlisted ? 'favorite' : 'favorite_border'"
      :color="isWishlisted ? 'red' : 'grey-5'"
      class="wishlist-btn"
      :aria-label="isWishlisted ? 'Remover dos favoritos' : 'Adicionar aos favoritos'"
      @click.stop="$emit('toggle-wishlist', vehicle.id)"
    />

    <q-card-section class="q-pb-xs">
      <div class="row items-start justify-between no-wrap">
        <div class="col">
          <p class="card-title ellipsis">{{ vehicle.title }}</p>
          <p class="card-price">{{ formatPrice(vehicle.price) }}</p>
          <p v-if="vehicle.fipePrice" class="card-fipe text-caption">
            FIPE: {{ formatPrice(vehicle.fipePrice) }}
            <q-badge
              :color="vehicle.price <= vehicle.fipePrice ? 'positive' : 'warning'"
              :label="priceDiff"
            />
          </p>
        </div>
      </div>
    </q-card-section>

    <q-card-section class="q-pt-xs">
      <div class="row q-gutter-xs vehicle-specs">
        <q-chip dense outline size="sm" icon="speed" :label="`${vehicle.mileage.toLocaleString('pt-BR')} km`" />
        <q-chip dense outline size="sm" icon="local_gas_station" :label="vehicle.fuel" />
        <q-chip dense outline size="sm" icon="settings" :label="vehicle.transmission" />
        <q-chip dense outline size="sm" icon="location_on" :label="`${vehicle.location.city}/${vehicle.location.state}`" />
      </div>
    </q-card-section>

    <q-separator />

    <q-card-actions class="q-pa-sm">
      <div class="row items-center full-width">
        <q-avatar size="24px" color="primary" text-color="white" class="q-mr-xs">
          {{ vehicle.store.name.charAt(0) }}
        </q-avatar>
        <span class="text-caption text-grey-6 ellipsis col">{{ vehicle.store.name }}</span>
        <q-btn
          flat
          color="primary"
          label="Ver detalhes"
          size="sm"
          :to="`/produto/${vehicle.id}`"
          @click.stop
        />
      </div>
    </q-card-actions>
  </q-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Vehicle } from 'src/data/types'

interface Props {
  vehicle: Vehicle
  isWishlisted?: boolean
}

const props = defineProps<Props>()

defineEmits<{
  click: [vehicle: Vehicle]
  'toggle-wishlist': [id: string]
}>()

const activeSlide = ref(0)

const priceDiff = computed(() => {
  if (!props.vehicle.fipePrice) return ''
  const diff = props.vehicle.price - props.vehicle.fipePrice
  const pct = Math.abs(Math.round((diff / props.vehicle.fipePrice) * 100))
  return diff <= 0 ? `-${pct}%` : `+${pct}%`
})

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.vehicle-card {
  border-radius: 16px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  cursor: pointer;
  position: relative;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12);
  }

  &--featured {
    border-color: #ffd740;
  }
}

.card-carousel {
  :deep(.q-carousel__slide) {
    background-size: cover;
    background-position: center;
  }
}

.wishlist-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
  z-index: 2;
}

.card-title {
  font-weight: 600;
  font-size: 14px;
  margin: 0 0 4px;
}

.card-price {
  font-size: 20px;
  font-weight: 800;
  color: var(--q-primary);
  margin: 0;
}

.card-fipe {
  color: #9e9e9e;
  margin: 2px 0 0;
}

.vehicle-specs {
  :deep(.q-chip) {
    font-size: 11px;
  }
}
</style>
