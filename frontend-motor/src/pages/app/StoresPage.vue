<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant primeiro (faça login novamente ou peça ao admin para incluí-lo).
    </q-banner>

    <div v-else>
      <div class="row items-center q-mb-md">
        <div class="col">
          <h1 class="text-h5 q-my-none">Lojas físicas</h1>
          <p class="text-caption text-grey-7 q-mb-none">
            {{ stores.length }} ativa(s) de até {{ maxStores }} permitida(s) pelo plano.
          </p>
        </div>
        <div class="col-auto">
          <q-btn
            color="primary"
            icon="add"
            label="Nova loja"
            unelevated
            :disable="atLimit"
            @click="openCreate"
          >
            <q-tooltip v-if="atLimit">Limite do plano atingido. Faça upgrade.</q-tooltip>
          </q-btn>
        </div>
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

      <q-table
        :rows="stores"
        :columns="columns"
        row-key="id"
        :loading="loading"
        flat
        bordered
        :rows-per-page-options="[10, 25]"
      >
        <template #body-cell-isMain="props">
          <q-td :props="props">
            <q-icon v-if="props.row.isMain" name="star" color="primary" size="20px">
              <q-tooltip>Loja principal</q-tooltip>
            </q-icon>
          </q-td>
        </template>
        <template #body-cell-isActive="props">
          <q-td :props="props">
            <q-badge :color="props.row.isActive ? 'positive' : 'grey-6'" :label="props.row.isActive ? 'Ativa' : 'Inativa'" />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn flat round dense icon="edit" @click="openEdit(props.row)" />
            <q-btn flat round dense icon="delete" color="negative" @click="confirmDelete(props.row)" />
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-md text-grey-6">
            Nenhuma loja cadastrada ainda. Clique em <strong>Nova loja</strong> para começar.
          </div>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="formOpen">
      <q-card style="min-width: 480px; max-width: 640px">
        <q-card-section>
          <div class="text-h6">{{ editing ? 'Editar loja' : 'Nova loja' }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-form @submit.prevent="onSubmit" class="q-gutter-sm">
            <q-input
              v-model="form.name"
              label="Nome"
              outlined
              dense
              lazy-rules
              :rules="[(v: string) => !!v || 'Obrigatório']"
            />
            <q-input
              v-model="form.slug"
              label="Slug (opcional — gerado a partir do nome)"
              outlined
              dense
              hint="Ex.: matriz-centro, filial-zona-sul"
            />
            <div class="row q-col-gutter-md">
              <q-input v-model="form.phone" label="Telefone" outlined dense class="col-6" mask="(##) #####-####" unmasked-value />
              <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-6" mask="(##) #####-####" unmasked-value />
            </div>
            <q-input v-model="form.email" label="E-mail" type="email" outlined dense />
            <q-toggle v-model="form.isMain" label="Marcar como loja principal" />
            <q-toggle v-if="editing" v-model="form.isActive" label="Ativa" />
            <div class="row q-col-gutter-md">
              <q-input
                v-model="form.latitude"
                label="Latitude"
                type="number"
                outlined
                dense
                class="col-6"
                step="0.000001"
                hint="-23.5505 (São Paulo)"
              />
              <q-input
                v-model="form.longitude"
                label="Longitude"
                type="number"
                outlined
                dense
                class="col-6"
                step="0.000001"
                hint="-46.6333 (São Paulo)"
              />
            </div>
          </q-form>
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn unelevated color="primary" :label="editing ? 'Salvar' : 'Criar'" :loading="saving" @click="onSubmit" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { tenantApi, type StoreView, type CreateStoreRequest, type UpdateStoreRequest } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const auth = useAuthStore()

const stores = ref<StoreView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const maxStores = ref(3)

const formOpen = ref(false)
const editing = ref(false)
const saving = ref(false)
const editingId = ref<string | null>(null)
const form = reactive({
  name: '',
  slug: '',
  phone: '',
  whatsapp: '',
  email: '',
  isMain: false,
  isActive: true,
  latitude: null as number | null,
  longitude: null as number | null,
})

const atLimit = computed(() => stores.value.filter((s) => s.isActive).length >= maxStores.value)

const columns: QTableColumn<StoreView>[] = [
  { name: 'isMain', label: '', field: 'isMain', align: 'center' },
  { name: 'name', label: 'Nome', field: 'name', align: 'left', sortable: true },
  { name: 'slug', label: 'Slug', field: 'slug', align: 'left' },
  { name: 'phone', label: 'Telefone', field: 'phone', align: 'left' },
  { name: 'isActive', label: 'Status', field: 'isActive', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]

async function load() {
  loading.value = true
  error.value = null
  try {
    stores.value = await tenantApi.listStores()
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status === 403) error.value = 'Selecione um tenant para continuar.'
    else if (status === 402) error.value = 'Limite de lojas do plano atingido. Faça upgrade.'
    else error.value = 'Não foi possível carregar as lojas.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    name: '', slug: '', phone: '', whatsapp: '', email: '',
    isMain: stores.value.length === 0,
    isActive: true,
    latitude: null, longitude: null,
  })
  formOpen.value = true
}

function openEdit(s: StoreView) {
  editing.value = true
  editingId.value = s.id
  Object.assign(form, {
    name: s.name,
    slug: s.slug,
    phone: s.phone ?? '',
    whatsapp: s.whatsapp ?? '',
    email: s.email ?? '',
    isMain: s.isMain,
    isActive: s.isActive,
    latitude: s.latitude ?? null,
    longitude: s.longitude ?? null,
  })
  formOpen.value = true
}

async function onSubmit() {
  saving.value = true
  try {
    if (editing.value && editingId.value) {
      const patch: UpdateStoreRequest = {
        name: form.name,
        isMain: form.isMain,
        isActive: form.isActive,
      }
      if (form.phone) patch.phone = form.phone
      if (form.whatsapp) patch.whatsapp = form.whatsapp
      if (form.email) patch.email = form.email
      if (form.latitude != null) patch.latitude = form.latitude
      if (form.longitude != null) patch.longitude = form.longitude
      await tenantApi.updateStore(editingId.value, patch)
      $q.notify({ message: 'Loja atualizada.', color: 'positive' })
    } else {
      const create: CreateStoreRequest = {
        name: form.name,
        isMain: form.isMain,
      }
      if (form.slug) create.slug = form.slug
      if (form.phone) create.phone = form.phone
      if (form.whatsapp) create.whatsapp = form.whatsapp
      if (form.email) create.email = form.email
      if (form.latitude != null) create.latitude = form.latitude
      if (form.longitude != null) create.longitude = form.longitude
      await tenantApi.createStore(create)
      $q.notify({ message: 'Loja criada.', color: 'positive' })
    }
    formOpen.value = false
    void load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  } finally {
    saving.value = false
  }
}

function confirmDelete(row: StoreView) {
  $q.dialog({
    title: 'Excluir loja',
    message: `Excluir "${row.name}"?`,
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await tenantApi.deleteStore(row.id)
        $q.notify({ message: 'Loja excluída.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao excluir.', color: 'negative' })
      }
    })()
  })
}

onMounted(() => {
  if (auth.currentTenantId.value) void load()
})
</script>
