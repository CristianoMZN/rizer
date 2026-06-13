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
            :to="{ name: 'app-product-new' }"
          />
        </div>
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

      <q-table
        :rows="rows"
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
        <template #body-cell-status="props">
          <q-td :props="props">
            <q-badge :color="statusColor(props.row.status)" :label="props.row.status" />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn
              flat
              round
              dense
              icon="edit"
              :to="{ name: 'app-product-edit', params: { id: props.row.id } }"
            />
            <q-btn flat round dense icon="delete" color="negative" @click="confirmDelete(props.row)" />
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-md text-grey-6">
            Nenhum anúncio ainda. Clique em <strong>Novo anúncio</strong> para começar.
          </div>
        </template>
      </q-table>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { tenantProductApi, type ProductView, type ProductImageView } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const auth = useAuthStore()

const rows = ref<ProductView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const columns: QTableColumn<ProductView>[] = [
  { name: 'cover', label: '', field: 'images', align: 'center' },
  { name: 'title', label: 'Anúncio', field: 'title', align: 'left', sortable: true },
  { name: 'categoryName', label: 'Categoria', field: 'categoryName', align: 'left' },
  { name: 'physicalStoreName', label: 'Loja', field: 'physicalStoreName', align: 'left' },
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

async function load() {
  loading.value = true
  error.value = null
  try {
    rows.value = await tenantProductApi.list()
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 403
      ? 'Selecione um tenant para continuar.'
      : 'Não foi possível carregar os anúncios.'
  } finally {
    loading.value = false
  }
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
