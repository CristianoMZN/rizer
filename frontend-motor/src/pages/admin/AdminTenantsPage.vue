<template>
  <q-page padding>
    <div class="row items-center q-mb-md">
      <div class="col">
        <h1 class="text-h5 q-my-none">Tenants</h1>
        <p class="text-caption text-grey-7 q-mb-none">
          Empresas parceiras cadastradas na plataforma.
        </p>
      </div>
      <div class="col-auto">
        <q-btn
          color="primary"
          icon="add"
          label="Novo tenant"
          unelevated
          @click="openCreate"
        />
      </div>
    </div>

    <q-banner v-if="error" class="bg-negative text-white q-mb-md">
      {{ error }}
    </q-banner>

    <q-table
      :rows="rows"
      :columns="columns"
      row-key="id"
      :loading="loading"
      flat
      bordered
      :rows-per-page-options="[10, 25, 50]"
    >
      <template #body-cell-status="props">
        <q-td :props="props">
          <q-badge :color="statusColor(props.row.status)" :label="props.row.status" />
        </q-td>
      </template>
      <template #body-cell-tradeName="props">
        <q-td :props="props">
          <div class="text-weight-medium">{{ props.row.tradeName }}</div>
          <div class="text-caption text-grey-6">{{ props.row.slug }}.{{ baseDomain }}</div>
        </q-td>
      </template>
      <template #body-cell-owner="props">
        <q-td :props="props">
          <div>{{ props.row.ownerName || '—' }}</div>
          <div class="text-caption text-grey-6">{{ props.row.ownerEmail || '' }}</div>
        </q-td>
      </template>
      <template #body-cell-stats="props">
        <q-td :props="props">
          {{ props.row.activeStoresCount }} loja(s) · {{ props.row.membersCount }} membro(s)
        </q-td>
      </template>
      <template #body-cell-actions="props">
        <q-td :props="props" class="q-gutter-xs">
          <q-btn flat round dense icon="visibility" @click="viewTenant(props.row.id)">
            <q-tooltip>Ver detalhes</q-tooltip>
          </q-btn>
          <q-btn flat round dense icon="delete" color="negative" @click="confirmDelete(props.row)">
            <q-tooltip>Excluir</q-tooltip>
          </q-btn>
        </q-td>
      </template>
    </q-table>

    <!-- Dialog: criar tenant -->
    <q-dialog v-model="createOpen" persistent>
      <q-card style="min-width: 540px; max-width: 720px">
        <q-card-section>
          <div class="text-h6">Novo tenant + 1º owner</div>
          <div class="text-caption text-grey-7">
            O owner recebe o papel <code>agency_owner</code> e gerencia o tenant.
          </div>
        </q-card-section>

        <q-card-section class="q-pt-none">
          <q-form @submit.prevent="onSubmit" class="q-gutter-sm">
            <div class="text-subtitle2 q-mt-sm">Empresa</div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="form.countryCode"
                label="País (código)"
                outlined
                dense
                class="col-3"
                maxlength="2"
                hint="BR"
              />
              <q-input
                v-model="form.slug"
                label="Slug (subdomínio)"
                outlined
                dense
                class="col-9"
                :hint="`Será acessível em ${form.slug || 'slug'}.${baseDomain}`"
                lazy-rules
                :rules="[(v: string) => !!v && /^[a-z0-9-]+$/.test(v) || 'Use letras minúsculas, números e hífen']"
              />
            </div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="form.tradeName"
                label="Nome fantasia"
                outlined
                dense
                class="col-8"
                lazy-rules
                :rules="[(v: string) => !!v || 'Obrigatório']"
              />
              <q-input
                v-model="form.cnpj"
                label="CNPJ"
                outlined
                dense
                class="col-4"
                mask="##.###.###/####-##"
                unmasked-value
              />
            </div>
            <q-input
              v-model="form.legalName"
              label="Razão social"
              outlined
              dense
            />
            <div class="row q-col-gutter-md">
              <q-input v-model="form.phone" label="Telefone" outlined dense class="col-4" mask="(##) #####-####" unmasked-value />
              <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-4" mask="(##) #####-####" unmasked-value />
              <q-input v-model="form.email" label="E-mail" type="email" outlined dense class="col-4" />
            </div>
            <q-input v-model="form.website" label="Site (opcional)" outlined dense />
            <q-input v-model="form.description" label="Descrição" outlined dense type="textarea" autogrow />

            <q-separator class="q-my-md" />
            <div class="text-subtitle2">1º owner (acesso ao painel)</div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="form.ownerName"
                label="Nome"
                outlined
                dense
                class="col-6"
                lazy-rules
                :rules="[(v: string) => !!v || 'Obrigatório']"
              />
              <q-input
                v-model="form.ownerEmail"
                label="E-mail"
                type="email"
                outlined
                dense
                class="col-6"
                lazy-rules
                :rules="[(v: string) => !!v || 'Obrigatório']"
              />
            </div>
            <div class="row q-col-gutter-md">
              <q-input
                v-model="form.ownerPhone"
                label="Telefone"
                outlined
                dense
                class="col-4"
                mask="(##) #####-####"
                unmasked-value
              />
              <q-input
                v-model="form.ownerPassword"
                label="Senha temporária"
                type="password"
                outlined
                dense
                class="col-8"
                lazy-rules
                :rules="[(v: string) => !!v && v.length >= 8 || 'Mínimo 8 caracteres']"
              />
            </div>
            <q-toggle v-model="form.startWithTrial" label="Iniciar com trial (Fase 5 vai materializar isso)" />

          </q-form>
        </q-card-section>

        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn
            unelevated
            color="primary"
            label="Criar tenant"
            :loading="creating"
            @click="onSubmit"
          />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { useRouter } from 'vue-router'
import { adminApi, type TenantView } from 'src/services/api'

const $q = useQuasar()
const router = useRouter()

const rows = ref<TenantView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const baseDomain = 'motorise.com.br'

const createOpen = ref(false)
const creating = ref(false)
const form = reactive({
  countryCode: 'BR',
  slug: '',
  tradeName: '',
  legalName: '',
  cnpj: '',
  phone: '',
  whatsapp: '',
  email: '',
  website: '',
  description: '',
  ownerName: '',
  ownerEmail: '',
  ownerPhone: '',
  ownerPassword: '',
  startWithTrial: true,
})

const columns: QTableColumn<TenantView>[] = [
  { name: 'tradeName', label: 'Empresa', field: 'tradeName', align: 'left', sortable: true },
  { name: 'owner', label: 'Owner', field: 'ownerName', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'stats', label: 'Lojas · Membros', field: 'activeStoresCount', align: 'left' },
  { name: 'createdAt', label: 'Criado em', field: 'createdAt', align: 'left', format: (v) => v ? new Date(v as string).toLocaleDateString('pt-BR') : '' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]

function statusColor(s: string): string {
  switch (s) {
    case 'active': return 'positive'
    case 'pending': return 'warning'
    case 'paused': return 'info'
    case 'suspended': return 'orange'
    case 'canceled': return 'negative'
    default: return 'grey'
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    rows.value = await adminApi.listTenants()
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 401
      ? 'Sessão expirada. Faça login novamente.'
      : 'Não foi possível carregar a lista de tenants.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(form, {
    countryCode: 'BR',
    slug: '',
    tradeName: '',
    legalName: '',
    cnpj: '',
    phone: '',
    whatsapp: '',
    email: '',
    website: '',
    description: '',
    ownerName: '',
    ownerEmail: '',
    ownerPhone: '',
    ownerPassword: '',
    startWithTrial: true,
  })
  createOpen.value = true
}

async function onSubmit() {
  creating.value = true
  try {
    const created = await adminApi.createTenant({
      countryCode: form.countryCode,
      slug: form.slug,
      tradeName: form.tradeName,
      ...pickDefined({
        legalName: form.legalName,
        cnpj: form.cnpj,
        phone: form.phone,
        whatsapp: form.whatsapp,
        email: form.email,
        website: form.website,
        description: form.description,
        ownerPhone: form.ownerPhone,
      }),
      ownerName: form.ownerName,
      ownerEmail: form.ownerEmail,
      ownerPassword: form.ownerPassword,
      startWithTrial: form.startWithTrial,
    })
    createOpen.value = false
    $q.notify({ message: `Tenant "${created.tradeName}" criado.`, color: 'positive' })
    void load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao criar tenant.', color: 'negative' })
  } finally {
    creating.value = false
  }
}

function viewTenant(id: string) {
  void router.push({ name: 'admin-tenants' }).then(() => {
    $q.notify({ message: `Detalhes do tenant ${id} virão na próxima iteração.`, color: 'info' })
  })
}

function pickDefined<T extends Record<string, unknown>>(obj: T): Partial<T> {
  const out: Partial<T> = {}
  for (const k in obj) {
    if (obj[k] !== '' && obj[k] !== null && obj[k] !== undefined) {
      out[k] = obj[k]
    }
  }
  return out
}

function confirmDelete(row: TenantView) {
  $q.dialog({
    title: 'Excluir tenant',
    message: `Tem certeza que deseja excluir "${row.tradeName}"? Esta ação é reversível (soft delete).`,
    cancel: true,
    persistent: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await adminApi.deleteTenant(row.id)
        $q.notify({ message: 'Tenant excluído.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao excluir.', color: 'negative' })
      }
    })()
  })
}

onMounted(load)
</script>
