import { ref, computed } from 'vue'
import { meApi, type AddressView, type ConsumerProfile, type AddressInput } from 'src/services/api'

const profile = ref<ConsumerProfile | null>(null)
const addresses = ref<AddressView[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const [p, a] = await Promise.all([meApi.getProfile(), meApi.listAddresses()])
    profile.value = p
    addresses.value = a
  } finally {
    loading.value = false
  }
}

async function updateProfile(patch: Partial<{
  name: string; phone: string; cpf: string; birthDate: string; avatarUrl: string
}>): Promise<ConsumerProfile> {
  const updated = await meApi.updateProfile(patch)
  profile.value = updated
  return updated
}

async function addAddress(input: AddressInput): Promise<AddressView> {
  const created = await meApi.createAddress(input)
  if (created.isPrimary) {
    addresses.value = addresses.value.map((a) => ({ ...a, isPrimary: false }))
  }
  addresses.value = [...addresses.value, created]
  return created
}

async function updateAddress(id: string, patch: Partial<AddressInput>): Promise<AddressView> {
  const updated = await meApi.updateAddress(id, patch)
  if (updated.isPrimary) {
    addresses.value = addresses.value.map((a) => (a.id === id ? updated : { ...a, isPrimary: false }))
  } else {
    addresses.value = addresses.value.map((a) => (a.id === id ? updated : a))
  }
  return updated
}

async function removeAddress(id: string): Promise<void> {
  await meApi.deleteAddress(id)
  addresses.value = addresses.value.filter((a) => a.id !== id)
}

async function setPrimary(id: string): Promise<AddressView> {
  const updated = await meApi.setPrimaryAddress(id)
  addresses.value = addresses.value.map((a) => (a.id === id ? updated : { ...a, isPrimary: false }))
  return updated
}

function reset() {
  profile.value = null
  addresses.value = []
}

export function useMe() {
  const primaryAddress = computed(() => addresses.value.find((a) => a.isPrimary) ?? addresses.value[0] ?? null)
  return {
    profile,
    addresses,
    primaryAddress,
    loading,
    load,
    updateProfile,
    addAddress,
    updateAddress,
    removeAddress,
    setPrimary,
    reset,
  }
}
