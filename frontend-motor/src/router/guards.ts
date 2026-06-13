import type { NavigationGuard } from 'vue-router';
import { useAuthStore } from 'src/stores/authStore';

/**
 * Guard global de autenticação/role.
 *
 * - Se a rota pede `requiresAuth` e o usuário não está logado → /entrar?redirect=...
 * - Se a rota pede `requiredRoles` e o system_role não bate → /
 * - Aguarda o carregamento inicial do `/auth/me` (boot/auth) antes de decidir.
 */
export const authGuard: NavigationGuard = async (to) => {
  const auth = useAuthStore()

  if (!auth.initialized.value) {
    await auth.refreshMe()
  }

  const meta = to.meta
  if (meta.requiresAuth && !auth.isAuthenticated.value) {
    return { path: '/entrar', query: { redirect: to.fullPath } }
  }
  if (meta.requiredRoles && auth.user.value) {
    const role = auth.user.value.systemRole
    if (!meta.requiredRoles.includes(role)) {
      return { path: '/' }
    }
  }
  return true
}
