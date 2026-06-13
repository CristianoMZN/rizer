import { defineBoot } from '#q-app/wrappers';
import { useAuthStore } from 'src/stores/authStore';

/**
 * Carrega o usuário autenticado (se houver) na inicialização do app.
 * Idempotente — pode ser chamado em client e server (SSR).
 */
export default defineBoot(() => {
  const auth = useAuthStore()
  void auth.refreshMe()
})
