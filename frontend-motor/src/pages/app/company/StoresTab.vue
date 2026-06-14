<template>
  <div>
    <div class="row items-center q-mb-md">
      <div class="col">
        <h2 class="text-h6 q-my-none">Lojas físicas</h2>
        <p class="text-caption text-grey-7 q-mb-none">
          {{ stores.length }} ativa(s) de até {{ maxStores }} permitida(s) pelo plano.
        </p>
      </div>
      <div class="col-auto">
        <q-btn color="primary" icon="add" label="Nova loja" unelevated :disable="atLimit" @click="openCreate">
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
      <template #body-cell-isBranch="props">
        <q-td :props="props">
          <q-badge :color="props.row.isBranch ? 'secondary' : 'primary'" :label="props.row.isBranch ? 'Filial' : 'Matriz'" />
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
          <q-btn flat round dense icon="photo_library" @click="openGallery(props.row)">
            <q-tooltip>Galeria</q-tooltip>
          </q-btn>
          <q-btn flat round dense icon="delete" color="negative" @click="confirmDelete(props.row)" />
        </q-td>
      </template>
      <template #no-data>
        <div class="full-width text-center q-pa-md text-grey-6">
          Nenhuma loja cadastrada. Clique em <strong>Nova loja</strong>.
        </div>
      </template>
    </q-table>

    <StoreEditDialog
      v-model="formOpen"
      :editing-id="editingId"
      @saved="onSaved"
    />

    <StoreGalleryDialog
      v-model="galleryOpen"
      :store-id="galleryStoreId"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { tenantApi, type StoreView } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'
import { useTenantRole } from 'src/composables/useTenantRole'
import StoreEditDialog from './StoreEditDialog.vue'
import StoreGalleryDialog from './StoreGalleryDialog.vue'

const $q = useQuasar()
const auth = useAuthStore()
const role = useTenantRole()

const stores = ref<StoreView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const maxStores = ref(3)

const formOpen = ref(false)
const editingId = ref<string | null>(null)
const galleryOpen = ref(false)
const galleryStoreId = ref<string | null>(null)

const atLimit = computed(() => stores.value.filter((s) => s.isActive).length >= maxStores.value)

const columns: QTableColumn<StoreView>[] = [
  { name: 'isMain', label: '', field: 'isMain', align: 'center' },
  { name: 'name', label: 'Nome', field: 'name', align: 'left', sortable: true },
  { name: 'isBranch', label: 'Tipo', field: 'isBranch', align: 'left' },
  { name: 'city', label: 'Cidade', field: (r) => r.addressCity, align: 'left' },
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
    if (status === 403) error.value = 'Selecione um tenant.'
    else if (status === 402) error.value = 'Limite de lojas do plano atingido.'
    else error.value = 'Não foi possível carregar as lojas.'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  formOpen.value = true
}

function openEdit(s: StoreView) {
  editingId.value = s.id
  formOpen.value = true
}

function openGallery(s: StoreView) {
  galleryStoreId.value = s.id
  galleryOpen.value = true
}

function onSaved() {
  void load()
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
void role // suppress unused warning in templates
</script>
