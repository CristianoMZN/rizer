<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant para gerenciar vendedores.
    </q-banner>

    <div v-else>
      <div class="row items-center q-mb-md">
        <div class="col">
          <h1 class="text-h5 q-my-none">Vendedores</h1>
          <p class="text-caption text-grey-7 q-mb-none">
            Vendedores, gerentes e proprietário da sua rede.
          </p>
        </div>
        <div class="col-auto">
          <q-btn
            v-if="role.canInviteMembers.value"
            color="primary"
            icon="person_add"
            label="Convidar membro"
            unelevated
            @click="openInvite"
          />
        </div>
      </div>

      <q-banner v-if="!role.canManageTenant.value && !role.canInviteMembers.value" class="bg-info text-white q-mb-md">
        Você é SELLER. Apenas o proprietário (OWNER) pode convidar, editar ou remover membros.
      </q-banner>

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
        <template #body-cell-avatar="props">
          <q-td :props="props">
            <q-avatar v-if="props.row.avatarUrl" size="32px">
              <img :src="props.row.avatarUrl" :alt="props.row.name || props.row.email">
            </q-avatar>
            <q-avatar v-else color="grey-3" text-color="grey-7" size="32px">
              {{ (props.row.name || props.row.email || '?').charAt(0).toUpperCase() }}
            </q-avatar>
          </q-td>
        </template>
        <template #body-cell-role="props">
          <q-td :props="props">
            <q-badge :color="roleColor(props.row.role)" :label="props.row.role" />
          </q-td>
        </template>
        <template #body-cell-stores="props">
          <q-td :props="props">
            <q-chip
              v-if="props.row.physicalStoreIds.length === 0"
              size="sm"
              color="positive"
              text-color="white"
              label="Todas"
            />
            <q-chip
              v-else
              v-for="sid in props.row.physicalStoreIds"
              :key="sid"
              size="sm"
              color="primary"
              text-color="white"
              :label="storeName(sid)"
            />
          </q-td>
        </template>
        <template #body-cell-actions="props">
          <q-td :props="props" class="q-gutter-xs">
            <q-btn
              v-if="role.canManageTenant.value"
              flat round dense icon="edit" @click="openEdit(props.row)"
            />
            <q-btn
              v-if="role.canManageTenant.value"
              flat round dense icon="delete" color="negative" @click="confirmRemove(props.row)"
            />
          </q-td>
        </template>
        <template #no-data>
          <div class="full-width text-center q-pa-md text-grey-6">
            Nenhum membro cadastrado. Clique em <strong>Convidar membro</strong>.
          </div>
        </template>
      </q-table>
    </div>

    <q-dialog v-model="inviteOpen">
      <q-card style="min-width: 520px; max-width: 720px">
        <q-card-section>
          <div class="text-h6">{{ editingId ? 'Editar membro' : 'Convidar membro' }}</div>
        </q-card-section>
        <q-card-section class="q-pt-none">
          <q-form @submit.prevent="onSubmit" class="q-gutter-sm">
            <q-input
              v-model="form.name"
              label="Nome"
              outlined dense lazy-rules
              :rules="[(v: string) => !!v || 'Obrigatório']"
            />
            <div class="row q-col-gutter-md">
              <q-input v-model="form.email" label="E-mail" type="email" outlined dense class="col-12 col-sm-7" lazy-rules :rules="[(v: string) => !!v || 'Obrigatório']" />
              <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-12 col-sm-5" mask="(##) #####-####" unmasked-value />
            </div>
            <q-input
              v-if="!editingId"
              v-model="form.password"
              label="Senha temporária"
              type="password"
              outlined dense
              hint="O vendedor poderá trocar depois. Mín. 6 caracteres."
              lazy-rules
              :rules="[(v: string) => !v || v.length >= 6 || 'Mín. 6 caracteres']"
            />
            <q-select
              v-model="form.role"
              :options="roleOptions"
              label="Papel"
              outlined dense emit-value map-options
            />
            <q-select
              v-if="stores.length > 0"
              v-model="form.physicalStoreIds"
              :options="storeOptions"
              label="Lojas permitidas (vazio = todas)"
              outlined dense emit-value map-options multiple use-chips
            />

            <div>
              <div class="text-caption q-mb-xs">Foto de perfil</div>
              <q-file
                v-model="avatarFile"
                outlined dense accept="image/*"
                @update:model-value="onAvatarSelected"
              >
                <template #prepend><q-icon name="person" /></template>
              </q-file>
              <q-img v-if="form.avatarUrl" :src="form.avatarUrl" :ratio="1" class="q-mt-sm" style="max-width:96px" />
            </div>

            <q-banner v-if="formError" class="bg-negative text-white">{{ formError }}</q-banner>
          </q-form>
        </q-card-section>
        <q-card-actions align="right" class="q-pa-md">
          <q-btn flat label="Cancelar" v-close-popup />
          <q-btn unelevated color="primary" :label="editingId ? 'Salvar' : 'Convidar'" :loading="saving" @click="onSubmit" />
        </q-card-actions>
      </q-card>
    </q-dialog>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { tenantApi, mediaApi, type MemberView, type StoreView, type TenantUserRole, type InviteMemberRequest } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'
import { useTenantRole } from 'src/composables/useTenantRole'

const $q = useQuasar()
const auth = useAuthStore()
const role = useTenantRole()

const members = ref<MemberView[]>([])
const stores = ref<StoreView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const inviteOpen = ref(false)
const editingId = ref<string | null>(null)
const saving = ref(false)
const formError = ref<string | null>(null)
const avatarFile = ref<File | null>(null)

const form = reactive<{
  name: string
  email: string
  whatsapp: string
  password: string
  role: TenantUserRole
  physicalStoreIds: string[]
  avatarUrl: string
}>({
  name: '', email: '', whatsapp: '', password: '',
  role: 'SELLER',
  physicalStoreIds: [],
  avatarUrl: '',
})

const roleOptions = [
  { label: 'Vendedor (SELLER)', value: 'SELLER' },
  { label: 'Gerente (MANAGER)', value: 'MANAGER' },
  { label: 'Proprietário (OWNER)', value: 'OWNER' },
]

const storeOptions = computed(() => stores.value.map((s) => ({ label: s.name, value: s.id })))

const columns: QTableColumn<MemberView>[] = [
  { name: 'avatar', label: '', field: 'avatarUrl', align: 'center' },
  { name: 'name', label: 'Nome', field: 'name', align: 'left' },
  { name: 'whatsapp', label: 'WhatsApp', field: 'whatsapp', align: 'left' },
  { name: 'email', label: 'E-mail', field: 'email', align: 'left' },
  { name: 'role', label: 'Papel', field: 'role', align: 'left' },
  { name: 'stores', label: 'Lojas', field: 'physicalStoreIds', align: 'left' },
  { name: 'actions', label: '', field: 'id', align: 'right' },
]

function roleColor(r: string): string {
  switch (r) {
    case 'OWNER': return 'primary'
    case 'MANAGER': return 'secondary'
    case 'SELLER': return 'info'
    default: return 'grey'
  }
}

function storeName(id: string): string {
  return stores.value.find((s) => s.id === id)?.name ?? id.slice(0, 8)
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
  editingId.value = null
  formError.value = null
  Object.assign(form, {
    name: '', email: '', whatsapp: '', password: '',
    role: 'SELLER' as TenantUserRole,
    physicalStoreIds: [] as string[],
    avatarUrl: '',
  })
  avatarFile.value = null
  inviteOpen.value = true
}

function openEdit(m: MemberView) {
  editingId.value = m.id
  formError.value = null
  Object.assign(form, {
    name: m.name ?? '',
    email: m.email ?? '',
    whatsapp: m.whatsapp ?? '',
    password: '',
    role: m.role,
    physicalStoreIds: [...m.physicalStoreIds],
    avatarUrl: m.avatarUrl ?? '',
  })
  avatarFile.value = null
  inviteOpen.value = true
}

async function onAvatarSelected(f: File | null) {
  if (!f) return
  try {
    const up = await mediaApi.uploadImage(f, 'seller-avatar')
    form.avatarUrl = up.url
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload da foto.', color: 'negative' })
  } finally {
    avatarFile.value = null
  }
}

async function onSubmit() {
  saving.value = true
  formError.value = null
  try {
    if (editingId.value) {
      await tenantApi.updateMember(editingId.value, {
        role: form.role,
        physicalStoreIds: form.physicalStoreIds,
      })
      $q.notify({ message: 'Membro atualizado.', color: 'positive' })
    } else {
      const req: InviteMemberRequest = {
        email: form.email,
        name: form.name,
        role: form.role,
      }
      if (form.physicalStoreIds.length > 0) req.physicalStoreIds = form.physicalStoreIds
      if (form.whatsapp) req.whatsapp = form.whatsapp
      if (form.avatarUrl) req.avatarUrl = form.avatarUrl
      if (form.password) req.password = form.password
      await tenantApi.inviteMember(req)
      $q.notify({ message: 'Membro convidado.', color: 'positive' })
    }
    inviteOpen.value = false
    void load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    formError.value = detail || 'Falha ao salvar.'
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
