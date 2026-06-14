<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant para gerenciar anúncios.
    </q-banner>

    <div v-else>
      <div class="row items-center q-mb-md">
        <div class="col">
          <h1 class="text-h5 q-my-none">Anúncios</h1>
          <p class="text-caption text-grey-7 q-mb-none">
            {{ rows.length }} anúncio(s) no total.
          </p>
        </div>
        <div class="col-auto">
          <q-btn
            color="primary"
            icon="add"
            label="Novo anúncio"
            unelevated
            @click="onNew"
          />
        </div>
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

      <q-card flat bordered class="q-mb-md">
        <q-card-section class="row q-gutter-md items-end">
          <q-input
            v-model="searchTerm"
            label="Buscar por título"
            outlined dense clearable
            class="col-12 col-sm-4"
            @update:model-value="applyFilters"
          />
          <q-select
            v-model="filterStatus"
            :options="statusOptions"
            label="Status"
            outlined dense emit-value map-options clearable
            class="col-6 col-sm-2"
            @update:model-value="applyFilters"
          />
          <q-select
            v-model="filterStore"
            :options="storeOptions"
            label="Loja"
            outlined dense emit-value map-options clearable
            class="col-6 col-sm-3"
            @update:model-value="applyFilters"
          />
          <q-select
            v-model="filterSeller"
            :options="sellerOptions"
            label="Vendedor"
            outlined dense emit-value map-options clearable
            class="col-6 col-sm-3"
            @update:model-value="applyFilters"
          />
        </q-card-section>
      </q-card>

      <q-table
        :rows="filtered"
        :columns="columns"
        row-key="id"
        :loading="loading"
        flat
        bordered
        :rows-per-page-options="[10, 25, 50]"
      >
        <template #body-cell-cover="props">
          <q-td :props="props">
            <q-avatar v-if="coverOf(props.row)" square size="48px">
              <img :src="coverOf(props.row)" :alt="props.row.title">
            </q-avatar>
            <q-icon v-else name="image" size="32px" color="grey-5" />
          </q-td>
        </template>
        <template #body-cell-title="props">
          <q-td :props="props">
            <div class="text-weight-medium">{{ props.row.title || '(sem título)' }}</div>
            <div class="text-caption text-grey-6">
              {{ props.row.brandName || '—' }} {{ props.row.modelName || '' }}
              · {{ props.row.yearModel || '—' }}
            </div>
          </q-td>
        </template>
        <template #body-cell-price="props">
          <q-td :props="props">
            {{ formatPrice(props.row.price, props.row.currency) }}
          </q-td>
        </template>
        <template #body-cell-seller="props">
          <q-td :props="props">
            <span v-if="props.row.sellerName">{{ props.row.sellerName }}</span>
            <span v-else class="text-grey-6 text-caption">—</span>
          </q-td>
        </template>
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge :color="statusColor(props.row.status)" :label="props.row.status" />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props">
            <q-btn flat round dense icon="more_vert">
              <q-menu>
                <q-list dense style="min-width: 180px">
                  <q-item clickable v-close-popup :to="{ name: 'app-product-edit', params: { id: props.row.id } }">
                    <q-item-section avatar><q-icon name="edit" /></q-item-section>
                    <q-item-section>Editar</q-item-section>
                  </q-item>
                  <q-item
                    v-if="props.row.status !== 'SOLD' && canMarkSold"
                    clickable v-close-popup
                    @click="confirmChangeStatus(props.row, 'SOLD')"
                  >
                    <q-item-section avatar><q-icon name="check_circle" color="positive" /></q-item-section>
                    <q-item-section>Marcar como vendido</q-item-section>
                  </q-item>
                  <q-item
                    v-if="props.row.status !== 'ARCHIVED' && canMarkSold"
                    clickable v-close-popup
                    @click="confirmChangeStatus(props.row, 'ARCHIVED')"
                  >
                    <q-item-section avatar><q-icon name="inventory_2" color="warning" /></q-item-section>
                    <q-item-section>Arquivar</q-item-section>
                  </q-item>
                  <q-item clickable v-close-popup @click="confirmDelete(props.row)">
                    <q-item-section avatar><q-icon name="delete" color="negative" /></q-item-section>
                    <q-item-section>Excluir</q-item-section>
                  </q-item>
                </q-list>
              </q-menu>
            </q-btn>
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-md text-grey-6">
            Nenhum anúncio. Clique em <strong>Novo anúncio</strong>.
          </div>
        </template>
      </q-table>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { useRouter } from 'vue-router'
import { tenantProductApi, tenantApi, type ProductView, type ProductImageView, type ProductStatus, type StoreView, type MemberView } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'
import { useTenantRole } from 'src/composables/useTenantRole'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()
const role = useTenantRole()

const rows = ref<ProductView[]>([])
const stores = ref<StoreView[]>([])
const members = ref<MemberView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const searchTerm = ref('')
const filterStatus = ref<ProductStatus | null>(null)
const filterStore = ref<string | null>(null)
const filterSeller = ref<string | null>(null)

const canMarkSold = computed(() => role.canMarkSold.value)

const statusOptions = [
  { label: 'Rascunho', value: 'DRAFT' },
  { label: 'Ativo', value: 'ACTIVE' },
  { label: 'Inativo', value: 'INACTIVE' },
  { label: 'Vendido', value: 'SOLD' },
  { label: 'Arquivado', value: 'ARCHIVED' },
]

const storeOptions = computed(() =>
  stores.value.map((s) => ({ label: s.name, value: s.id }))
)

const sellerOptions = computed(() =>
  members.value
    .filter((m) => m.role === 'SELLER')
    .map((m) => ({ label: m.name || m.email || '—', value: m.userId }))
)

const filtered = computed(() => {
  let list = rows.value
  if (filterStatus.value) list = list.filter((r) => r.status === filterStatus.value)
  if (filterStore.value) list = list.filter((r) => r.physicalStoreId === filterStore.value)
  if (filterSeller.value) list = list.filter((r) => r.sellerUserId === filterSeller.value)
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase()
    list = list.filter((r) => (r.title || '').toLowerCase().includes(q))
  }
  return list
})

const columns: QTableColumn<ProductView>[] = [
  { name: 'cover', label: '', field: 'images', align: 'center' },
  { name: 'title', label: 'Anúncio', field: 'title', align: 'left', sortable: true },
  { name: 'categoryName', label: 'Categoria', field: 'categoryName', align: 'left' },
  { name: 'physicalStoreName', label: 'Loja', field: 'physicalStoreName', align: 'left' },
  { name: 'seller', label: 'Vendedor', field: 'sellerName', align: 'left' },
  { name: 'price', label: 'Preço', field: 'price', align: 'right' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]

function statusColor(s: string): string {
  switch (s) {
    case 'ACTIVE': return 'positive'
    case 'DRAFT': return 'warning'
    case 'INACTIVE': return 'grey'
    case 'SOLD': return 'info'
    case 'ARCHIVED': return 'negative'
    default: return 'grey'
  }
}

function formatPrice(value: number, currency: string): string {
  try {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(value)
  } catch {
    return value.toFixed(2)
  }
}

function coverOf(row: ProductView): string | undefined {
  const cover: ProductImageView | undefined = row.images.find((i) => i.isCover) || row.images[0]
  return cover?.url
}

function applyFilters() { /* no-op, computed re-evaluates */ }

async function load() {
  loading.value = true
  error.value = null
  try {
    const [prods, ss, mm] = await Promise.all([
      tenantProductApi.list(),
      tenantApi.listStores(),
      tenantApi.listMembers(),
    ])
    rows.value = prods
    stores.value = ss
    members.value = mm
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 403
      ? 'Selecione um tenant para continuar.'
      : 'Não foi possível carregar os anúncios.'
  } finally {
    loading.value = false
  }
}

async function onNew() {
  // Cria rascunho imediatamente para preservar dados em caso de problemas
  const firstStore = stores.value[0]
  if (!firstStore) {
    error.value = 'Cadastre uma loja antes de criar anúncios.'
    return
  }
  try {
    const draft = await tenantProductApi.createDraft(firstStore.id)
    void router.push({ name: 'app-product-edit', params: { id: draft.id } })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao criar rascunho.', color: 'negative' })
  }
}

function confirmChangeStatus(row: ProductView, status: ProductStatus) {
  const label = status === 'SOLD' ? 'Marcar como vendido' : 'Arquivar'
  $q.dialog({
    title: label,
    message: `Confirmar "${label.toLowerCase()}" o anúncio "${row.title || 'sem título'}"?`,
    cancel: true,
    color: status === 'SOLD' ? 'positive' : 'warning',
  }).onOk(() => {
    void (async () => {
      try {
        await tenantProductApi.changeStatus(row.id, status)
        $q.notify({ message: 'Status atualizado.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao atualizar status.', color: 'negative' })
      }
    })()
  })
}

function confirmDelete(row: ProductView) {
  $q.dialog({
    title: 'Excluir anúncio',
    message: `Excluir "${row.title}"? (soft delete)`,
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await tenantProductApi.delete(row.id)
        $q.notify({ message: 'Anúncio excluído.', color: 'positive' })
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
