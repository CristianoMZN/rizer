import { ref, computed } from 'vue'
import { meApi, type FavoriteView } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const ids = ref<Set<string>>(new Set())
const favorites = ref<FavoriteView[]>([])
const loaded = ref(false)
const loading = ref(false)

async function loadIds() {
  const auth = useAuthStore()
  if (!auth.isAuthenticated.value) {
    ids.value = new Set()
    favorites.value = []
    loaded.value = false
    return
  }
  try {
    const list = await meApi.listFavoriteIds()
    ids.value = new Set(list)
    loaded.value = true
  } catch {
    ids.value = new Set()
  }
}

async function loadFavorites() {
  const auth = useAuthStore()
  if (!auth.isAuthenticated.value) {
    favorites.value = []
    return
  }
  loading.value = true
  try {
    favorites.value = await meApi.listFavorites()
    ids.value = new Set(favorites.value.map((f) => f.productId))
    loaded.value = true
  } finally {
    loading.value = false
  }
}

async function toggle(productId: string): Promise<boolean> {
  const auth = useAuthStore()
  if (!auth.isAuthenticated.value) {
    throw new Error('Faça login para favoritar')
  }
  const wasFav = ids.value.has(productId)
  if (wasFav) {
    ids.value.delete(productId)
    favorites.value = favorites.value.filter((f) => f.productId !== productId)
    try {
      await meApi.removeFavorite(productId)
    } catch (e) {
      ids.value.add(productId)
      throw e
    }
    return false
  }
  ids.value.add(productId)
  try {
    await meApi.addFavorite(productId)
  } catch (e) {
    ids.value.delete(productId)
    throw e
  }
  return true
}

function isFavorite(productId: string): boolean {
  return ids.value.has(productId)
}

function reset() {
  ids.value = new Set()
  favorites.value = []
  loaded.value = false
}

export function useFavorites() {
  const count = computed(() => ids.value.size)
  return {
    ids,
    favorites,
    loading,
    loaded,
    count,
    loadIds,
    loadFavorites,
    toggle,
    isFavorite,
    reset,
  }
}
