<template>
  <q-page padding>
    <h1 class="text-h5 q-mb-md">Integrações de marketing</h1>
    <p class="text-caption text-grey-7 q-mb-lg">
      Conecte sua conta Meta/Google para anunciar seus veículos no Instagram, META Dynamic Ads e Google Shopping.
    </p>

    <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>
    <q-banner v-if="info" class="bg-info text-white q-mb-md">{{ info }}</q-banner>

    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <div v-else class="row q-col-gutter-md">
      <div
        v-for="card in cards"
        :key="card.provider"
        class="col-12 col-md-4"
      >
        <q-card flat bordered class="full-height">
          <q-card-section>
            <div class="row items-center">
              <q-icon :name="card.icon" :color="card.color" size="32px" class="q-mr-sm" />
              <div class="text-h6">{{ card.name }}</div>
            </div>
            <p class="text-body2 text-grey-7 q-mt-sm">{{ card.description }}</p>
            <q-chip
              v-if="integrationFor(card.provider)"
              :color="integrationFor(card.provider)!.status === 'CONNECTED' ? 'positive' : 'orange'"
              :label="integrationFor(card.provider)!.status"
              class="q-mt-sm"
            />
            <q-chip
              v-else
              color="grey-5"
              label="Desconectado"
              class="q-mt-sm"
            />
            <div v-if="integrationFor(card.provider)" class="q-mt-md">
              <div v-if="integrationFor(card.provider)!.externalAccountName" class="text-caption text-grey-7">
                <q-icon name="account_circle" size="14px" />
                {{ integrationFor(card.provider)!.externalAccountName }}
              </div>
              <div v-if="integrationFor(card.provider)!.tokenExpiresAt" class="text-caption text-grey-7">
                <q-icon name="schedule" size="14px" />
                Token expira {{ formatDate(integrationFor(card.provider)!.tokenExpiresAt!) }}
              </div>
              <div v-if="integrationFor(card.provider)!.lastSyncAt" class="text-caption text-grey-7">
                <q-icon name="sync" size="14px" />
                Última sync {{ formatDate(integrationFor(card.provider)!.lastSyncAt!) }}
              </div>
              <div v-if="integrationFor(card.provider)!.lastError" class="text-caption text-negative q-mt-xs">
                {{ integrationFor(card.provider)!.lastError }}
              </div>
            </div>
          </q-card-section>
          <q-card-actions class="q-pa-md">
            <q-btn
              v-if="!integrationFor(card.provider)"
              unelevated
              color="primary"
              :label="`Conectar ${card.name}`"
              full-width
              :disable="!card.available"
              @click="onConnect(card.provider)"
            >
              <q-tooltip v-if="!card.available">
                Disponível no plano Platinum. Faça upgrade em /app/assinatura.
              </q-tooltip>
            </q-btn>
            <template v-else>
              <q-btn
                outline
                color="negative"
                label="Desconectar"
                @click="onDisconnect(card.provider)"
              />
              <q-space />
              <q-btn
                flat
                color="primary"
                :label="card.provider === 'GOOGLE_MERCHANT' ? 'Ver feed' : 'Sincronizar agora'"
                @click="card.provider === 'GOOGLE_MERCHANT' ? onCopyFeedUrl() : onSync()"
              />
            </template>
          </q-card-actions>
        </q-card>
      </div>
    </div>

    <q-banner
      v-if="hasGoogleFeed && googleFeedUrl"
      class="bg-grey-2 q-mt-md"
    >
      <div class="row items-center">
        <div class="col">
          <div class="text-subtitle2">URL do feed do Google Merchant Center</div>
          <div class="text-caption text-grey-7 q-mt-xs" style="word-break: break-all;">
            {{ googleFeedUrl }}
          </div>
        </div>
        <q-btn flat color="primary" icon="content_copy" label="Copiar" @click="copyFeed" />
      </div>
    </q-banner>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute, useRouter } from 'vue-router'
import {
  integrationApi, tenantApi,
  type IntegrationProvider, type IntegrationView,
} from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const integrations = ref<IntegrationView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const info = ref<string | null>(null)
const tenantSlug = ref<string | null>(null)

const cards = [
  {
    provider: 'INSTAGRAM' as IntegrationProvider,
    name: 'Instagram',
    description: 'Cada novo anúncio vira automaticamente um post na sua conta business do Instagram.',
    icon: 'photo_camera',
    color: 'pink-6',
    available: true,
  },
  {
    provider: 'META_BUSINESS' as IntegrationProvider,
    name: 'META Dynamic Ads',
    description: 'Sincronize seu catálogo com o Meta Commerce Manager para veicular Dynamic Product Ads.',
    icon: 'campaign',
    color: 'blue-7',
    available: true,
  },
  {
    provider: 'GOOGLE_MERCHANT' as IntegrationProvider,
    name: 'Google Shopping',
    description: 'Exponha seus veículos no Google Shopping. Use o feed XML público abaixo.',
    icon: 'shopping_cart',
    color: 'red-6',
    available: true,
  },
]

const hasGoogleFeed = computed(() =>
  integrations.value.some((i) => i.provider === 'GOOGLE_MERCHANT' && i.status === 'CONNECTED')
)

const googleFeedUrl = computed(() => {
  if (!tenantSlug.value) return null
  return `${window.location.origin}/BR/public/tenants/${tenantSlug.value}/feed.xml`
})

function integrationFor(provider: IntegrationProvider): IntegrationView | undefined {
  return integrations.value.find((i) => i.provider === provider)
}

function formatDate(d: string): string {
  if (!d) return '—'
  try { return new Date(d).toLocaleString('pt-BR') } catch { return d }
}

async function load() {
  loading.value = true
  error.value = null
  info.value = null
  try {
    integrations.value = await integrationApi.list()
    // Pega o slug do tenant atual via membership
    const mem = auth.user.value?.memberships.find((m) => m.tenantId === auth.currentTenantId.value)
    if (mem?.tenantSlug) tenantSlug.value = mem.tenantSlug
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status === 402) error.value = 'Seu plano atual não cobre integrações. Faça upgrade para Platinum.'
    else if (status === 403) error.value = 'Selecione um tenant para continuar.'
    else error.value = 'Não foi possível carregar as integrações.'
  } finally {
    loading.value = false
  }
  // Trata callback do OAuth
  if (route.query.code && route.query.state) {
    const code = route.query.code as string
    const state = route.query.state as string
    const provider = (route.query.provider as IntegrationProvider) ?? 'INSTAGRAM'
    try {
      await integrationApi.callback(provider, code, state)
      info.value = `${provider} conectado com sucesso!`
      await load()
    } catch (e: unknown) {
      const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      error.value = detail || 'Falha no callback OAuth.'
    }
    // Limpa a URL
    void router.replace({ query: {} })
  }
}

async function onConnect(provider: IntegrationProvider) {
  try {
    const res = await integrationApi.authorize(provider)
    window.location.href = res.authorizeUrl
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao iniciar OAuth.', color: 'negative' })
  }
}

function onDisconnect(provider: IntegrationProvider) {
  $q.dialog({
    title: 'Desconectar',
    message: `Desconectar ${provider}?`,
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await integrationApi.disconnect(provider)
        $q.notify({ message: 'Desconectado.', color: 'positive' })
        await load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao desconectar.', color: 'negative' })
      }
    })()
  })
}

function onSync() {
  $q.notify({ message: 'Sincronização agendada. O job roda a cada 5 minutos.', color: 'info' })
}

function onCopyFeedUrl() {
  if (googleFeedUrl.value) {
    void navigator.clipboard.writeText(googleFeedUrl.value)
    $q.notify({ message: 'URL copiada.', color: 'positive' })
  }
}

function copyFeed() { onCopyFeedUrl() }

// Tenta buscar o slug real via tenantApi (admin membership fallback)
onMounted(async () => {
  if (auth.currentTenantId.value) {
    try { void tenantApi.listStores() } catch { /* ignore */ }
  }
  await load()
})
</script>
