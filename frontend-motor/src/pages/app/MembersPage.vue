<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant primeiro.
    </q-banner>

    <div v-else>
      <div class="row items-center q-mb-md">
        <div class="col">
          <h1 class="text-h5 q-my-none">Membros</h1>
          <p class="text-caption text-grey-7 q-mb-none">
            Convide vendedores e gerentes que terão acesso ao tenant.
          </p>
        </div>
        <div class="col-auto">
          <q-btn color="primary" icon="person_add" label="Convidar membro" unelevated @click="openInvite" />
        </div>
      </div>

      <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

      <q-table
        :rows="members"
        :columns="columns"
        row-key="id"
        :loading="loading"
        flat
        bordered
        :rows-per-page-options="[10, 25]"
      >
        <template #body-cell-role="props">
          <q-td :props="props">
            <q-badge :color="roleColor(props.row.role)" :label="props.row.role" />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn flat round dense icon="delete" color="negative" @click="confirmRemove(props.row)" />
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-md text-grey-6">Nenhum membro convidado ainda.</div>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="inviteOpen">
      <q-card style="min-width: 480px; max-width: 640px">
        <q-card-section>
          <div class="text-h6">Convidar membro</div>
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
              v-model="form.email"
              label="E-mail"
              type="email"
              outlined
              dense
              lazy-rules
              :rules="[(v: string) => !!v || 'Obrigatório']"
            />
            <q-select
              v-model="form.role"
              :options="roleOptions"
              label="Papel"
              outlined
              dense
              emit-value
              map-options
            />
            <q-select
              v-if="stores.length > 0"
              v-model="form.physicalStoreIds"
              :options="storeOptions"
              label="Lojas permitidas (vazio = todas)"
              outlined
              dense
              emit-value
              map-options
              multiple
              use-chips
            />
          </q-form>
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn unelevated color="primary" label="Convidar" :loading="saving" @click="onSubmit" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { tenantApi, type MemberView, type StoreView, type TenantUserRole } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const auth = useAuthStore()

const members = ref<MemberView[]>([])
const stores = ref<StoreView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const inviteOpen = ref(false)
const saving = ref(false)
const form = reactive({
  name: '',
  email: '',
  role: 'SELLER' as TenantUserRole satisfies TenantUserRole,
  physicalStoreIds: [] as string[],
})

const roleOptions = [
  { label: 'Vendedor (SELLER)', value: 'SELLER' },
  { label: 'Gerente (MANAGER)', value: 'MANAGER' },
  { label: 'Proprietário (OWNER)', value: 'OWNER' },
]

const storeOptions = computed(() =>
  stores.value.map((s) => ({ label: s.name, value: s.id }))
)

const columns: QTableColumn<MemberView>[] = [
  { name: 'name', label: 'Nome', field: 'name', align: 'left' },
  { name: 'email', label: 'E-mail', field: 'email', align: 'left' },
  { name: 'role', label: 'Papel', field: 'role', align: 'left' },
  { name: 'acceptedAt', label: 'Aceito em', field: 'acceptedAt', align: 'left', format: (v) => v ? new Date(v as string).toLocaleDateString('pt-BR') : '—' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]

function roleColor(role: string): string {
  switch (role) {
    case 'OWNER': return 'primary'
    case 'MANAGER': return 'secondary'
    case 'SELLER': return 'info'
    default: return 'grey'
  }
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [m, s] = await Promise.all([tenantApi.listMembers(), tenantApi.listStores()])
    members.value = m
    stores.value = s
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 403 ? 'Selecione um tenant para continuar.' : 'Não foi possível carregar membros.'
  } finally {
    loading.value = false
  }
}

function openInvite() {
  Object.assign(form, { name: '', email: '', role: 'SELLER', physicalStoreIds: [] })
  inviteOpen.value = true
}

async function onSubmit() {
  saving.value = true
  try {
    await tenantApi.inviteMember({
      name: form.name,
      email: form.email,
      role: form.role,
      ...(form.physicalStoreIds.length > 0 ? { physicalStoreIds: form.physicalStoreIds } : {}),
    })
    inviteOpen.value = false
    $q.notify({ message: 'Membro convidado.', color: 'positive' })
    void load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao convidar.', color: 'negative' })
  } finally {
    saving.value = false
  }
}

function confirmRemove(row: MemberView) {
  $q.dialog({
    title: 'Remover membro',
    message: `Remover ${row.name || row.email} do tenant?`,
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await tenantApi.removeMember(row.id)
        $q.notify({ message: 'Membro removido.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao remover.', color: 'negative' })
      }
    })()
  })
}

onMounted(() => {
  if (auth.currentTenantId.value) void load()
})
</script>
