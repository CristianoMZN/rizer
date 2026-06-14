<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant antes de criar anúncios.
    </q-banner>

    <q-banner v-else-if="stores.length === 0" class="bg-info text-white q-mb-md">
      Você ainda não tem lojas físicas cadastradas. <router-link :to="{ name: 'app-company' }" class="text-white text-weight-bold">Cadastre uma loja</router-link> antes de continuar.
    </q-banner>

    <q-stepper
      v-else
      v-model="step"
      header-nav
      color="primary"
      animated
      :contracted="$q.screen.lt.md"
      class="shadow-0"
    >
      <!-- ─── PASSO 1: Título ─── -->
      <q-step :name="1" title="Título" icon="title" :done="step > 1">
        <p class="text-caption text-grey-7">
          Rascunho criado. Você pode sair e voltar — salvamos automaticamente a cada passo.
        </p>
        <q-input
          v-model="form.title"
          label="Título do anúncio"
          outlined dense
          autofocus
          lazy-rules
          :rules="[(v: string) => !!v || 'Obrigatório']"
        />
        <q-banner v-if="!productId" class="bg-info text-white q-mt-md">
          <q-spinner size="1em" class="q-mr-sm" /> Criando rascunho...
        </q-banner>
        <q-stepper-navigation>
          <q-btn
            unelevated color="primary" label="Próximo"
            :disable="!form.title || !productId"
            @click="onStepForward"
          />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 2: Dados do veículo ─── -->
      <q-step :name="2" title="Dados" icon="description" :done="step > 2">
        <div class="row q-gutter-md">
          <q-card
            v-for="r in realms"
            :key="r.value"
            class="cursor-pointer col-12 col-sm-4 col-md-2"
            flat bordered
            :class="{ 'bg-primary text-white': form.realm === r.value }"
            @click="selectRealm(r.value)"
          >
            <q-card-section class="text-center">
              <q-icon :name="r.icon" size="40px" />
              <div class="text-subtitle2 q-mt-sm">{{ r.label }}</div>
            </q-card-section>
          </q-card>
        </div>
        <q-select
          v-if="form.realm"
          v-model="form.categoryId"
          :options="subtypes"
          option-value="id" option-label="name"
          emit-value map-options
          label="Subcategoria"
          outlined dense
          class="q-mt-md"
          :rules="[(v: string | null) => !!v || 'Selecione a subcategoria']"
        />
        <div class="row q-col-gutter-md q-mt-sm">
          <q-input
            v-model="form.brandSearch" label="Marca" outlined dense class="col-12 col-sm-6"
            @update:model-value="onBrandSearch" use-input hide-selected fill-input input-debounce="200"
          >
            <template #append><q-icon name="search" /></template>
            <q-list v-if="brandOptions.length > 0" dense>
              <q-item v-for="b in brandOptions" :key="b.id" clickable @click="selectBrand(b)">
                <q-item-section>{{ b.name }}</q-item-section>
              </q-item>
            </q-list>
          </q-input>
          <q-select
            v-if="form.brandId"
            v-model="form.modelId"
            :options="modelOptions"
            option-value="id" option-label="name"
            emit-value map-options
            label="Modelo" outlined dense class="col-12 col-sm-6"
          />
        </div>
        <div class="row q-col-gutter-md q-mt-sm">
          <q-input v-model.number="form.yearModel" type="number" label="Ano (modelo)" outlined dense class="col-6 col-sm-3"
                   :rules="[(v: number | null) => (v != null && v >= 1950) || 'Obrigatório']" />
          <q-input v-model.number="form.yearBuild" type="number" label="Ano (fabricação)" outlined dense class="col-6 col-sm-3" />
          <q-input v-model.number="form.mileageKm" type="number" label="Quilometragem" outlined dense class="col-6 col-sm-3" />
          <q-input v-model="form.color" label="Cor" outlined dense class="col-6 col-sm-3" />
          <q-select v-model="form.fuel" :options="fuelOptions" label="Combustível" outlined dense class="col-6 col-sm-3" />
          <q-select v-model="form.transmission" :options="transmissionOptions" label="Câmbio" outlined dense class="col-6 col-sm-3" />
        </div>
        <div class="row q-col-gutter-md q-mt-sm">
          <q-input v-model="form.plate" label="Placa" outlined dense class="col-6 col-sm-3" />
          <q-input v-model="form.renavam" label="Renavam" outlined dense class="col-6 col-sm-3" />
        </div>
        <q-input
          v-model="form.description"
          label="Descrição"
          outlined dense type="textarea" autogrow
          class="q-mt-sm"
        />
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 1" />
          <q-btn unelevated color="primary" label="Próximo" :disable="!canGoToStep3" @click="onStepForward" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 3: Imagens ─── -->
      <q-step :name="3" title="Imagens" icon="photo_camera" :done="step > 3">
        <p class="text-caption text-grey-7">Envie fotos do veículo. A primeira foto será a capa.</p>
        <q-file
          v-model="pendingFiles"
          label="Selecionar fotos"
          outlined multiple accept="image/*"
          :disable="!productId"
          @update:model-value="onFilesSelected"
        />
        <div class="row q-gutter-sm q-mt-md">
          <div v-for="(img, i) in form.images" :key="img.id" class="position-relative">
            <q-img :src="img.url" :ratio="1" style="width: 120px; height: 120px;" />
            <q-btn
              dense round color="negative" icon="close" size="sm"
              class="absolute" style="top: 4px; right: 4px;"
              @click="removeImage(i)"
            />
            <q-badge v-if="img.isCover" color="primary" class="absolute" style="bottom: 4px; left: 4px;">Capa</q-badge>
            <q-btn
              v-else
              dense flat label="Definir como capa" size="sm" color="primary"
              class="absolute" style="bottom: 0; left: 0; right: 0; background: rgba(255,255,255,0.8);"
              @click="setCover(i)"
            />
          </div>
        </div>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 2" />
          <q-btn unelevated color="primary" label="Próximo" :disable="form.images.length === 0" @click="onStepForward" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 4: Loja ─── -->
      <q-step :name="4" title="Loja" icon="store" :done="step > 4">
        <p class="text-caption text-grey-7">Selecione a loja onde este veículo está exposto.</p>
        <q-select
          v-model="form.physicalStoreId"
          :options="availableStores"
          option-value="id" option-label="name"
          emit-value map-options
          label="Loja"
          outlined dense
          lazy-rules
          :rules="[(v: string | null) => !!v || 'Selecione a loja']"
        />
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 3" />
          <q-btn unelevated color="primary" label="Próximo" :disable="!form.physicalStoreId" @click="onStepForward" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 5: Vendedor ─── -->
      <q-step :name="5" title="Vendedor" icon="person" :done="step > 5">
        <p class="text-caption text-grey-7">Defina o vendedor responsável. Se deixar em branco, o WhatsApp da loja será exibido no anúncio.</p>
        <q-select
          v-model="form.sellerUserId"
          :options="sellerOptions"
          option-value="value" option-label="label"
          emit-value map-options
          label="Vendedor"
          outlined dense clearable
          :disable="isCurrentUserSeller"
        />
        <q-banner v-if="isCurrentUserSeller" class="bg-info text-white q-mt-sm">
          Você é SELLER. O vendedor deste anúncio será você.
        </q-banner>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 4" />
          <q-btn unelevated color="primary" label="Próximo" @click="onStepForward" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 6: Confirmar ─── -->
      <q-step :name="6" title="Publicar" icon="publish" :done="step > 6">
        <q-card flat bordered>
          <q-card-section>
            <div class="text-overline">Resumo</div>
            <q-list dense>
              <q-item><q-item-section>Título</q-item-section><q-item-section side>{{ form.title || '—' }}</q-item-section></q-item>
              <q-item><q-item-section>Marca / Modelo</q-item-section><q-item-section side>{{ brandLabel }} {{ modelLabel }}</q-item-section></q-item>
              <q-item><q-item-section>Ano</q-item-section><q-item-section side>{{ form.yearModel || '—' }}</q-item-section></q-item>
              <q-item><q-item-section>Loja</q-item-section><q-item-section side>{{ storeLabel }}</q-item-section></q-item>
              <q-item><q-item-section>Vendedor</q-item-section><q-item-section side>{{ sellerLabel }}</q-item-section></q-item>
            </q-list>
          </q-card-section>
        </q-card>
        <q-banner v-if="!canPublish" class="bg-info text-white q-mt-md">
          <q-icon name="info" class="q-mr-sm" /> Você é SELLER. Apenas gerentes ou proprietários podem publicar. Seu anúncio será salvo como rascunho.
        </q-banner>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 5" />
          <q-btn
            unelevated color="warning" label="Salvar como rascunho"
            :loading="saving" @click="onFinalize('DRAFT')"
          />
          <q-btn
            unelevated color="primary" :label="canPublish ? 'Publicar anúncio' : 'Publicar (apenas gerente+)'"
            :loading="saving" :disable="!canPublish"
            class="q-ml-sm"
            @click="onFinalize('ACTIVE')"
          />
        </q-stepper-navigation>
      </q-step>
    </q-stepper>

    <q-page-sticky position="bottom-right" :offset="[18, 18]">
      <q-btn
        fab color="secondary" icon="save" :loading="savingManual"
        :disable="!productId"
        @click="onManualSave"
      >
        <q-tooltip>Salvar manualmente</q-tooltip>
      </q-btn>
    </q-page-sticky>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'
import {
  catalogApi, tenantApi, tenantProductApi, settingsApi,
  type VehicleRealm, type CategoryView, type VehicleBrandView, type VehicleModelView,
  type StoreView, type ProductImageView, type ProductView,
  type CreateProductRequest, type UpdateProductRequest,
} from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'
import { useTenantRole } from 'src/composables/useTenantRole'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()
const role = useTenantRole()

interface Props {
  editId?: string
}
const props = defineProps<Props>()

const step = ref(1)
const saving = ref(false)
const savingManual = ref(false)
const stores = ref<StoreView[]>([])
const subtypes = ref<CategoryView[]>([])
const brandOptions = ref<VehicleBrandView[]>([])
const modelOptions = ref<VehicleModelView[]>([])
const members = ref<{ userId: string; name?: string; email?: string; role: string }[]>([])

const productId = ref<string | null>(props.editId ?? null)

interface WizardForm {
  realm: VehicleRealm | ''
  categoryId: string
  brandSearch: string
  brandId: number | undefined
  modelName: string
  modelId: number | undefined
  yearModel: number | null
  yearBuild: number | null
  mileageKm: number | null
  fuel: string
  transmission: string
  color: string
  plate: string
  renavam: string
  title: string
  description: string
  price: number
  currency: string
  physicalStoreId: string
  sellerUserId: string | null
  images: ProductImageView[]
}

const form = reactive<WizardForm>({
  realm: '',
  categoryId: '',
  brandSearch: '',
  brandId: undefined,
  modelName: '',
  modelId: undefined,
  yearModel: null,
  yearBuild: null,
  mileageKm: null,
  fuel: 'Flex',
  transmission: 'Manual',
  color: '',
  plate: '',
  renavam: '',
  title: props.editId ? '' : '',
  description: '',
  price: 0,
  currency: 'BRL',
  physicalStoreId: '',
  sellerUserId: null,
  images: [],
})

const pendingFiles = ref<File[]>([])

const isCurrentUserSeller = computed(() => role.isSeller.value)
const canPublish = computed(() => role.canPublish.value)

const canGoToStep3 = computed(() => !!form.realm && !!form.categoryId && !!form.yearModel)

const availableStores = computed(() => {
  // Se for SELLER e tiver lojas restritas, filtra
  if (role.isSeller.value && auth.currentMembership.value) {
    const ids = auth.currentMembership.value.physicalStoreIds ?? []
    if (ids.length > 0) return stores.value.filter((s) => ids.includes(s.id))
  }
  return stores.value
})

const sellerOptions = computed(() => {
  const list = members.value.filter((m) => m.role === 'SELLER')
  return [
    { label: '— Sem vendedor (usar whatsapp da loja) —', value: null as string | null },
    ...list.map((m) => ({ label: m.name || m.email || m.userId, value: m.userId })),
  ]
})

const brandLabel = computed(() => {
  const b = brandOptions.value.find((x) => x.id === form.brandId)
  return b?.name || ''
})
const modelLabel = computed(() => modelOptions.value.find((x) => x.id === form.modelId)?.name || form.modelName || '')
const storeLabel = computed(() => stores.value.find((x) => x.id === form.physicalStoreId)?.name || '—')
const sellerLabel = computed(() => {
  if (!form.sellerUserId) return '— (whatsapp da loja)'
  const m = members.value.find((x) => x.userId === form.sellerUserId)
  return m?.name || m?.email || form.sellerUserId
})

const realms = [
  { value: 'CAR' as VehicleRealm, label: 'Carro', icon: 'directions_car' },
  { value: 'MOTORCYCLE' as VehicleRealm, label: 'Moto', icon: 'two_wheeler' },
  { value: 'TRUCK' as VehicleRealm, label: 'Caminhão', icon: 'local_shipping' },
  { value: 'NAUTICAL' as VehicleRealm, label: 'Náutico', icon: 'sailing' },
  { value: 'BUS' as VehicleRealm, label: 'Ônibus', icon: 'directions_bus' },
]

const fuelOptions = ['Flex', 'Gasolina', 'Álcool', 'Diesel', 'Elétrico', 'Híbrido']
const transmissionOptions = ['Manual', 'Automático', 'Automatizado', 'CVT']

async function selectRealm(realm: VehicleRealm) {
  form.realm = realm
  form.categoryId = ''
  subtypes.value = await catalogApi.listSubtypes('BR', realm)
}

async function onBrandSearch(value: string | number | null) {
  const v = (value ?? '').toString()
  if (!v || v.length < 1 || !form.realm) {
    brandOptions.value = []
    return
  }
  const all = await catalogApi.listBrands(form.realm)
  const term = v.toLowerCase()
  brandOptions.value = all.filter((b) => b.name.toLowerCase().includes(term)).slice(0, 10)
}

function selectBrand(b: VehicleBrandView) {
  form.brandSearch = b.name
  form.brandId = b.id
  form.modelId = undefined
  brandOptions.value = []
  void catalogApi.listModels(b.id).then((m) => { modelOptions.value = m })
}

async function ensureDraft() {
  if (productId.value) return
  const firstStore = availableStores.value[0]
  if (!firstStore) return
  try {
    const draft = await tenantProductApi.createDraft(firstStore.id)
    productId.value = draft.id
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao criar rascunho.', color: 'negative' })
  }
}

function buildPatch(): UpdateProductRequest {
  const patch: UpdateProductRequest = {}
  if (form.title) patch.title = form.title
  if (form.description) patch.description = form.description
  if (form.price > 0) patch.price = form.price
  if (form.currency) patch.currency = form.currency
  if (form.physicalStoreId) patch.physicalStoreId = form.physicalStoreId
  if (form.categoryId) patch.categoryId = form.categoryId
  if (form.brandId != null) patch.brandId = form.brandId
  if (form.modelId != null) patch.modelId = form.modelId
  if (form.yearModel != null) patch.yearModel = form.yearModel
  if (form.yearBuild != null) patch.yearBuild = form.yearBuild
  if (form.mileageKm != null) patch.mileageKm = form.mileageKm
  if (form.fuel) patch.fuel = form.fuel
  if (form.transmission) patch.transmission = form.transmission
  if (form.sellerUserId) patch.sellerUserId = form.sellerUserId

  const attrs: Record<string, unknown> = {}
  if (form.color) attrs.color = form.color
  if (form.plate) attrs.plate = form.plate
  if (form.renavam) attrs.renavam = form.renavam
  if (Object.keys(attrs).length > 0) patch.attributes = attrs
  return patch
}

async function autosave() {
  if (!productId.value) return
  try {
    await tenantProductApi.update(productId.value, buildPatch())
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  }
}

async function onStepForward() {
  await ensureDraft()
  if (!productId.value) return
  await autosave()
  step.value++
}

async function onManualSave() {
  await ensureDraft()
  if (!productId.value) return
  savingManual.value = true
  try {
    await tenantProductApi.update(productId.value, buildPatch())
    $q.notify({ message: 'Salvo.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  } finally {
    savingManual.value = false
  }
}

async function onFilesSelected(files: File[]) {
  if (files.length === 0) return
  await ensureDraft()
  if (!productId.value) return
  for (const file of files) {
    try {
      const isCover = form.images.length === 0
      const up = await tenantProductApi.uploadImage(productId.value, file, isCover)
      form.images.push(up.image)
    } catch (e: unknown) {
      const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      $q.notify({ message: detail || `Falha no upload de ${file.name}.`, color: 'negative' })
    }
  }
  pendingFiles.value = []
}

async function removeImage(i: number) {
  const img = form.images[i]
  if (!img) return
  if (productId.value) {
    void tenantProductApi.deleteImage(productId.value, img.id).catch(() => undefined)
  }
  form.images.splice(i, 1)
  if (form.images.length > 0 && !form.images.some((x) => x.isCover)) {
    const first = form.images[0]
    if (first) first.isCover = true
  }
}

function setCover(i: number) {
  form.images.forEach((img, idx) => { if (img) img.isCover = idx === i })
}

async function onFinalize(status: 'DRAFT' | 'ACTIVE') {
  await ensureDraft()
  if (!productId.value) return
  saving.value = true
  try {
    await tenantProductApi.update(productId.value, { ...buildPatch(), status })
    $q.notify({ message: status === 'ACTIVE' ? 'Anúncio publicado!' : 'Anúncio salvo como rascunho.', color: 'positive' })
    void router.push({ name: 'app-products' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  } finally {
    saving.value = false
  }
}

async function loadExisting() {
  if (!props.editId) return
  try {
    const p: ProductView = await tenantProductApi.get(props.editId)
    productId.value = p.id
    form.title = p.title ?? ''
    form.description = p.description ?? ''
    form.price = p.price ?? 0
    form.currency = p.currency ?? 'BRL'
    form.physicalStoreId = p.physicalStoreId
    form.categoryId = p.categoryId
    form.brandId = p.brandId ?? undefined
    form.modelId = p.modelId ?? undefined
    form.yearModel = p.yearModel ?? null
    form.yearBuild = p.yearBuild ?? null
    form.mileageKm = p.mileageKm ?? null
    form.fuel = p.fuel ?? 'Flex'
    form.transmission = p.transmission ?? 'Manual'
    form.realm = (p.realm as VehicleRealm) || 'CAR'
    form.sellerUserId = p.sellerUserId ?? null
    form.images = [...p.images]
    if (p.attributes) {
      form.color = (p.attributes.color as string) || ''
      form.plate = (p.attributes.plate as string) || ''
      form.renavam = (p.attributes.renavam as string) || ''
    }
    if (form.realm) {
      subtypes.value = await catalogApi.listSubtypes('BR', form.realm)
    }
    if (form.brandId) {
      modelOptions.value = await catalogApi.listModels(form.brandId)
    }
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao carregar anúncio.', color: 'negative' })
  }
}

onMounted(async () => {
  if (!auth.currentTenantId.value) return
  try {
    const [ss, mm, settings] = await Promise.all([
      tenantApi.listStores(),
      tenantApi.listMembers(),
      settingsApi.get().catch(() => null),
    ])
    stores.value = ss
    members.value = mm as typeof members.value
    if (settings) {
      // SELLER auto-preenchido
      if (role.isSeller.value && auth.user.value) {
        form.sellerUserId = auth.user.value.id
      }
    }
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status !== 403) $q.notify({ message: 'Não foi possível carregar dados básicos.', color: 'negative' })
  }
  if (props.editId) {
    await loadExisting()
  } else {
    await ensureDraft()
  }
})

watch(() => form.title, () => {
  // autosave debitado
})

void form // suppress unused (used via reactive)
</script>

<style scoped>
.position-relative { position: relative; }
.absolute { position: absolute; }
</style>
