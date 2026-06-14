<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant para gerenciar a empresa.
    </q-banner>

    <div v-else>
      <div class="row items-center q-mb-md">
        <div class="col">
          <h1 class="text-h5 q-my-none">Minha Empresa</h1>
          <p class="text-caption text-grey-7 q-mb-none">
            Perfil, lojas e galeria da sua rede.
          </p>
        </div>
      </div>

      <q-tabs
        v-model="tab"
        dense
        class="text-grey-7 q-mb-md"
        active-color="primary"
        indicator-color="primary"
      >
        <q-tab name="profile" label="Perfil" icon="business" />
        <q-tab name="stores" label="Lojas" icon="store" />
        <q-tab name="gallery" label="Galeria" icon="photo_library" />
      </q-tabs>

      <q-tab-panels v-model="tab" animated keep-alive>

        <!-- ─── ABA PERFIL ─── -->
        <q-tab-panel name="profile">
          <q-banner v-if="!role.canManageTenant.value" class="bg-info text-white q-mb-md">
            Apenas o proprietário (OWNER) pode editar dados cadastrais da empresa.
          </q-banner>

          <q-banner class="bg-warning text-black q-mb-md">
            <q-icon name="lock" class="q-mr-sm" />
            Os campos abaixo são apenas leitura. Para alterar CNPJ, razão social, nome fantasia ou slug, solicite ao suporte mediante conferência da documentação.
          </q-banner>

          <q-spinner v-if="loading && !settings" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />
          <q-form v-else-if="settings" @submit.prevent="saveProfile" class="q-gutter-md">
            <q-banner v-if="error" class="bg-negative text-white">{{ error }}</q-banner>

            <q-card flat bordered>
              <q-card-section>
                <div class="text-overline">Identidade (somente leitura)</div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <q-input v-model="form.slug" label="Slug" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                  <q-input v-model="form.tradeName" label="Nome fantasia" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                  <q-input v-model="form.legalName" label="Razão social" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                  <q-input v-model="form.cnpj" label="CNPJ" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                  <q-input v-model="form.partnerOwnerName" label="Sócio proprietário" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                  <q-input v-model="form.partnerOwnerCpf" label="CPF do sócio" outlined dense class="col-12 col-sm-4" disable>
                    <template #append><q-icon name="lock" /></template>
                  </q-input>
                </div>
              </q-card-section>
            </q-card>

            <q-card flat bordered>
              <q-card-section>
                <div class="text-overline">Contato</div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <q-input v-model="form.phone" label="Telefone comercial" outlined dense class="col-6 col-sm-3" mask="(##) #####-####" unmasked-value :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.whatsapp" label="WhatsApp" outlined dense class="col-6 col-sm-3" mask="(##) #####-####" unmasked-value :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.adminPhone" label="Tel. financeiro/admin" outlined dense class="col-6 col-sm-3" mask="(##) #####-####" unmasked-value :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.email" label="E-mail" type="email" outlined dense class="col-6 col-sm-3" :disable="!role.canManageTenant.value" />
                </div>
                <q-input v-model="form.website" label="Site" outlined dense class="q-mt-sm" :disable="!role.canManageTenant.value" />
                <q-input v-model="form.description" label="Descrição da empresa" outlined dense type="textarea" autogrow class="q-mt-sm" :disable="!role.canManageTenant.value" />
              </q-card-section>
            </q-card>

            <q-card flat bordered>
              <q-card-section>
                <div class="text-overline">Endereço</div>
                <div class="row q-col-gutter-md q-mt-sm items-end">
                  <q-input
                    v-model="form.addressZipCode"
                    label="CEP"
                    outlined dense
                    class="col-6 col-sm-3"
                    mask="#####-###"
                    unmasked-value
                    :disable="!role.canManageTenant.value"
                  >
                    <template #append>
                      <q-btn flat dense round icon="search" size="sm" :disable="!role.canManageTenant.value" @click="onCepLookup" />
                    </template>
                  </q-input>
                  <q-btn outline color="primary" icon="my_location" label="Usar minha localização" :loading="geo.loading.value" :disable="!role.canManageTenant.value" @click="onUseGeo" class="col-6 col-sm-3" />
                </div>
                <q-banner v-if="cepError" class="bg-warning text-black q-mt-sm">{{ cepError }}</q-banner>
                <div class="row q-col-gutter-md q-mt-sm">
                  <q-input v-model="form.addressStreet" label="Logradouro" outlined dense class="col-12 col-sm-6" :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.addressNumber" label="Número" outlined dense class="col-4 col-sm-2" :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.addressComplement" label="Complemento" outlined dense class="col-8 col-sm-4" :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.addressNeighborhood" label="Bairro" outlined dense class="col-12 col-sm-4" :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.addressCity" label="Cidade" outlined dense class="col-12 col-sm-6" :disable="!role.canManageTenant.value" />
                  <q-input v-model="form.addressState" label="UF" outlined dense class="col-6 col-sm-2" :disable="!role.canManageTenant.value" />
                </div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <q-input v-model.number="form.addressLatitude" label="Latitude" type="number" outlined dense class="col-6 col-sm-3" step="0.000001" :disable="!role.canManageTenant.value" />
                  <q-input v-model.number="form.addressLongitude" label="Longitude" type="number" outlined dense class="col-6 col-sm-3" step="0.000001" :disable="!role.canManageTenant.value" />
                </div>
              </q-card-section>
            </q-card>

            <q-card flat bordered>
              <q-card-section>
                <div class="text-overline">Mídia</div>
                <div class="row q-col-gutter-md q-mt-sm">
                  <div class="col-12 col-sm-6">
                    <div class="text-caption q-mb-xs">Logo (1:1)</div>
                    <q-file
                      v-model="logoFile"
                      label="Selecionar logo"
                      outlined dense
                      accept="image/*"
                      :disable="!role.canManageTenant.value"
                      @update:model-value="onLogoSelected"
                    >
                      <template #prepend><q-icon name="image" /></template>
                    </q-file>
                    <q-img v-if="form.logoUrl" :src="form.logoUrl" :ratio="1" class="q-mt-sm" style="max-width:160px" />
                  </div>
                  <div class="col-12 col-sm-6">
                    <div class="text-caption q-mb-xs">Banner (16:5)</div>
                    <q-file
                      v-model="bannerFile"
                      label="Selecionar banner"
                      outlined dense
                      accept="image/*"
                      :disable="!role.canManageTenant.value"
                      @update:model-value="onBannerSelected"
                    >
                      <template #prepend><q-icon name="image" /></template>
                    </q-file>
                    <q-img v-if="form.bannerUrl" :src="form.bannerUrl" :ratio="16/9" class="q-mt-sm" style="max-width:320px" />
                  </div>
                </div>
              </q-card-section>
            </q-card>

            <div class="q-mt-md">
              <q-btn
                unelevated
                color="primary"
                label="Salvar perfil"
                :loading="saving"
                :disable="!role.canManageTenant.value"
                type="submit"
              />
            </div>
          </q-form>
        </q-tab-panel>

        <!-- ─── ABA LOJAS ─── -->
        <q-tab-panel name="stores">
          <StoresTab />
        </q-tab-panel>

        <!-- ─── ABA GALERIA ─── -->
        <q-tab-panel name="gallery">
          <GalleryTab />
        </q-tab-panel>
      </q-tab-panels>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { useTenantRole } from 'src/composables/useTenantRole'
import { useCepLookup } from 'src/composables/useCepLookup'
import { useGeolocation } from 'src/composables/useGeolocation'
import {
  settingsApi, mediaApi,
  type TenantSettingsView, type UpdateProfileRequest,
} from 'src/services/api'
import StoresTab from 'pages/app/company/StoresTab.vue'
import GalleryTab from 'pages/app/company/GalleryTab.vue'

const $q = useQuasar()
const auth = useAuthStore()
const role = useTenantRole()
const cep = useCepLookup()
const geo = useGeolocation()

const tab = ref<'profile' | 'stores' | 'gallery'>('profile')
const settings = ref<TenantSettingsView | null>(null)
const loading = ref(false)
const saving = ref(false)
const error = ref<string | null>(null)
const cepError = ref<string | null>(null)

const form = reactive<UpdateProfileRequest>({
  tradeName: '', legalName: '', cnpj: '',
  partnerOwnerName: '', partnerOwnerCpf: '',
  description: '', phone: '', whatsapp: '', adminPhone: '', email: '', website: '',
  logoUrl: '', bannerUrl: '',
  addressZipCode: '', addressStreet: '', addressNumber: '', addressComplement: '',
  addressNeighborhood: '', addressCity: '', addressState: '',
  addressLatitude: null as number | null,
  addressLongitude: null as number | null,
})

const logoFile = ref<File | null>(null)
const bannerFile = ref<File | null>(null)

function hydrate(s: TenantSettingsView) {
  form.tradeName = s.tradeName ?? ''
  form.legalName = s.legalName ?? ''
  form.cnpj = s.cnpj ?? ''
  form.partnerOwnerName = s.partnerOwnerName ?? ''
  form.partnerOwnerCpf = s.partnerOwnerCpf ?? ''
  form.description = s.description ?? ''
  form.phone = s.phone ?? ''
  form.whatsapp = s.whatsapp ?? ''
  form.adminPhone = s.adminPhone ?? ''
  form.email = s.email ?? ''
  form.website = s.website ?? ''
  form.logoUrl = s.logoUrl ?? ''
  form.bannerUrl = s.bannerUrl ?? ''
  form.addressZipCode = s.addressZipCode ?? ''
  form.addressStreet = s.addressStreet ?? ''
  form.addressNumber = s.addressNumber ?? ''
  form.addressComplement = s.addressComplement ?? ''
  form.addressNeighborhood = s.addressNeighborhood ?? ''
  form.addressCity = s.addressCity ?? ''
  form.addressState = s.addressState ?? ''
  form.addressLatitude = s.addressLatitude ?? null
  form.addressLongitude = s.addressLongitude ?? null
}

async function loadProfile() {
  loading.value = true
  error.value = null
  try {
    settings.value = await settingsApi.get()
    hydrate(settings.value)
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Não foi possível carregar o perfil.'
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  error.value = null
  try {
    settings.value = await settingsApi.updateProfile({ ...form })
    $q.notify({ message: 'Perfil salvo.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Falha ao salvar.'
  } finally {
    saving.value = false
  }
}

async function onCepLookup() {
  cepError.value = null
  const v = await cep.lookup(form.addressZipCode ?? '')
  if (!v) {
    cepError.value = 'CEP não encontrado. Preencha o endereço manualmente.'
    return
  }
  if (v.street) form.addressStreet = v.street
  if (v.neighborhood) form.addressNeighborhood = v.neighborhood
  if (v.city) form.addressCity = v.city
  if (v.state) form.addressState = v.state
  if (v.complement) form.addressComplement = v.complement
  if (v.latitude != null) form.addressLatitude = v.latitude
  if (v.longitude != null) form.addressLongitude = v.longitude
  $q.notify({ message: 'CEP encontrado.', color: 'info' })
}

async function onUseGeo() {
  const c = await geo.getCurrent()
  if (!c) {
    $q.notify({ message: geo.error.value || 'Não foi possível obter localização.', color: 'warning' })
    return
  }
  form.addressLatitude = c.latitude
  form.addressLongitude = c.longitude
  $q.notify({ message: 'Localização capturada.', color: 'info' })
}

async function onLogoSelected(file: File | null) {
  if (!file) return
  try {
    const up = await mediaApi.uploadImage(file, 'tenant-logo')
    form.logoUrl = up.url
    $q.notify({ message: 'Logo enviado.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload do logo.', color: 'negative' })
  } finally {
    logoFile.value = null
  }
}

async function onBannerSelected(file: File | null) {
  if (!file) return
  try {
    const up = await mediaApi.uploadImage(file, 'tenant-banner')
    form.bannerUrl = up.url
    $q.notify({ message: 'Banner enviado.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload do banner.', color: 'negative' })
  } finally {
    bannerFile.value = null
  }
}

watch(() => auth.currentTenantId.value, (tid) => {
  if (tid) void loadProfile()
}, { immediate: true })

onMounted(() => {
  if (auth.currentTenantId.value) void loadProfile()
})
</script>
