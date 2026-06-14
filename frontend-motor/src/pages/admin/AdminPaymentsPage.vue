<template>
  <q-page padding>
    <h1 class="text-h5 q-mb-md">Pagamentos</h1>
    <p class="text-caption text-grey-7">Livro-caixa global da plataforma.</p>

    <div class="row q-col-gutter-md q-mb-md">
      <div class="col-6 col-sm-3">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-caption text-grey-6">Tenants ativos</div>
            <div class="text-h4">{{ stats?.activeTenants ?? '—' }}</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-caption text-grey-6">Em trial</div>
            <div class="text-h4 text-info">{{ stats?.trialingTenants ?? '—' }}</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-caption text-grey-6">Inadimplentes</div>
            <div class="text-h4 text-warning">{{ stats?.pastDueTenants ?? '—' }}</div>
          </q-card-section>
        </q-card>
      </div>
      <div class="col-6 col-sm-3">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-caption text-grey-6">Receita 30d</div>
            <div class="text-h4 text-primary">{{ formatPrice(stats?.revenueLast30d ?? 0, 'BRL') }}</div>
          </q-card-section>
        </q-card>
      </div>
    </div>

    <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

    <q-card flat bordered>
      <q-card-section>
        <div class="row items-center q-mb-sm">
          <div class="col text-h6">Todos os pagamentos</div>
          <q-btn flat color="primary" icon="file_download" label="Exportar CSV" :href="csvUrl" @click="onExport" />
        </div>
        <q-table
          :rows="rows"
          :columns="columns"
          row-key="id"
          :loading="loading"
          flat
          :rows-per-page-options="[20, 50, 100]"
          v-model:pagination="pagination"
          :rows-number="total"
          @request="onRequest"
        >
          <template #body-cell-amount="props">
            <q-td :props="props">{{ formatPrice(props.row.amount, props.row.currency) }}</q-td>
          </template>
          <template #body-cell-method="props">
            <q-td :props="props">
              <q-chip size="sm" :label="methodLabel(props.row.method)" />
            </q-td>
          </template>
          <template #body-cell-status="props">
            <q-td :props="props">
              <q-badge :color="statusColor(props.row.status)" :label="props.row.status" />
            </q-td>
          </template>
          <template #body-cell-paidAt="props">
            <q-td :props="props">{{ props.row.paidAt ? formatDate(props.row.paidAt) : '—' }}</q-td>
          </template>
        </q-table>
      </q-card-section>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { billingApi, type PaymentView, type AdminPaymentStats, http } from 'src/services/api'

const $q = useQuasar()
const rows = ref<PaymentView[]>([])
const stats = ref<AdminPaymentStats | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const total = ref(0)
const pagination = reactive({ page: 1, rowsPerPage: 20 })

const columns: QTableColumn<PaymentView>[] = [
  { name: 'paidAt', label: 'Pago em', field: 'paidAt', align: 'left' },
  { name: 'tenantName', label: 'Tenant', field: 'tenantName', align: 'left' },
  { name: 'amount', label: 'Valor', field: 'amount', align: 'right' },
  { name: 'method', label: 'Método', field: 'method', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'description', label: 'Descrição', field: 'description', align: 'left' },
  { name: 'recordedByEmail', label: 'Lançado por', field: 'recordedByEmail', align: 'left' },
]

function statusColor(s: string): string {
  switch (s) {
    case 'succeeded': return 'positive'
    case 'pending': return 'warning'
    case 'failed': return 'negative'
    case 'refunded': return 'info'
    case 'voided': return 'grey'
    case 'chargeback': return 'negative'
    default: return 'grey'
  }
}
function methodLabel(m: string): string {
  const map: Record<string, string> = {
    stripe_card: 'Stripe cartão', stripe_pix: 'Stripe Pix', stripe_boleto: 'Stripe boleto',
    manual_cash: 'Dinheiro', manual_bank_transfer: 'Transferência', manual_pix_external: 'Pix externo',
    manual_bonus: 'Bônus', manual_courtesy: 'Cortesia', manual_other: 'Outro',
  }
  return map[m] ?? m
}
function formatPrice(v: number, c: string): string {
  try { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: c }).format(v) }
  catch { return v.toFixed(2) }
}
function formatDate(d: string): string {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('pt-BR') } catch { return d }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await billingApi.listAllPayments(pagination.page - 1, pagination.rowsPerPage)
    rows.value = res.content
    total.value = res.totalElements
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 403
      ? 'Acesso restrito a sys_admin/sys_manager.'
      : 'Não foi possível carregar os pagamentos.'
  } finally {
    loading.value = false
  }
  try {
    stats.value = await billingApi.adminStats()
  } catch { /* stats não são críticos */ }
}

function onRequest(props: { pagination: { page: number; rowsPerPage: number } }) {
  pagination.page = props.pagination.page
  pagination.rowsPerPage = props.pagination.rowsPerPage
  void load()
}

function onExport() {
  try {
    const base = (http.defaults.baseURL ?? '')
    const url = `${base}/admin/billing/payments/export.csv?page=0&size=1000`
    window.open(url, '_blank')
  } catch {
    $q.notify({ message: 'Export ainda não implementado nesta fase.', color: 'info' })
  }
}

const csvUrl = '/admin/billing/payments/export.csv'

onMounted(() => {
  void load()
})
</script>
