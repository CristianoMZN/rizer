import { defineBoot } from '#q-app/wrappers';
import axios, { type AxiosInstance } from 'axios';
import { getTenantScope } from 'src/composables/useTenant';

declare module 'vue' {
  interface ComponentCustomProperties {
    $axios: AxiosInstance;
    $api: AxiosInstance;
  }
}

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' });

// ─── Interceptor de tenant ────────────────────────────────────────────────────
// Injeta automaticamente o header X-Tenant-Slug em todas as requisições
// quando o usuário está navegando em um domínio de loja (modo store).
// O backend usa esse header para escopar queries ao tenant.

api.interceptors.request.use((config) => {
  const scope = getTenantScope();
  if (scope.mode === 'store') {
    config.headers['X-Tenant-Slug'] = scope.storeSlug;
  }
  return config;
});

export default defineBoot(({ app }) => {
  app.config.globalProperties.$axios = axios;
  app.config.globalProperties.$api = api;
});

export { api };
