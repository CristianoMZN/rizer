import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';
import { getTenantScope } from 'src/composables/useTenant';
import { bindBootApi } from 'src/stores/authStore';

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api', withCredentials: true });

// ─── Interceptor de tenant ────────────────────────────────────────────────────
// Injeta automaticamente o header X-Tenant-Slug quando o usuário está
// navegando em um domínio de loja (modo store).

api.interceptors.request.use((config) => {
  const scope = getTenantScope();
  if (scope.mode === 'store') {
    config.headers['X-Tenant-Slug'] = scope.storeSlug;
  }
  return config;
});

// ─── Interceptor de auth ──────────────────────────────────────────────────────
// Lê o access token do cookie HttpOnly (motorise_access) e injeta
// o header Authorization. Como o cookie é HttpOnly, o JS não consegue
// ler o valor — mas o navegador envia automaticamente em cada request.
// Para chamadas client-side feitas via fetch/axios a partir de JS,
// o backend aceita tanto o cookie quanto o header. Aqui só adicionamos
// o header Authorization se houver um token em cookie legível
// (no caso de uso de cookie não-HttpOnly em dev).

api.interceptors.request.use((config) => {
  if (typeof document !== 'undefined' && config.headers) {
    const match = document.cookie.match(/(?:^|; )motorise_access_token=([^;]*)/)
    if (match && match[1]) {
      config.headers['Authorization'] = `Bearer ${decodeURIComponent(match[1])}`
    }
  }
  return config
})

// ─── Interceptor de erro ──────────────────────────────────────────────────────
// 401 → tenta refresh; 402 → billing; outros → propaga.

let refreshing: Promise<string | null> | null = null

  async function tryRefresh(): Promise<string | null> {
    if (refreshing) return refreshing
    refreshing = (async (): Promise<string | null> => {
      try {
        await axios.post(
          `${import.meta.env.VITE_API_URL || '/api'}/auth/login/refresh`,
          null,
          { withCredentials: true }
        )
        return 'refreshed'
      } catch {
        return null
      } finally {
        refreshing = null
      }
    })()
    return refreshing
  }

api.interceptors.response.use(
  (r) => r,
  async (error) => {
    if (error?.response?.status === 401 && !error.config?._retried) {
      const ok = await tryRefresh()
      if (ok) {
        error.config._retried = true
        return api.request(error.config)
      }
    }
    return Promise.reject(error instanceof Error ? error : new Error(String(error)))
  }
)

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
  bindBootApi(api)
});

export { api };
