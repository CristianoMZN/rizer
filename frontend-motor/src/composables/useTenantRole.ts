import { computed } from 'vue'
import { useAuthStore } from 'src/stores/authStore'

/**
 * Papel atual do usuário dentro do tenant selecionado.
 * OWNER > MANAGER > SELLER (ranking de permissões).
 */
export type TenantRole = 'OWNER' | 'MANAGER' | 'SELLER'

export function useTenantRole() {
  const auth = useAuthStore()
  const role = computed<TenantRole | null>(() => auth.currentMembership.value?.role ?? null)

  const isOwner = computed(() => role.value === 'OWNER')
  const isManagerOrOwner = computed(() => role.value === 'OWNER' || role.value === 'MANAGER')
  const isSeller = computed(() => role.value === 'SELLER')

  /** OWNER/MANAGER podem publicar anúncios. SELLER só salva rascunho. */
  const canPublish = computed(() => isManagerOrOwner.value)
  /** OWNER/MANAGER podem marcar como vendido / arquivar. */
  const canMarkSold = computed(() => isManagerOrOwner.value)
  /** Apenas OWNER pode editar o perfil do tenant (CNPJ, razão social, etc.). */
  const canManageTenant = computed(() => isOwner.value)
  /** Apenas OWNER pode convidar/remover membros. */
  const canInviteMembers = computed(() => isOwner.value)

  return {
    role,
    isOwner,
    isManagerOrOwner,
    isSeller,
    canPublish,
    canMarkSold,
    canManageTenant,
    canInviteMembers,
  }
}
