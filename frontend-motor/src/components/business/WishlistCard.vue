<template>
  <q-card
    class="wishlist-card"
    flat
    bordered
    @click="$router.push(`/produto/${vehicle.id}`)"
  >
    <q-img
      :src="vehicle.images[0]"
      :alt="vehicle.title"
      height="160px"
      fit="cover"
      class="wishlist-img"
    />
    <q-card-section class="q-pb-xs">
      <p class="wish-title ellipsis">{{ vehicle.title }}</p>
      <p class="wish-price text-primary">{{ formatPrice(vehicle.price) }}</p>
      <p class="text-caption text-grey-5">
        {{ vehicle.mileage.toLocaleString('pt-BR') }} km · {{ vehicle.year }} · {{ vehicle.location.city }}
      </p>
    </q-card-section>
    <q-card-actions class="row justify-between">
      <q-btn flat icon="notifications" label="Alertar preço" size="sm" color="grey-6" @click.stop="toggleAlert" />
      <q-btn flat icon="delete_outline" size="sm" color="negative" @click.stop="$emit('remove', vehicle.id)" />
    </q-card-actions>
  </q-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useQuasar } from 'quasar'
import type { Vehicle } from 'src/data/types'

interface Props { vehicle: Vehicle }
defineProps<Props>()
defineEmits<{ remove: [id: string] }>()

const $q = useQuasar()
const alertActive = ref(false)

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}

function toggleAlert() {
  alertActive.value = !alertActive.value
  $q.notify({
    message: alertActive.value ? 'Alerta de preço ativado!' : 'Alerta de preço desativado',
    color: alertActive.value ? 'positive' : 'grey',
    icon: alertActive.value ? 'notifications_active' : 'notifications_off',
    position: 'bottom',
    timeout: 2000,
  })
}
</script>

<style scoped lang="scss">
.wishlist-card {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease;

  &:hover { transform: translateY(-3px); }
}

.wishlist-img {
  :deep(.q-img__image) { border-radius: 0; }
}

.wish-title {
  font-weight: 600;
  font-size: 14px;
  margin: 0 0 4px;
}

.wish-price {
  font-weight: 800;
  font-size: 18px;
  margin: 0;
}

.body--dark .wishlist-card {
  .q-btn {
    color: rgba(241, 245, 255, 0.86) !important;
  }
}
</style>
