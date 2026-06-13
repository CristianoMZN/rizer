<template>
  <q-page padding>
    <h1 class="text-h5 q-mb-md">Configurações</h1>

    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant para editar suas configurações.
    </q-banner>

    <template v-else>
      <q-tabs
        v-model="tab"
        dense
        class="text-grey-7 q-mb-md"
        active-color="primary"
        indicator-color="primary"
      >
        <q-tab name="profile" label="Perfil" icon="store" />
        <q-tab name="domain" label="Domínio" icon="language" />
        <q-tab name="team" label="Membros" icon="group" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated keep-alive>
      <!-- ─── ABA PERFIL ─── -->
      <q-tab-panel name="profile">
        <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>
        <q-spinner v-if="loading && !settings" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />
        <q-form v-else-if="settings" @submit.prevent="saveProfile" class="q-gutter-sm">
          <div class="row q-col-gutter-md">
            <q-input v-model="form.tradeName" label="Nome fantasia" outlined dense class="col-12" lazy-rules :rules="[(v: string) => !!v || 'Obrigatório']" />
          </div>
          <q-input v-model="form.legalName" label="Razão social" outlined dense />
          <q-input v-model="form.description" label="Descrição (sobre a empresa)" outlined dense type="textarea" autogrow />
          <div class="row q-col-gutter-md">
            <q-input v-model="form.phone" label="Telefone" outlined dense class="col-6 col-sm-3" mask="(##) #####-####" unmasked-value />
            <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-6 col-sm-3" mask="(##) #####-####" unmasked-value />
            <q-input v-model="form.email" label="E-mail" type="email" outlined dense class="col-12 col-sm-3" />
            <q-input v-model="form.website" label="Site" outlined dense class="col-12 col-sm-3" />
          </div>
          <div class="row q-col-gutter-md">
            <q-input v-model="form.logoUrl" label="URL do logo" outlined dense class="col-12 col-sm-6" />
            <q-input v-model="form.bannerUrl" label="URL do banner" outlined dense class="col-12 col-sm-6" />
          </div>
          <div class="q-mt-md">
            <q-btn unelevated color="primary" label="Salvar perfil" :loading="savingProfile" type="submit" />
          </div>
        </q-form>
      </q-tab-panel>

      <!-- ─── ABA DOMÍNIO ─── -->
      <q-tab-panel name="domain">
        <q-banner v-if="domainError" class="bg-negative text-white q-mb-md">{{ domainError }}</q-banner>
        <q-spinner v-if="loadingDomain" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />
        <template v-else-if="domain">
          <q-card flat bordered>
            <q-card-section>
              <div class="text-overline">Domínio customizado</div>
              <div class="row items-center q-mt-sm">
                <div class="text-h6">{{ domain.domain || 'Nenhum configurado' }}</div>
                <q-chip
                  v-if="domain.domain"
                  :color="statusColor(domain.status)"
                  :label="domain.status"
                  class="q-ml-md"
                />
              </div>
              <div v-if="domain.lastError" class="text-caption text-negative q-mt-sm">
                {{ domain.lastError }}
              </div>
            </q-card-section>
            <q-card-section v-if="!domain.domain" class="q-pt-none">
              <q-banner class="bg-info text-white q-mb-md">
                O domínio customizado requer plano PRO ou Platinum. Após configurar,
                aponte o CNAME do seu domínio para
                <strong>{{ domain.expectedCname }}</strong>.
              </q-banner>
              <div class="row q-col-gutter-md items-end">
                <q-input
                  v-model="domainInput"
                  label="seudominio.com.br"
                  outlined
                  dense
                  class="col-12 col-sm-8"
                  lazy-rules
                  :rules="[(v: string) => !v || /^([a-z0-9-]+\\.)+[a-z]{2,}$/i.test(v) || 'Domínio inválido']"
                />
                <q-btn
                  unelevated
                  color="primary"
                  label="Configurar domínio"
                  :loading="settingDomain"
                  :disable="!domainInput"
                  @click="setDomain"
                />
              </div>
            </q-card-section>
            <q-card-section v-else class="q-pt-none">
              <q-banner class="bg-grey-2 q-mb-md">
                <div class="text-subtitle2">Configure o CNAME no seu provedor de DNS</div>
                <div class="text-caption q-mt-sm" style="word-break: break-all;">
                  <code>{{ domain.domain }}</code> → CNAME → <code>{{ domain.expectedCname }}</code>
                </div>
              </q-banner>
              <div class="row q-gutter-sm">
                <q-btn
                  unelevated
                  color="primary"
                  icon="refresh"
                  label="Verificar agora"
                  :loading="verifying"
                  @click="verifyDomain"
                />
                <q-btn
                  outline
                  color="negative"
                  icon="delete"
                  label="Remover"
                  @click="removeDomain"
                />
              </div>
              <q-banner class="bg-warning text-black q-mt-md">
                <q-icon name="lock" class="q-mr-sm" />
                <strong>SSL:</strong> a emissão automática do certificado está marcada como
                <code>// TODO(fase-7-prod): emitir/renovar SSL via Cloudflare Origin CA</code>.
                Em produção, basta o status estar <strong>VERIFIED</strong> que o cert será provisionado.
              </q-banner>

              <q-separator class="q-my-md" />
              <div class="text-subtitle2 q-mb-sm">Histórico de verificações</div>
              <q-list bordered>
                <q-item v-for="h in history" :key="h.id">
                  <q-item-section avatar>
                    <q-icon :name="h.status === 'VERIFIED' ? 'check_circle' : 'error'" :color="h.status === 'VERIFIED' ? 'positive' : 'negative'" />
                  </q-item-section>
                  <q-item-section>
                    <q-item-label>{{ formatDate(h.checkedAt) }}</q-item-label>
                    <q-item-label caption>
                      CNAME encontrado: <code>{{ h.cnameFound || '—' }}</code> · IP: <code>{{ h.resolvedIp || '—' }}</code>
                    </q-item-label>
                    <q-item-label v-if="h.errorMessage" caption class="text-negative">{{ h.errorMessage }}</q-item-label>
                  </q-item-section>
                </q-item>
                <q-item v-if="history.length === 0">
                  <q-item-section>
                    <q-item-label caption>Nenhuma verificação ainda. Clique em "Verificar agora".</q-item-label>
                  </q-item-section>
                </q-item>
              </q-list>
            </q-card-section>
          </q-card>
        </template>
      </q-tab-panel>

      <!-- ─── ABA MEMBROS ─── -->
      <q-tab-panel name="team">
        <q-banner class="bg-info text-white">
          <q-icon name="info" class="q-mr-sm" />
          Para gerenciar membros (vendedores, gerentes), acesse
          <router-link :to="{ name: 'app-members' }" class="text-white text-weight-bold q-ml-sm">/app/membros</router-link>.
        </q-banner>
      </q-tab-panel>
      </q-tab-panels>
    </template>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import {
  settingsApi,
  type TenantSettingsView, type CustomDomainView, type CustomDomainCheck,
  type UpdateProfileRequest,
} from 'src/services/api'

const $q = useQuasar()
const auth = useAuthStore()

const tab = ref<'profile' | 'domain' | 'team'>('profile')
const settings = ref<TenantSettingsView | null>(null)
const loading = ref(false)
const savingProfile = ref(false)
const error = ref<string | null>(null)
const form = reactive<UpdateProfileRequest>({
  tradeName: '',
  legalName: '',
  description: '',
  phone: '',
  whatsapp: '',
  email: '',
  website: '',
  logoUrl: '',
  bannerUrl: '',
})

const domain = ref<CustomDomainView | null>(null)
const history = ref<CustomDomainCheck[]>([])
const loadingDomain = ref(false)
const domainError = ref<string | null>(null)
const settingDomain = ref(false)
const verifying = ref(false)
const domainInput = ref('')

function statusColor(s: string): string {
  switch (s) {
    case 'VERIFIED': return 'positive'
    case 'PENDING': return 'warning'
    case 'FAILED': return 'negative'
    case 'NONE':
    default: return 'grey'
  }
}

function formatDate(d: string): string {
  if (!d) return '—'
  try { return new Date(d).toLocaleString('pt-BR') } catch { return d }
}

async function loadProfile() {
  loading.value = true
  error.value = null
  try {
    settings.value = await settingsApi.get()
    form.tradeName = settings.value.tradeName
    form.legalName = settings.value.legalName ?? ''
    form.description = settings.value.description ?? ''
    form.phone = settings.value.phone ?? ''
    form.whatsapp = settings.value.whatsapp ?? ''
    form.email = settings.value.email ?? ''
    form.website = settings.value.website ?? ''
    form.logoUrl = settings.value.logoUrl ?? ''
    form.bannerUrl = settings.value.bannerUrl ?? ''
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Não foi possível carregar o perfil.'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  savingProfile.value = true
  try {
    settings.value = await settingsApi.updateProfile({ ...form })
    $q.notify({ message: 'Perfil salvo.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  } finally {
    savingProfile.value = false
  }
}

async function loadDomain() {
  loadingDomain.value = true
  domainError.value = null
  try {
    domain.value = await settingsApi.getCustomDomain()
    if (domain.value.domain) {
      try { history.value = await settingsApi.customDomainHistory() } catch { history.value = [] }
    }
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    domainError.value = detail || 'Não foi possível carregar o domínio.'
  } finally {
    loadingDomain.value = false
  }
}

async function setDomain() {
  if (!domainInput.value) return
  settingDomain.value = true
  try {
    domain.value = await settingsApi.setCustomDomain(domainInput.value)
    $q.notify({ message: 'Domínio salvo. Verifique o CNAME e clique em "Verificar agora".', color: 'info' })
    domainInput.value = ''
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar domínio.', color: 'negative' })
  } finally {
    settingDomain.value = false
  }
}

async function verifyDomain() {
  verifying.value = true
  try {
    const result = await settingsApi.verifyCustomDomain()
    $q.notify({
      message: result.status === 'VERIFIED'
        ? 'CNAME verificado! O SSL será emitido automaticamente.'
        : `Falha: ${result.errorMessage ?? 'verifique o CNAME'}`,
      color: result.status === 'VERIFIED' ? 'positive' : 'negative',
    })
    await loadDomain()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao verificar.', color: 'negative' })
  } finally {
    verifying.value = false
  }
}

function removeDomain() {
  $q.dialog({
    title: 'Remover domínio customizado',
    message: 'Tem certeza? O site voltará a usar o subdomínio padrão.',
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await settingsApi.setCustomDomain('')
        $q.notify({ message: 'Domínio removido.', color: 'positive' })
        await loadDomain()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao remover.', color: 'negative' })
      }
    })()
  })
}

onMounted(() => {
  if (auth.currentTenantId.value) {
    void loadProfile()
    void loadDomain()
  }
})
</script>
