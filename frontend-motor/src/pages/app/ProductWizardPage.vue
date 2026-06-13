<template>
  <q-page padding>
    <h1 class="text-h5 q-mb-md">Novo anúncio</h1>

    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant antes de criar anúncios.
    </q-banner>

    <q-banner v-else-if="stores.length === 0" class="bg-info text-white q-mb-md">
      Você ainda não tem lojas físicas cadastradas. <router-link :to="{ name: 'app-stores' }" class="text-white text-weight-bold">Cadastre uma loja</router-link> antes de continuar.
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
      <!-- ─── PASSO 1: Tipo de veículo ─── -->
      <q-step :name="1" title="Tipo" icon="directions_car" :done="step > 1">
        <p class="text-caption text-grey-7">Escolha o tipo de veículo que você está anunciando.</p>
        <div class="row q-gutter-md q-mt-md">
          <q-card
            v-for="r in realms"
            :key="r.value"
            class="cursor-pointer col-12 col-sm-4 col-md-2"
            flat
            bordered
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
          option-value="id"
          option-label="name"
          emit-value
          map-options
          label="Subcategoria"
          outlined
          dense
          class="q-mt-md"
          :rules="[(v: string | null) => !!v || 'Selecione a subcategoria']"
        />
        <q-stepper-navigation>
          <q-btn
            unelevated
            color="primary"
            label="Próximo"
            :disable="!canGoToStep2"
            @click="step = 2"
          />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 2: Dados do veículo ─── -->
      <q-step :name="2" title="Dados" icon="description" :done="step > 2">
        <div class="row q-col-gutter-md">
          <q-input v-model="form.brandSearch" label="Marca" outlined dense class="col-12 col-sm-6"
                   @update:model-value="onBrandSearch" use-input hide-selected fill-input
                   input-debounce="200">
            <template #append>
              <q-icon name="search" />
            </template>
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
            option-value="id"
            option-label="name"
            emit-value
            map-options
            label="Modelo"
            outlined
            dense
            class="col-12 col-sm-6"
          />
        </div>
        <div class="row q-col-gutter-md q-mt-sm">
          <q-input
            v-model.number="form.yearModel"
            type="number"
            label="Ano (modelo)"
            outlined
            dense
            class="col-6 col-sm-3"
            :rules="[(v: number | null) => (v != null && v >= 1950) || 'Obrigatório']"
          />
          <q-input
            v-model.number="form.yearBuild"
            type="number"
            label="Ano (fabricação)"
            outlined
            dense
            class="col-6 col-sm-3"
          />
          <q-input
            v-model.number="form.mileageKm"
            type="number"
            label="Quilometragem"
            outlined
            dense
            class="col-6 col-sm-3"
          />
          <q-select
            v-model="form.fuel"
            :options="fuelOptions"
            label="Combustível"
            outlined
            dense
            class="col-6 col-sm-3"
          />
          <q-select
            v-model="form.transmission"
            :options="transmissionOptions"
            label="Câmbio"
            outlined
            dense
            class="col-6 col-sm-3"
          />
        </div>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 1" />
          <q-btn unelevated color="primary" label="Próximo" :disable="!canGoToStep3" @click="step = 3" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 3: Descrição e preço ─── -->
      <q-step :name="3" title="Descrição" icon="edit" :done="step > 3">
        <q-input
          v-model="form.title"
          label="Título do anúncio"
          outlined
          dense
          lazy-rules
          :rules="[(v: string) => !!v || 'Obrigatório']"
        />
        <q-input
          v-model="form.description"
          label="Descrição"
          outlined
          dense
          type="textarea"
          autogrow
          class="q-mt-sm"
        />
        <div class="row q-col-gutter-md q-mt-sm">
          <q-input
            v-model.number="form.price"
            type="number"
            label="Preço (R$)"
            outlined
            dense
            class="col-6 col-sm-3"
            lazy-rules
            :rules="[(v: number | null) => (v != null && v > 0) || 'Obrigatório']"
          />
          <q-select
            v-model="form.currency"
            :options="['BRL']"
            label="Moeda"
            outlined
            dense
            class="col-6 col-sm-3"
            disable
          />
        </div>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 2" />
          <q-btn unelevated color="primary" label="Próximo" :disable="!canGoToStep4" @click="step = 4" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 4: Fotos ─── -->
      <q-step :name="4" title="Fotos" icon="photo_camera" :done="step > 4">
        <p class="text-caption text-grey-7">Envie fotos do veículo. A primeira foto será a capa.</p>
        <q-file
          v-model="pendingFiles"
          label="Selecionar fotos"
          outlined
          multiple
          accept="image/*"
          @update:model-value="onFilesSelected"
        />
        <div class="row q-gutter-sm q-mt-md">
          <div v-for="(img, i) in form.images" :key="img.id" class="position-relative">
            <q-img :src="img.url" :ratio="1" style="width: 120px; height: 120px;" />
            <q-btn
              dense
              round
              color="negative"
              icon="close"
              size="sm"
              class="absolute"
              style="top: 4px; right: 4px;"
              @click="removeImage(i)"
            />
            <q-badge v-if="img.isCover" color="primary" class="absolute" style="bottom: 4px; left: 4px;">
              Capa
            </q-badge>
            <q-btn
              v-else
              dense
              flat
              label="Definir como capa"
              size="sm"
              color="primary"
              class="absolute"
              style="bottom: 0; left: 0; right: 0; background: rgba(255,255,255,0.8);"
              @click="setCover(i)"
            />
          </div>
        </div>
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 3" />
          <q-btn unelevated color="primary" label="Próximo" :disable="!canGoToStep5" @click="step = 5" />
        </q-stepper-navigation>
      </q-step>

      <!-- ─── PASSO 5: Loja de origem + publicar ─── -->
      <q-step :name="5" title="Loja" icon="store" :done="step > 5">
        <p class="text-caption text-grey-7">Selecione a loja onde este veículo está exposto. O lead será enviado para essa loja.</p>
        <q-select
          v-model="form.physicalStoreId"
          :options="stores"
          option-value="id"
          option-label="name"
          emit-value
          map-options
          label="Loja"
          outlined
          dense
          lazy-rules
          :rules="[(v: string | null) => !!v || 'Selecione a loja']"
        />
        <q-toggle v-model="form.publish" label="Publicar imediatamente" class="q-mt-md" />
        <q-stepper-navigation>
          <q-btn flat label="Voltar" @click="step = 4" />
          <q-btn
            unelevated
            color="primary"
            label="Criar anúncio"
            :loading="creating"
            @click="onSubmit"
          />
          <q-btn
            v-if="productIdTemp"
            flat
            color="pink-6"
            icon="photo_camera"
            label="Postar no Instagram"
            class="q-ml-sm"
            :loading="postingIg"
            @click="onPostToInstagram"
          />
        </q-stepper-navigation>
      </q-step>
    </q-stepper>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRouter } from 'vue-router'
import {
  catalogApi, tenantApi, tenantProductApi,
  type VehicleRealm, type CategoryView, type VehicleBrandView, type VehicleModelView,
  type StoreView, type ProductImageView, type CreateProductRequest,
} from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const router = useRouter()
const auth = useAuthStore()

const step = ref(1)
const creating = ref(false)
const stores = ref<StoreView[]>([])
const subtypes = ref<CategoryView[]>([])
const brandOptions = ref<VehicleBrandView[]>([])
const modelOptions = ref<VehicleModelView[]>([])

interface WizardForm {
  realm: VehicleRealm | ''
  categoryId: string
  brandSearch: string
  brandId: number | undefined
  modelId: number | undefined
  yearModel: number | null
  yearBuild: number | null
  mileageKm: number | null
  fuel: string
  transmission: string
  title: string
  description: string
  price: number | null
  currency: string
  physicalStoreId: string
  publish: boolean
  images: ProductImageView[]
}

const form = reactive<WizardForm>({
  realm: '',
  categoryId: '',
  brandSearch: '',
  brandId: undefined,
  modelId: undefined,
  yearModel: null,
  yearBuild: null,
  mileageKm: null,
  fuel: 'Flex',
  transmission: 'Manual',
  title: '',
  description: '',
  price: null,
  currency: 'BRL',
  physicalStoreId: '',
  publish: true,
  images: [],
})

const pendingFiles = ref<File[]>([])
const postingIg = ref(false)
let productIdTemp: string | null = null

async function onPostToInstagram() {
  if (!productIdTemp) return
  postingIg.value = true
  try {
    const { integrationApi } = await import('src/services/api')
    const res = await integrationApi.publishToInstagram(productIdTemp)
    $q.notify({ message: `Publicado no Instagram! (mediaId: ${res.mediaId.slice(0, 10)}…)`, color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao postar no Instagram. Verifique a integração em /app/integracoes.', color: 'negative' })
  } finally {
    postingIg.value = false
  }
}

const realms = [
  { value: 'CAR' as VehicleRealm, label: 'Carro', icon: 'directions_car' },
  { value: 'MOTORCYCLE' as VehicleRealm, label: 'Moto', icon: 'two_wheeler' },
  { value: 'TRUCK' as VehicleRealm, label: 'Caminhão', icon: 'local_shipping' },
  { value: 'NAUTICAL' as VehicleRealm, label: 'Náutico', icon: 'sailing' },
  { value: 'BUS' as VehicleRealm, label: 'Ônibus', icon: 'directions_bus' },
]

const fuelOptions = ['Flex', 'Gasolina', 'Álcool', 'Diesel', 'Elétrico', 'Híbrido']
const transmissionOptions = ['Manual', 'Automático', 'Automatizado', 'CVT']

const canGoToStep2 = computed(() => !!form.realm && !!form.categoryId)
const canGoToStep3 = computed(() => !!form.yearModel)
const canGoToStep4 = computed(() => !!form.title && !!form.price && form.price > 0)
const canGoToStep5 = computed(() => form.images.length > 0)

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

async function onFilesSelected(files: File[]) {
  if (files.length === 0) return
  // Cria o anúncio como DRAFT primeiro para obter ID e poder enviar imagens
  if (!productIdTemp) {
    if (!canCreateTemp()) {
      $q.notify({ message: 'Preencha todos os passos até a foto (tenha tipo, dados, descrição e preço).', color: 'warning' })
      return
    }
    creating.value = true
    try {
      const created = await tenantProductApi.create(buildCreatePayload(false))
      productIdTemp = created.id
    } catch (e: unknown) {
      const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      $q.notify({ message: detail || 'Falha ao criar anúncio (rascunho).', color: 'negative' })
      creating.value = false
      return
    } finally {
      creating.value = false
    }
  }
  for (const file of files) {
    try {
      const isCover = form.images.length === 0
      const up = await tenantProductApi.uploadImage(productIdTemp, file, isCover)
      form.images.push(up.image)
      if (isCover) {
        form.images.forEach((img, i) => { if (i !== form.images.length - 1) img.isCover = false })
      }
    } catch (e: unknown) {
      const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      $q.notify({ message: detail || `Falha no upload de ${file.name}.`, color: 'negative' })
    }
  }
  pendingFiles.value = []
}

function canCreateTemp(): boolean {
  return !!form.realm && !!form.categoryId && !!form.yearModel && !!form.title && !!form.price && form.price > 0
}

function buildCreatePayload(publish: boolean): CreateProductRequest {
  const storeId = form.physicalStoreId || stores.value[0]?.id
  if (!storeId) {
    throw new Error('Loja não definida')
  }
  const payload: CreateProductRequest = {
    physicalStoreId: storeId,
    categoryId: form.categoryId,
    title: form.title,
    price: form.price ?? 0,
    currency: form.currency,
    publish,
  }
  if (form.brandId != null) payload.brandId = form.brandId
  if (form.modelId != null) payload.modelId = form.modelId
  if (form.description) payload.description = form.description
  if (form.yearModel != null) payload.yearModel = form.yearModel
  if (form.yearBuild != null) payload.yearBuild = form.yearBuild
  if (form.mileageKm != null) payload.mileageKm = form.mileageKm
  payload.fuel = form.fuel
  payload.transmission = form.transmission
  payload.countryCode = 'BR'
  return payload
}

function removeImage(i: number) {
  const img = form.images[i]
  if (!img) return
  if (productIdTemp) {
    void tenantProductApi.deleteImage(productIdTemp, img.id).catch(() => undefined)
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

async function onSubmit() {
  creating.value = true
  try {
    if (!productIdTemp) {
      const created = await tenantProductApi.create(buildCreatePayload(form.publish))
      productIdTemp = created.id
    } else if (form.publish) {
      await tenantProductApi.update(productIdTemp, { status: 'ACTIVE' })
    }
    if (productIdTemp && form.physicalStoreId) {
      await tenantProductApi.update(productIdTemp, { physicalStoreId: form.physicalStoreId })
    }
    $q.notify({ message: form.publish ? 'Anúncio publicado!' : 'Anúncio salvo como rascunho.', color: 'positive' })
    void router.push({ name: 'app-products' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao criar/publicar.', color: 'negative' })
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  if (!auth.currentTenantId.value) return
  try {
    stores.value = await tenantApi.listStores()
    const first = stores.value[0]
    if (first) {
      form.physicalStoreId = first.id
    }
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    if (status !== 403) $q.notify({ message: 'Não foi possível carregar suas lojas.', color: 'negative' })
  }
})
</script>

<style scoped>
.position-relative { position: relative; }
.absolute { position: absolute; }
</style>
