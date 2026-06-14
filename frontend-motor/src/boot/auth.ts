import { defineBoot } from '#q-app/wrappers';
import { useAuthStore } from 'src/stores/authStore';
import { useFavorites } from 'src/composables/useFavorites';
import { useMe } from 'src/composables/useMe';

/**
 * Carrega o usuário autenticado (se houver) na inicialização do app.
 * Idempotente — pode ser chamado em client e server (SSR).
 */
export default defineBoot(() => {
  const auth = useAuthStore()
  const favorites = useFavorites()
  const me = useMe()

  void auth.refreshMe().then((user) => {
    if (user) {
      void favorites.loadIds()
      void me.load().catch(() => { /* perfil/endereços opcionais */ })
    }
  })
})
