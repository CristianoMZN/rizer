<template>
  <div class="lead-dashboard">
    <!-- Stats row -->
    <div class="row q-gutter-md q-mb-lg">
      <q-card
        v-for="stat in stats"
        :key="stat.label"
        class="stat-card col"
        flat
        bordered
      >
        <q-card-section class="text-center">
          <q-icon :name="stat.icon" :color="stat.color" size="28px" />
          <p class="stat-value">{{ stat.value }}</p>
          <p class="stat-label">{{ stat.label }}</p>
        </q-card-section>
      </q-card>
    </div>

    <!-- Leads table -->
    <q-table
      :rows="leads"
      :columns="columns"
      row-key="id"
      flat
      bordered
      :pagination="{ rowsPerPage: 10 }"
    >
      <template #body-cell-status="propsCell">
        <q-td :props="propsCell">
          <q-badge :color="statusColor(propsCell.value)" :label="statusLabel(propsCell.value)" />
        </q-td>
      </template>
      <template #body-cell-actions="propsCell">
        <q-td :props="propsCell">
          <q-btn flat icon="call" size="sm" color="primary" :href="`tel:${propsCell.row.buyerPhone}`" />
          <q-btn flat icon="email" size="sm" color="secondary" :href="`mailto:${propsCell.row.buyerEmail}`" />
          <q-btn-dropdown flat icon="more_vert" size="sm">
            <q-list>
              <q-item
                v-for="s in nextStatuses(propsCell.row.status)"
                :key="s.value"
                clickable
                v-close-popup
                @click="updateStatus(propsCell.row.id, s.value)"
              >
                <q-item-section>{{ s.label }}</q-item-section>
              </q-item>
            </q-list>
          </q-btn-dropdown>
        </q-td>
      </template>
    </q-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import type { Lead, LeadStatus } from 'src/data/types'
import { api } from 'src/services/api'

interface Props { storeId: string }
const props = defineProps<Props>()

const leads = ref<Lead[]>([])

onMounted(async () => {
  leads.value = await api.getLeads(props.storeId)
})

const stats = computed(() => [
  { label: 'Novos', value: leads.value.filter((l) => l.status === 'new').length, icon: 'fiber_new', color: 'primary' },
  { label: 'Contatados', value: leads.value.filter((l) => l.status === 'contacted').length, icon: 'phone', color: 'secondary' },
  { label: 'Negociando', value: leads.value.filter((l) => l.status === 'negotiating').length, icon: 'handshake', color: 'warning' },
  { label: 'Ganhos', value: leads.value.filter((l) => l.status === 'closed_won').length, icon: 'check_circle', color: 'positive' },
])

const columns = [
  { name: 'buyerName', label: 'Cliente', field: 'buyerName', align: 'left' as const, sortable: true },
  { name: 'buyerPhone', label: 'Telefone', field: 'buyerPhone', align: 'left' as const },
  { name: 'vehicleId', label: 'Veículo', field: 'vehicleId', align: 'left' as const },
  { name: 'status', label: 'Status', field: 'status', align: 'left' as const, sortable: true },
  { name: 'createdAt', label: 'Data', field: (r: Lead) => new Date(r.createdAt).toLocaleDateString('pt-BR'), align: 'left' as const, sortable: true },
  { name: 'actions', label: 'Ações', field: 'actions', align: 'center' as const },
]

const STATUS_MAP: Record<LeadStatus, { color: string; label: string }> = {
  new: { color: 'primary', label: 'Novo' },
  contacted: { color: 'info', label: 'Contatado' },
  negotiating: { color: 'warning', label: 'Negociando' },
  closed_won: { color: 'positive', label: 'Ganho' },
  closed_lost: { color: 'negative', label: 'Perdido' },
}

function statusColor(s: LeadStatus) { return STATUS_MAP[s]?.color ?? 'grey' }
function statusLabel(s: LeadStatus) { return STATUS_MAP[s]?.label ?? s }

function nextStatuses(current: LeadStatus) {
  const all: { value: LeadStatus; label: string }[] = [
    { value: 'contacted', label: 'Marcar Contatado' },
    { value: 'negotiating', label: 'Marcar Negociando' },
    { value: 'closed_won', label: 'Marcar Ganho' },
    { value: 'closed_lost', label: 'Marcar Perdido' },
  ]
  return all.filter((s) => s.value !== current)
}

function updateStatus(id: string, status: LeadStatus) {
  const lead = leads.value.find((l) => l.id === id)
  if (lead) lead.status = status
}
</script>

<style scoped lang="scss">
.stat-card {
  border-radius: 12px;
  min-width: 100px;
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  margin: 8px 0 4px;
}

.stat-label {
  font-size: 12px;
  color: #9e9e9e;
  margin: 0;
}
</style>
