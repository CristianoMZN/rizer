import { ref } from 'vue'

export interface Coords {
  latitude: number
  longitude: number
}

const _loading = ref(false)
const _error = ref<string | null>(null)

export function useGeolocation() {
  function getCurrent(): Promise<Coords | null> {
    if (typeof navigator === 'undefined' || !('geolocation' in navigator)) {
      _error.value = 'Geolocalização indisponível neste navegador'
      return Promise.resolve(null)
    }
    _loading.value = true
    _error.value = null
    return new Promise((resolve) => {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          _loading.value = false
          resolve({ latitude: pos.coords.latitude, longitude: pos.coords.longitude })
        },
        (err) => {
          _loading.value = false
          _error.value = err.message || 'Permissão negada'
          resolve(null)
        },
        { enableHighAccuracy: false, timeout: 8000, maximumAge: 60_000 }
      )
    })
  }

  return { getCurrent, loading: _loading, error: _error }
}
