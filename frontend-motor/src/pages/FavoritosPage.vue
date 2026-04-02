<template>
  <q-page class="favoritos-page container q-pa-md">
    <h1 class="page-title">Meus Favoritos</h1>

    <LoadingSpinner v-if="loading" full-page />

    <div v-else-if="!wishlisted.length" class="flex flex-center column q-py-xl text-grey-5">
      <q-icon name="favorite_border" size="80px" color="grey-3" />
      <p class="text-h6">Nenhum favorito ainda</p>
      <q-btn unelevated color="primary" to="/produtos">Explorar veículos</q-btn>
    </div>

    <div v-else class="vehicles-grid">
      <WishlistCard
        v-for="vehicle in wishlisted"
        :key="vehicle.id"
        :vehicle="vehicle"
        @remove="removeFromWishlist"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { Vehicle } from 'src/data/types'
import { api } from 'src/services/api'
import { MOCK_USER } from 'src/data/mock'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'
import WishlistCard from 'components/business/WishlistCard.vue'

const loading = ref(true)
const wishlisted = ref<Vehicle[]>([])

onMounted(async () => {
  const ids = MOCK_USER.wishlist
  const all = await api.getVehicles()
  wishlisted.value = all.filter((v) => ids.includes(v.id))
  loading.value = false
})

function removeFromWishlist(id: string) {
  wishlisted.value = wishlisted.value.filter((v) => v.id !== id)
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.page-title { font-size: 2rem; font-weight: 800; margin-bottom: 24px; }
.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
}
</style>
