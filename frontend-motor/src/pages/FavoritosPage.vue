<template>
  <q-page class="favoritos-page container q-pa-md">
    <div class="row items-center justify-between q-mb-md">
      <h1 class="page-title q-mb-none">Meus Favoritos</h1>
      <p class="text-grey-6 q-mb-none">{{ favorites.length }} veículo(s)</p>
    </div>

    <div v-if="!auth.isAuthenticated.value" class="empty-state flex flex-center column q-py-xl text-grey-5">
      <q-icon name="favorite_border" size="80px" color="grey-3" />
      <p class="text-h6">Faça login para ver seus favoritos</p>
      <q-btn unelevated color="primary" to="/entrar" label="Entrar" />
    </div>

    <LoadingSpinner v-else-if="loading && !favorites.length" full-page />

    <div v-else-if="!favorites.length" class="empty-state flex flex-center column q-py-xl text-grey-5">
      <q-icon name="favorite_border" size="80px" color="grey-3" />
      <p class="text-h6">Nenhum favorito ainda</p>
      <p class="text-caption">Toque no coração em qualquer anúncio para salvar aqui.</p>
      <q-btn unelevated color="primary" to="/produtos" label="Explorar veículos" />
    </div>

    <div v-else class="vehicles-grid">
      <FavoriteVehicleCard
        v-for="fav in favorites"
        :key="fav.id"
        :favorite="fav"
        @remove="onRemove"
        @open="(id) => $router.push(`/produto/${id}`)"
      />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { useFavorites } from 'src/composables/useFavorites'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'
import FavoriteVehicleCard from 'components/vehicle/FavoriteVehicleCard.vue'

const $q = useQuasar()
const auth = useAuthStore()
const { favorites, loading, loadFavorites, toggle } = useFavorites()

onMounted(async () => {
  if (!auth.isAuthenticated.value) return
  await loadFavorites()
})

async function onRemove(productId: string) {
  try {
    await toggle(productId)
    $q.notify({ message: 'Removido dos favoritos.', color: 'info', position: 'bottom' })
  } catch {
    $q.notify({ message: 'Falha ao remover.', color: 'negative' })
  }
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.page-title { font-size: 1.8rem; font-weight: 800; }
.vehicles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.empty-state { text-align: center; }
</style>
