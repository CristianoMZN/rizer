<template>
  <q-page class="perfil-page">
    <div class="container q-pa-md">
      <h1 class="page-title">Meu Perfil</h1>

      <LoadingSpinner v-if="loading && !profile" full-page />

      <div v-else-if="!profile" class="empty-state text-center q-py-xl">
        <q-icon name="error_outline" size="80px" color="grey-3" />
        <p class="text-h6 text-grey-5 q-mt-md">Não foi possível carregar seu perfil</p>
        <q-btn unelevated color="primary" label="Tentar novamente" @click="loadAll" />
      </div>

      <div v-else>
        <q-tabs v-model="tab" dense class="text-primary" active-color="primary" indicator-color="primary" align="left">
          <q-tab name="pessoal" icon="person" label="Dados pessoais" />
          <q-tab name="endereco" icon="home" label="Endereço" />
          <q-tab name="conta" icon="settings" label="Conta" />
        </q-tabs>

        <q-separator />

        <q-tab-panels v-model="tab" animated class="q-mt-md">
          <!-- ───── Dados pessoais ───── -->
          <q-tab-panel name="pessoal">
            <q-card flat bordered class="rounded-borders">
              <q-card-section>
                <div class="row items-center q-gutter-md q-mb-md">
                  <q-avatar size="80px" color="primary" text-color="white">
                    <img v-if="profile.avatarUrl" :src="profile.avatarUrl" :alt="profile.name" />
                    <span v-else style="font-size: 32px">{{ initials }}</span>
                  </q-avatar>
                  <div>
                    <q-btn
                      unelevated
                      color="primary"
                      icon="photo_camera"
                      label="Trocar foto"
                      size="sm"
                      :loading="uploading"
                      @click="pickFile"
                    />
                    <input
                      ref="fileInput"
                      type="file"
                      accept="image/*"
                      class="hidden"
                      @change="onFile"
                    />
                    <p class="text-caption text-grey-6 q-mt-sm q-mb-none">
                      JPG, PNG ou WEBP até 5 MB.
                    </p>
                  </div>
                </div>

                <q-input
                  v-model="form.name"
                  label="Nome completo"
                  outlined
                  dense
                  lazy-rules
                  :rules="[(v) => !!(v && v.trim()) || 'Informe seu nome']"
                  class="q-mb-sm"
                />
                <q-input
                  v-model="profile.email"
                  label="E-mail"
                  outlined
                  dense
                  disable
                  class="q-mb-sm"
                  hint="Para trocar o e-mail, fale com o suporte."
                />
                <q-input
                  v-model="form.phone"
                  label="Celular (com DDD)"
                  outlined
                  dense
                  mask="(##) #####-####"
                  unmasked-value
                  class="q-mb-sm"
                />
                <div class="row q-gutter-sm">
                  <q-input
                    v-model="form.cpf"
                    label="CPF"
                    outlined
                    dense
                    mask="###.###.###-##"
                    unmasked-value
                    lazy-rules
                    :rules="[(v) => !v || isCpfValid(v) || 'CPF inválido']"
                    class="col"
                  />
                  <q-input
                    v-model="form.birthDate"
                    label="Data de nascimento"
                    outlined
                    dense
                    mask="##/##/####"
                    class="col"
                  >
                    <template #append>
                      <q-icon name="event" class="cursor-pointer">
                        <q-popup-proxy cover transition-show="scale" transition-hide="scale">
                          <q-date v-model="form.birthDate" mask="DD/MM/YYYY" minimal />
                        </q-popup-proxy>
                      </q-icon>
                    </template>
                  </q-input>
                </div>

                <q-btn
                  unelevated
                  color="primary"
                  icon="save"
                  label="Salvar alterações"
                  class="q-mt-md"
                  :loading="saving"
                  @click="save"
                />
              </q-card-section>
            </q-card>
          </q-tab-panel>

          <!-- ───── Endereço ───── -->
          <q-tab-panel name="endereco">
            <div class="row items-center justify-between q-mb-md">
              <p class="text-subtitle1 q-mb-none">Meus endereços</p>
              <q-btn unelevated color="primary" icon="add" label="Novo endereço" @click="openAddressDialog()" />
            </div>

            <div v-if="!addresses.length" class="empty-state text-center q-py-xl text-grey-5">
              <q-icon name="location_off" size="64px" color="grey-3" />
              <p class="text-body1">Você ainda não cadastrou nenhum endereço.</p>
            </div>

            <div v-else class="column q-gutter-md">
              <q-card v-for="addr in addresses" :key="addr.id" flat bordered class="rounded-borders">
                <q-card-section>
                  <div class="row items-start justify-between">
                    <div>
                      <p class="text-weight-bold">
                        {{ addr.label || 'Endereço' }}
                        <q-badge v-if="addr.isPrimary" color="primary" class="q-ml-sm">Principal</q-badge>
                      </p>
                      <p class="text-body2 q-mb-none">
                        {{ addr.street }}, {{ addr.number }} {{ addr.complement ? '· ' + addr.complement : '' }}
                      </p>
                      <p class="text-caption text-grey-6 q-mb-none">
                        {{ addr.neighborhood }} · {{ addr.city }}/{{ addr.state }} · {{ addr.zipCode }}
                      </p>
                    </div>
                    <div class="column q-gutter-xs items-end">
                      <q-btn flat dense icon="edit" size="sm" @click="openAddressDialog(addr)" />
                      <q-btn v-if="!addr.isPrimary" flat dense icon="star" size="sm" @click="setPrimary(addr.id)">
                        <q-tooltip>Marcar como principal</q-tooltip>
                      </q-btn>
                      <q-btn flat dense icon="delete" size="sm" color="negative" @click="removeAddress(addr.id)" />
                    </div>
                  </div>
                </q-card-section>
              </q-card>
            </div>
          </q-tab-panel>

          <!-- ───── Conta ───── -->
          <q-tab-panel name="conta">
            <q-card flat bordered class="rounded-borders q-mb-md">
              <q-card-section>
                <p class="text-subtitle1">Sessão</p>
                <p class="text-caption text-grey-6 q-mb-md">
                  Você pode sair da sua conta a qualquer momento.
                </p>
                <q-btn outline color="primary" icon="logout" label="Sair" @click="logout" />
              </q-card-section>
            </q-card>

            <q-card flat bordered class="rounded-borders q-mb-md">
              <q-card-section>
                <p class="text-subtitle1">Seus dados (LGPD)</p>
                <p class="text-caption text-grey-6 q-mb-md">
                  Você pode solicitar um export de todos os seus dados pessoais ou pedir a exclusão da conta.
                </p>
                <q-btn outline color="primary" icon="download" label="Exportar meus dados" :loading="exporting" @click="exportData" class="q-mr-sm" />
                <q-btn outline color="negative" icon="delete_forever" label="Excluir minha conta" @click="confirmDelete" />
              </q-card-section>
            </q-card>
          </q-tab-panel>
        </q-tab-panels>
      </div>
    </div>

    <!-- Modal de endereço -->
    <q-dialog v-model="addressDialog">
      <q-card style="min-width: 480px; max-width: 600px">
        <q-card-section>
          <p class="text-h6 q-mb-none">{{ editingAddress ? 'Editar' : 'Novo' }} endereço</p>
        </q-card-section>
        <q-card-section class="q-gutter-sm">
          <q-input v-model="addressForm.label" label="Apelido (ex: casa, trabalho)" outlined dense />
          <div class="row q-gutter-sm">
            <q-input v-model="addressForm.zipCode" label="CEP" outlined dense mask="#####-###" class="col-3" @blur="lookupCepDialog">
              <template #append>
                <q-icon v-if="loadingCep" name="hourglass_empty" size="sm" />
              </template>
            </q-input>
            <q-input v-model="addressForm.street" label="Rua" outlined dense class="col" />
          </div>
          <div class="row q-gutter-sm">
            <q-input v-model="addressForm.number" label="Número" outlined dense class="col-2" />
            <q-input v-model="addressForm.complement" label="Complemento" outlined dense class="col" />
          </div>
          <q-input v-model="addressForm.neighborhood" label="Bairro" outlined dense />
          <div class="row q-gutter-sm">
            <q-input v-model="addressForm.city" label="Cidade" outlined dense class="col" />
            <q-input v-model="addressForm.state" label="UF" outlined dense class="col-2" maxlength="2" />
          </div>
          <q-checkbox v-model="addressForm.isPrimary" label="Marcar como principal" />
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn unelevated color="primary" label="Salvar" :loading="savingAddress" @click="saveAddress" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useMe } from 'src/composables/useMe'
import { useAuthStore } from 'src/stores/authStore'
import { useFavorites } from 'src/composables/useFavorites'
import { mediaApi, type AddressView, lgpdApi, MOCK_CONFIG } from 'src/services/api'
import { api as mockApi } from 'src/services/apiMock'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()
const favorites = useFavorites()
const me = useMe()

const tab = ref<'pessoal' | 'endereco' | 'conta'>('pessoal')
const loading = computed(() => me.loading.value)
const profile = computed(() => me.profile.value)
const addresses = computed(() => me.addresses.value)

const form = reactive({
  name: '',
  phone: '',
  cpf: '',
  birthDate: '',
})
const saving = ref(false)
const uploading = ref(false)
const exporting = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

const addressDialog = ref(false)
const editingAddress = ref<AddressView | null>(null)
const savingAddress = ref(false)
const loadingCep = ref(false)
const addressForm = reactive({
  label: '',
  zipCode: '',
  street: '',
  number: '',
  complement: '',
  neighborhood: '',
  city: '',
  state: '',
  countryCode: 'BR',
  country: 'Brasil',
  isPrimary: false,
})

const initials = computed(() => {
  if (!profile.value) return 'U'
  return profile.value.name
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('')
})

onMounted(async () => {
  await loadAll()
  if (profile.value) {
    form.name = profile.value.name
    form.phone = profile.value.phone ?? ''
    form.cpf = profile.value.cpf ?? ''
    form.birthDate = profile.value.birthDate ? formatBrDate(profile.value.birthDate) : ''
  }
})

async function loadAll() {
  await me.load()
}

function isCpfValid(cpf: string): boolean {
  if (!cpf) return true
  const d = cpf.replace(/\D/g, '')
  if (d.length !== 11) return false
  if (/^(\d)\1+$/.test(d)) return false
  const calc = (max: number) => {
    let sum = 0
    for (let i = 0; i < max; i++) sum += parseInt(d[i]!, 10) * (max + 1 - i)
    const rest = (sum * 10) % 11
    return rest === 10 ? 0 : rest
  }
  return calc(9) === parseInt(d[9]!, 10) && calc(10) === parseInt(d[10]!, 10)
}

function formatBrDate(iso: string): string {
  if (!iso) return ''
  const [y, m, d] = iso.split('-')
  return `${d}/${m}/${y}`
}

function formatApiDate(value: string): string | undefined {
  if (!value) return undefined
  const m = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!m) return undefined
  return `${m[3]}-${m[2]}-${m[1]}`
}

async function save() {
  if (!profile.value) return
  saving.value = true
  try {
    const cpf = form.cpf
      ? form.cpf.replace(/\D/g, '').length === 11
        ? `${form.cpf.replace(/\D/g, '').slice(0, 3)}.${form.cpf.replace(/\D/g, '').slice(3, 6)}.${form.cpf.replace(/\D/g, '').slice(6, 9)}-${form.cpf.replace(/\D/g, '').slice(9, 11)}`
        : form.cpf
      : ''
    const birthDate = formatApiDate(form.birthDate) ?? ''
    const payload: { name: string; phone: string; cpf: string; birthDate: string } = {
      name: form.name.trim(),
      phone: form.phone,
      cpf,
      birthDate,
    }
    await me.updateProfile(payload)
    $q.notify({ message: 'Perfil atualizado com sucesso.', color: 'positive', position: 'top' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail ?? 'Não foi possível salvar.', color: 'negative', position: 'top' })
  } finally {
    saving.value = false
  }
}

function pickFile() {
  fileInput.value?.click()
}

async function onFile(ev: Event) {
  const target = ev.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    $q.notify({ message: 'Arquivo muito grande (máx 5 MB).', color: 'warning' })
    return
  }
  uploading.value = true
  try {
    const up = await mediaApi.uploadImage(file, 'user-avatar')
    await me.updateProfile({ avatarUrl: up.url })
    $q.notify({ message: 'Foto atualizada.', color: 'positive', position: 'top' })
  } catch {
    $q.notify({ message: 'Falha no upload.', color: 'negative' })
  } finally {
    uploading.value = false
    target.value = ''
  }
}

function openAddressDialog(addr?: AddressView) {
  if (addr) {
    editingAddress.value = addr
    Object.assign(addressForm, {
      label: addr.label ?? '',
      zipCode: addr.zipCode ?? '',
      street: addr.street,
      number: addr.number ?? '',
      complement: addr.complement ?? '',
      neighborhood: addr.neighborhood ?? '',
      city: addr.city,
      state: addr.state,
      countryCode: addr.countryCode,
      country: addr.country ?? 'Brasil',
      isPrimary: addr.isPrimary,
    })
  } else {
    editingAddress.value = null
    Object.assign(addressForm, {
      label: '', zipCode: '', street: '', number: '', complement: '',
      neighborhood: '', city: '', state: '',
      countryCode: 'BR', country: 'Brasil',
      isPrimary: addresses.value.length === 0,
    })
  }
  addressDialog.value = true
}

async function lookupCepDialog() {
  if (!addressForm.zipCode || addressForm.zipCode.replace(/\D/g, '').length !== 8) return
  loadingCep.value = true
  try {
    const r = await mockApi.lookupCep(addressForm.zipCode)
    if (r) {
      addressForm.street = r.street ?? addressForm.street
      addressForm.neighborhood = r.neighborhood ?? addressForm.neighborhood
      addressForm.city = r.city ?? addressForm.city
      addressForm.state = r.state ?? addressForm.state
    }
  } finally {
    loadingCep.value = false
  }
}

async function saveAddress() {
  if (!addressForm.street || !addressForm.city || !addressForm.state) {
    $q.notify({ message: 'Preencha rua, cidade e UF.', color: 'warning' })
    return
  }
  savingAddress.value = true
  try {
    if (editingAddress.value) {
      await me.updateAddress(editingAddress.value.id, { ...addressForm })
    } else {
      await me.addAddress({ ...addressForm })
    }
    addressDialog.value = false
    $q.notify({ message: 'Endereço salvo.', color: 'positive', position: 'top' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail ?? 'Falha ao salvar endereço.', color: 'negative' })
  } finally {
    savingAddress.value = false
  }
}

async function setPrimary(id: string) {
  try {
    await me.setPrimary(id)
    $q.notify({ message: 'Endereço principal atualizado.', color: 'positive', position: 'top' })
  } catch {
    $q.notify({ message: 'Falha ao atualizar.', color: 'negative' })
  }
}

function removeAddress(id: string) {
  $q.dialog({
    title: 'Remover endereço?',
    message: 'Esta ação não pode ser desfeita.',
    cancel: true,
  }).onOk(() => {
    void (async () => {
      try {
        await me.removeAddress(id)
        $q.notify({ message: 'Endereço removido.', color: 'positive', position: 'top' })
      } catch {
        $q.notify({ message: 'Falha ao remover.', color: 'negative' })
      }
    })()
  })
}

async function exportData() {
  if (!MOCK_CONFIG.useBackend) {
    $q.notify({ message: 'Backend desabilitado.', color: 'warning' })
    return
  }
  exporting.value = true
  try {
    await lgpdApi.requestDataExport()
    $q.notify({
      message: 'Solicitação registrada. Você receberá um e-mail quando estiver pronta.',
      color: 'positive',
      position: 'top',
    })
  } catch {
    $q.notify({ message: 'Falha ao solicitar export.', color: 'negative' })
  } finally {
    exporting.value = false
  }
}

function confirmDelete() {
  $q.dialog({
    title: 'Excluir conta?',
    message: 'Seus dados serão anonimizados e a conta será removida em 30 dias.',
    cancel: true,
    persistent: true,
  }).onOk(() => {
    void (async () => {
      if (!MOCK_CONFIG.useBackend) {
        $q.notify({ message: 'Backend desabilitado.', color: 'warning' })
        return
      }
      try {
        await lgpdApi.deleteAccount()
        await auth.logout()
        favorites.reset()
        me.reset()
        void router.push('/')
      } catch {
        $q.notify({ message: 'Falha ao excluir conta.', color: 'negative' })
      }
    })()
  })
}

async function logout() {
  try {
    await auth.logout()
  } finally {
    favorites.reset()
    me.reset()
    void router.push('/')
  }
}
</script>

<style scoped lang="scss">
.container { max-width: 880px; margin: 0 auto; }
.page-title { font-size: 1.8rem; font-weight: 800; margin: 0 0 16px; }
.empty-state { color: #9e9e9e; }
.rounded-borders { border-radius: 16px; }
.hidden { display: none; }
</style>
