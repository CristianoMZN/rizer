<template>
  <q-page class="registro-page">
    <div class="container q-pa-md">
      <q-card flat bordered class="registro-card">
        <q-card-section class="text-center q-pb-none">
          <p class="reg-logo">Motorise</p>
          <p class="reg-title">Crie sua conta de consumidor</p>
          <p class="reg-sub text-grey-6">
            Salve favoritos, acompanhe buscas e fale direto com lojas parceiras.
          </p>
        </q-card-section>

        <q-card-section class="q-pa-lg">
          <q-form @submit.prevent="onSubmit" autofocus greedy>
            <p class="section-label">Dados obrigatórios</p>
            <q-input
              v-model="form.name"
              label="Nome completo *"
              outlined
              dense
              lazy-rules
              :rules="[(v) => !!(v && v.trim()) || 'Informe seu nome']"
              class="q-mb-sm"
            />
            <q-input
              v-model="form.email"
              label="E-mail *"
              type="email"
              outlined
              dense
              lazy-rules
              :rules="[(v) => /.+@.+\..+/.test(v || '') || 'E-mail inválido']"
              class="q-mb-sm"
            />
            <q-input
              v-model="form.phone"
              label="Celular (com DDD) *"
              outlined
              dense
              mask="(##) #####-####"
              unmasked-value
              lazy-rules
              :rules="[(v) => (v && v.replace(/\D/g, '').length >= 10) || 'Informe o celular com DDD']"
              class="q-mb-sm"
            />
            <div class="row q-gutter-sm q-mb-md">
              <q-input
                v-model="form.password"
                label="Senha *"
                :type="showPass ? 'text' : 'password'"
                outlined
                dense
                lazy-rules
                :rules="[
                  (v) => !!(v && v.length >= 8) || 'Mínimo 8 caracteres',
                  (v) => /[A-Za-z]/.test(v || '') && /\d/.test(v || '') || 'Use letras e números',
                ]"
                class="col"
              >
                <template #append>
                  <q-icon
                    :name="showPass ? 'visibility_off' : 'visibility'"
                    class="cursor-pointer"
                    @click="showPass = !showPass"
                  />
                </template>
              </q-input>
              <q-input
                v-model="form.passwordConfirmation"
                label="Confirmar senha *"
                :type="showPass ? 'text' : 'password'"
                outlined
                dense
                lazy-rules
                :rules="[(v) => v === form.password || 'Senhas não conferem']"
                class="col"
              />
            </div>

            <q-separator class="q-my-md" />
            <p class="section-label">Opcionais</p>

            <q-expansion-item
              v-model="showOptional"
              icon="add"
              label="Adicionar dados opcionais (CPF, data de nascimento, endereço)"
              header-class="text-primary"
              class="q-mb-sm"
            >
              <div class="q-pa-md q-gutter-sm">
                <div class="row q-gutter-sm">
                  <q-input
                    v-model="form.cpf"
                    label="CPF"
                    outlined
                    dense
                    mask="###.###.###-##"
                    unmasked-value
                    lazy-rules
                    :rules="[
                      (v) => !v || isCpfValid(v) || 'CPF inválido',
                    ]"
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

                <q-separator class="q-my-sm" />
                <p class="text-caption text-grey-6">Endereço (opcional)</p>
                <div class="row q-gutter-sm">
                  <q-input
                    v-model="address.zipCode"
                    label="CEP"
                    outlined
                    dense
                    mask="#####-###"
                    class="col-3"
                    @blur="lookupCep"
                  >
                    <template #append>
                      <q-icon v-if="loadingCep" name="hourglass_empty" size="sm" />
                    </template>
                  </q-input>
                  <q-input
                    v-model="address.street"
                    label="Rua"
                    outlined
                    dense
                    class="col"
                  />
                </div>
                <div class="row q-gutter-sm">
                  <q-input
                    v-model="address.number"
                    label="Número"
                    outlined
                    dense
                    class="col-2"
                  />
                  <q-input
                    v-model="address.complement"
                    label="Complemento"
                    outlined
                    dense
                    class="col"
                  />
                </div>
                <q-input
                  v-model="address.neighborhood"
                  label="Bairro"
                  outlined
                  dense
                />
                <div class="row q-gutter-sm">
                  <q-input
                    v-model="address.city"
                    label="Cidade"
                    outlined
                    dense
                    class="col"
                  />
                  <q-input
                    v-model="address.state"
                    label="UF"
                    outlined
                    dense
                    class="col-2"
                    maxlength="2"
                  />
                </div>
              </div>
            </q-expansion-item>

            <q-checkbox
              v-model="form.acceptTerms"
              class="q-mt-md"
              :rules="[(v: boolean) => v || 'Aceite os termos para continuar']"
            >
              <span class="text-caption">
                Li e aceito os
                <router-link to="/legal/termos-de-uso" target="_blank" class="text-primary">Termos de Uso</router-link>
                e a
                <router-link to="/legal/politica-de-privacidade" target="_blank" class="text-primary">Política de Privacidade</router-link>
              </span>
            </q-checkbox>

            <q-btn
              type="submit"
              unelevated
              color="primary"
              label="Criar conta"
              full-width
              size="md"
              :loading="loading"
              class="q-mt-md"
            />

            <q-separator class="q-my-md">
              <q-icon name="or" />
            </q-separator>

            <q-btn
              outline
              full-width
              icon="img:https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
              label="Continuar com Google"
              color="grey-9"
              :loading="loadingGoogle"
              @click="onGoogle"
            />
            <q-btn
              outline
              full-width
              icon="img:https://static.xx.fbcdn.net/rsrc.php/yv/r/h8Eun16b4M5.svg"
              label="Continuar com Facebook"
              color="primary"
              class="q-mt-sm"
              :loading="loadingFacebook"
              @click="onFacebook"
            />

            <div v-if="!hasBackend" class="q-mt-md text-center text-caption text-grey-6">
              Backend desabilitado. Defina <code>VITE_API_URL</code> e ligue
              <code>MOCK_CONFIG.useBackend = true</code> em <code>src/services/api.ts</code>.
            </div>
          </q-form>
        </q-card-section>

        <q-card-actions class="justify-center q-pb-lg q-pt-none">
          <span class="text-grey-6 text-caption">Já tem conta?</span>
          <q-btn flat color="primary" label="Entrar" to="/entrar" />
        </q-card-actions>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { MOCK_CONFIG } from 'src/services/api'
import { TERMS_VERSION, PRIVACY_VERSION } from 'src/data/legalVersions'
import { api } from 'src/services/apiMock'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()

const showPass = ref(false)
const showOptional = ref(false)
const loading = ref(false)
const loadingGoogle = ref(false)
const loadingFacebook = ref(false)
const loadingCep = ref(false)
const hasBackend = computed(() => MOCK_CONFIG.useBackend)

const form = reactive({
  name: '',
  email: '',
  phone: '',
  password: '',
  passwordConfirmation: '',
  cpf: '',
  birthDate: '',
  acceptTerms: false,
})

const address = reactive({
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
})

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

async function lookupCep() {
  if (!address.zipCode || address.zipCode.replace(/\D/g, '').length !== 8) return
  loadingCep.value = true
  try {
    const r = await api.lookupCep(address.zipCode)
    if (r) {
      address.street = r.street ?? address.street
      address.neighborhood = r.neighborhood ?? address.neighborhood
      address.city = r.city ?? address.city
      address.state = r.state ?? address.state
    }
  } finally {
    loadingCep.value = false
  }
}

function formatBirthDateForApi(value: string): string | undefined {
  if (!value) return undefined
  const m = value.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (!m) return undefined
  return `${m[3]}-${m[2]}-${m[1]}`
}

function formatCpfForApi(value: string): string | undefined {
  if (!value) return undefined
  const d = value.replace(/\D/g, '')
  if (d.length !== 11) return undefined
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9, 11)}`
}

async function onSubmit() {
  if (!hasBackend.value) {
    $q.notify({ message: 'Backend desabilitado.', color: 'warning' })
    return
  }
  if (!form.acceptTerms) {
    $q.notify({ message: 'Aceite os termos para continuar.', color: 'warning' })
    return
  }
  loading.value = true
  try {
    await auth.register({
      name: form.name.trim(),
      email: form.email.trim().toLowerCase(),
      phone: form.phone.replace(/\D/g, ''),
      password: form.password,
      passwordConfirmation: form.passwordConfirmation,
      cpf: formatCpfForApi(form.cpf) ?? '',
      birthDate: formatBirthDateForApi(form.birthDate) ?? '',
      termsVersion: TERMS_VERSION,
      privacyVersion: PRIVACY_VERSION,
    })
    $q.notify({
      message: `Bem-vindo, ${auth.user.value?.name?.split(' ')[0] ?? ''}!`,
      color: 'positive',
      position: 'top',
    })
    void router.push('/')
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number; data?: { detail?: string } } })?.response?.status
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    let msg = 'Não foi possível criar a conta. Tente novamente.'
    if (status === 409) msg = detail ?? 'E-mail ou CPF já cadastrado.'
    else if (status === 400) msg = detail ?? 'Dados inválidos.'
    $q.notify({ message: msg, color: 'negative', position: 'top' })
  } finally {
    loading.value = false
  }
}

async function onGoogle() {
  if (!hasBackend.value) {
    $q.notify({ message: 'Backend desabilitado.', color: 'warning' })
    return
  }
  loadingGoogle.value = true
  try {
    await auth.loginWithGoogle()
  } catch {
    loadingGoogle.value = false
  }
}

async function onFacebook() {
  if (!hasBackend.value) {
    $q.notify({ message: 'Backend desabilitado.', color: 'warning' })
    return
  }
  loadingFacebook.value = true
  try {
    await auth.loginWithFacebook()
    $q.notify({ message: `Bem-vindo, ${auth.user.value?.name?.split(' ')[0] ?? ''}!`, color: 'positive', position: 'top' })
    void router.push('/')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : 'Falha no login com Facebook.'
    $q.notify({ message: msg, color: 'negative', position: 'top' })
  } finally {
    loadingFacebook.value = false
  }
}
</script>

<style scoped lang="scss">
.container { max-width: 720px; margin: 0 auto; }
.registro-card {
  width: 100%;
  max-width: 720px;
  margin: 0 auto;
  border-radius: 24px;
}
.reg-logo {
  font-size: 1.8rem;
  font-weight: 900;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}
.reg-title { font-size: 1.4rem; font-weight: 700; margin: 8px 0 0; }
.reg-sub { font-size: 0.85rem; margin: 6px 0 0; }
.section-label {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--q-primary);
  margin: 0 0 8px;
}
</style>
