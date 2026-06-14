import { ref } from 'vue'
import { utilApi, type CepLookupView } from 'src/services/api'

// ─── Singleton reativo (cache local) ─────────────────────────────────────────

const _loading = ref(false)
const _error = ref<string | null>(null)

const _cache = new Map<string, CepLookupView>()

export function useCepLookup() {
  async function lookup(rawCep: string): Promise<CepLookupView | null> {
    const cep = (rawCep || '').replace(/\D/g, '')
    if (cep.length !== 8) return null
    const cached = _cache.get(cep)
    if (cached) return cached
    _loading.value = true
    _error.value = null
    try {
      const v = await utilApi.cepLookup(cep)
      if (v) _cache.set(cep, v)
      return v
    } catch (e: unknown) {
      const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      _error.value = detail || 'Falha ao consultar CEP'
      return null
    } finally {
      _loading.value = false
    }
  }

  return { lookup, loading: _loading, error: _error }
}
