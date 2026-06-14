<template>
  <q-dialog v-model="openProxy" persistent>
    <q-card style="min-width: 600px; max-width: 800px">
      <q-card-section class="row items-center">
        <div class="text-h6">{{ editingId ? 'Editar loja' : 'Nova loja' }}</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>
      <q-card-section class="q-pt-none">
        <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto" />
        <q-form v-else @submit.prevent="onSubmit" class="q-gutter-sm">
          <q-input v-model="form.name" label="Nome" outlined dense lazy-rules :rules="[(v: string) => !!v || 'Obrigatório']" />
          <q-input v-model="form.slug" label="Slug (opcional)" outlined dense hint="Ex.: matriz-centro, filial-zona-sul" />
          <q-toggle v-model="form.isBranch" label="É uma filial?" />
          <template v-if="form.isBranch">
            <q-input v-model="form.cnpj" label="CNPJ da filial" outlined dense />
            <q-input v-model="form.legalName" label="Razão social da filial" outlined dense />
          </template>
          <div class="row q-col-gutter-md">
            <q-input v-model="form.phone" label="Telefone" outlined dense class="col-6 col-sm-4" mask="(##) #####-####" unmasked-value />
            <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-6 col-sm-4" mask="(##) #####-####" unmasked-value />
            <q-input v-model="form.adminPhone" label="Tel. financeiro" outlined dense class="col-12 col-sm-4" mask="(##) #####-####" unmasked-value />
          </div>
          <q-input v-model="form.email" label="E-mail" type="email" outlined dense />

          <q-separator class="q-my-sm" />
          <div class="text-overline">Endereço</div>
          <div class="row q-col-gutter-md items-end">
            <q-input v-model="form.addressZipCode" label="CEP" outlined dense class="col-6 col-sm-3" mask="#####-###" unmasked-value>
              <template #append>
                <q-btn flat dense round icon="search" size="sm" @click="onCepLookup" />
              </template>
            </q-input>
            <q-btn outline color="primary" icon="my_location" label="Geo" :loading="geo.loading.value" @click="onUseGeo" class="col-6 col-sm-3" />
          </div>
          <q-banner v-if="cepError" class="bg-warning text-black q-mt-xs">{{ cepError }}</q-banner>
          <div class="row q-col-gutter-md">
            <q-input v-model="form.addressStreet" label="Logradouro" outlined dense class="col-12 col-sm-6" />
            <q-input v-model="form.addressNumber" label="Número" outlined dense class="col-4 col-sm-2" />
            <q-input v-model="form.addressComplement" label="Complemento" outlined dense class="col-8 col-sm-4" />
            <q-input v-model="form.addressNeighborhood" label="Bairro" outlined dense class="col-12 col-sm-4" />
            <q-input v-model="form.addressCity" label="Cidade" outlined dense class="col-12 col-sm-6" />
            <q-input v-model="form.addressState" label="UF" outlined dense class="col-6 col-sm-2" />
            <q-input v-model.number="form.latitude" label="Latitude" type="number" outlined dense class="col-6 col-sm-3" step="0.000001" />
            <q-input v-model.number="form.longitude" label="Longitude" type="number" outlined dense class="col-6 col-sm-3" step="0.000001" />
          </div>

          <q-separator class="q-my-sm" />
          <div class="text-overline">Banner</div>
          <q-file v-model="bannerFile" label="Selecionar banner" outlined dense accept="image/*" @update:model-value="onBannerSelected" />
          <q-img v-if="form.bannerUrl" :src="form.bannerUrl" :ratio="16/9" class="q-mt-sm" style="max-width:320px" />

          <q-toggle v-if="editingId" v-model="form.isActive" label="Ativa" />
          <q-banner v-if="error" class="bg-negative text-white">{{ error }}</q-banner>
        </q-form>
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat label="Cancelar" v-close-popup />
        <q-btn unelevated color="primary" :label="editingId ? 'Salvar' : 'Criar'" :loading="saving" @click="onSubmit" />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch, reactive } from 'vue'
import { useQuasar } from 'quasar'
import {
  tenantApi, mediaApi, type StoreView, type CreateStoreRequest, type UpdateStoreRequest,
} from 'src/services/api'
import { useCepLookup } from 'src/composables/useCepLookup'
import { useGeolocation } from 'src/composables/useGeolocation'

interface Props {
  modelValue: boolean
  editingId: string | null
}
const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'saved'): void
}>()

const $q = useQuasar()
const cep = useCepLookup()
const geo = useGeolocation()
const cepError = ref<string | null>(null)

const openProxy = ref(props.modelValue)
watch(() => props.modelValue, (v) => openProxy.value = v)
watch(openProxy, (v) => emit('update:modelValue', v))

const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const bannerFile = ref<File | null>(null)

const form = reactive<CreateStoreRequest & { isActive?: boolean }>({
  name: '', slug: '',
  phone: '', whatsapp: '', email: '', adminPhone: '',
  cnpj: '', legalName: '',
  bannerUrl: '',
  isBranch: false, isMain: false,
  addressZipCode: '', addressStreet: '', addressNumber: '', addressComplement: '',
  addressNeighborhood: '', addressCity: '', addressState: '',
  latitude: null as number | null,
  longitude: null as number | null,
  isActive: true,
})

async function loadIfEditing() {
  if (!props.editingId) return
  loading.value = true
  try {
    const stores = await tenantApi.listStores()
    const s: StoreView | undefined = stores.find((x) => x.id === props.editingId)
    if (s) {
      Object.assign(form, {
        name: s.name,
        slug: s.slug,
        phone: s.phone ?? '',
        whatsapp: s.whatsapp ?? '',
        email: s.email ?? '',
        adminPhone: s.adminPhone ?? '',
        cnpj: s.cnpj ?? '',
        legalName: s.legalName ?? '',
        bannerUrl: s.bannerUrl ?? '',
        isBranch: s.isBranch,
        isMain: s.isMain,
        isActive: s.isActive,
        addressZipCode: s.addressZipCode ?? '',
        addressStreet: s.addressStreet ?? '',
        addressNumber: s.addressNumber ?? '',
        addressComplement: s.addressComplement ?? '',
        addressNeighborhood: s.addressNeighborhood ?? '',
        addressCity: s.addressCity ?? '',
        addressState: s.addressState ?? '',
        latitude: s.latitude ?? null,
        longitude: s.longitude ?? null,
      })
    }
  } finally {
    loading.value = false
  }
}

watch(() => props.modelValue, (v) => {
  if (v) {
    error.value = null
    cepError.value = null
    bannerFile.value = null
    if (props.editingId) {
      void loadIfEditing()
    } else {
      Object.assign(form, {
        name: '', slug: '',
        phone: '', whatsapp: '', email: '', adminPhone: '',
        cnpj: '', legalName: '', bannerUrl: '',
        isBranch: false, isMain: false,
        addressZipCode: '', addressStreet: '', addressNumber: '', addressComplement: '',
        addressNeighborhood: '', addressCity: '', addressState: '',
        latitude: null, longitude: null,
        isActive: true,
      })
    }
  }
})

async function onCepLookup() {
  cepError.value = null
  const v = await cep.lookup(form.addressZipCode ?? '')
  if (!v) { cepError.value = 'CEP não encontrado.'; return }
  if (v.street) form.addressStreet = v.street
  if (v.neighborhood) form.addressNeighborhood = v.neighborhood
  if (v.city) form.addressCity = v.city
  if (v.state) form.addressState = v.state
  if (v.complement) form.addressComplement = v.complement
  if (v.latitude != null) form.latitude = v.latitude
  if (v.longitude != null) form.longitude = v.longitude
}

async function onUseGeo() {
  const c = await geo.getCurrent()
  if (!c) {
    $q.notify({ message: geo.error.value || 'Sem localização.', color: 'warning' })
    return
  }
  form.latitude = c.latitude
  form.longitude = c.longitude
}

async function onBannerSelected(file: File | null) {
  if (!file) return
  try {
    const up = await mediaApi.uploadImage(file, 'store-banner')
    form.bannerUrl = up.url
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload.', color: 'negative' })
  } finally {
    bannerFile.value = null
  }
}

async function onSubmit() {
  saving.value = true
  error.value = null
  try {
    if (props.editingId) {
      const patch: UpdateStoreRequest = { name: form.name }
      if (form.phone) patch.phone = form.phone
      if (form.whatsapp) patch.whatsapp = form.whatsapp
      if (form.email) patch.email = form.email
      if (form.adminPhone) patch.adminPhone = form.adminPhone
      if (form.cnpj) patch.cnpj = form.cnpj
      if (form.legalName) patch.legalName = form.legalName
      if (form.bannerUrl) patch.bannerUrl = form.bannerUrl
      patch.isBranch = form.isBranch ?? false
      patch.isActive = form.isActive === undefined ? true : form.isActive
      if (form.addressZipCode) patch.addressZipCode = form.addressZipCode
      if (form.addressStreet) patch.addressStreet = form.addressStreet
      if (form.addressNumber) patch.addressNumber = form.addressNumber
      if (form.addressComplement) patch.addressComplement = form.addressComplement
      if (form.addressNeighborhood) patch.addressNeighborhood = form.addressNeighborhood
      if (form.addressCity) patch.addressCity = form.addressCity
      if (form.addressState) patch.addressState = form.addressState
      if (form.latitude != null) patch.latitude = form.latitude
      if (form.longitude != null) patch.longitude = form.longitude
      await tenantApi.updateStore(props.editingId, patch)
      $q.notify({ message: 'Loja atualizada.', color: 'positive' })
    } else {
      const create: CreateStoreRequest = { name: form.name }
      if (form.slug) create.slug = form.slug
      if (form.phone) create.phone = form.phone
      if (form.whatsapp) create.whatsapp = form.whatsapp
      if (form.email) create.email = form.email
      if (form.adminPhone) create.adminPhone = form.adminPhone
      if (form.cnpj) create.cnpj = form.cnpj
      if (form.legalName) create.legalName = form.legalName
      if (form.bannerUrl) create.bannerUrl = form.bannerUrl
      create.isBranch = form.isBranch ?? false
      create.isMain = form.isMain ?? false
      if (form.addressZipCode) create.addressZipCode = form.addressZipCode
      if (form.addressStreet) create.addressStreet = form.addressStreet
      if (form.addressNumber) create.addressNumber = form.addressNumber
      if (form.addressComplement) create.addressComplement = form.addressComplement
      if (form.addressNeighborhood) create.addressNeighborhood = form.addressNeighborhood
      if (form.addressCity) create.addressCity = form.addressCity
      if (form.addressState) create.addressState = form.addressState
      if (form.latitude != null) create.latitude = form.latitude
      if (form.longitude != null) create.longitude = form.longitude
      await tenantApi.createStore(create)
      $q.notify({ message: 'Loja criada.', color: 'positive' })
    }
    openProxy.value = false
    emit('saved')
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Falha ao salvar.'
  } finally {
    saving.value = false
  }
}
</script>
