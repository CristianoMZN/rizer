<template>
  <q-page>
    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <template v-else-if="tenant && store">
      <div class="store-hero" :style="bannerStyle">
        <div class="container q-py-md">
          <q-btn
            flat dense color="white" icon="arrow_back"
            :label="`Voltar para ${tenant.tradeName}`"
            :to="{ name: 'parceiro', params: { slug: tenant.slug } }"
            class="q-mb-md"
          />
          <div class="row items-end">
            <q-icon name="store" size="40px" color="white" class="q-mr-md" />
            <div class="col">
              <h1 class="text-h5 text-white q-my-none">{{ store.name }}</h1>
              <div v-if="store.city" class="text-subtitle2 text-white" style="opacity: 0.9;">
                <q-icon name="place" size="16px" /> {{ store.city }} - {{ store.state }}
              </div>
            </div>
            <div v-if="store.whatsapp || tenant.whatsapp" class="col-auto">
              <q-btn
                unelevated color="positive" icon="phone"
                :href="`https://wa.me/55${(store.whatsapp ?? tenant.whatsapp ?? '').replace(/\D/g, '')}`"
                target="_blank"
                label="Conversar"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="container q-py-lg">
        <h2 class="text-h6 q-mb-md">Veículos desta loja</h2>

        <div v-if="filteredProducts.length === 0" class="text-center q-pa-xl text-grey-6">
          <q-icon name="directions_car" size="64px" />
          <div class="text-h6 q-mt-md">Sem veículos nesta loja</div>
        </div>

        <div v-else class="row q-col-gutter-md">
          <div
            v-for="p in filteredProducts"
            :key="p.id"
            class="col-12 col-sm-6 col-md-4"
          >
            <q-card flat bordered class="full-height product-card">
              <q-img
                v-if="coverOf(p)"
                :src="coverOf(p)"
                :ratio="16/9"
                no-spinner
              />
              <div v-else class="product-card-placeholder">
                <q-icon name="directions_car" size="48px" />
              </div>
              <q-card-section>
                <div class="text-caption text-grey-6">
                  {{ p.brandName || '—' }} {{ p.modelName || '' }}
                </div>
                <div class="text-h6 ellipsis-2-lines q-my-xs">{{ p.title || 'Veículo' }}</div>
                <div class="text-h6 text-primary q-mt-sm">{{ formatPrice(p.price, p.currency) }}</div>
                <div class="row q-gutter-xs q-mt-xs">
                  <q-chip v-if="p.yearModel" size="sm" color="grey-3" text-color="grey-8" :label="`${p.yearModel}`" />
                  <q-chip v-if="p.mileageKm" size="sm" color="grey-3" text-color="grey-8" :label="`${p.mileageKm.toLocaleString('pt-BR')} km`" />
                  <q-chip v-if="p.fuel" size="sm" color="grey-3" text-color="grey-8" :label="p.fuel" />
                </div>
              </q-card-section>
              <q-card-actions>
                <q-btn
                  flat color="primary" label="Ver detalhes"
                  :to="{ name: 'produto', params: { id: p.id } }"
                  no-caps
                />
              </q-card-actions>
            </q-card>
          </div>
        </div>
      </div>
    </template>

    <q-banner v-else-if="error" class="bg-negative text-white q-ma-md">{{ error }}</q-banner>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { partnerApi, type PublicTenantView, type PublicProductView } from 'src/services/api'
import { MOCK_CONFIG } from 'src/services/api'
import { MOCK_STORES, MOCK_VEHICLES } from 'src/data/mock'

const route = useRoute()
const slug = computed(() => route.params.slug as string)
const storeSlug = computed(() => route.params.storeSlug as string)

const tenant = ref<PublicTenantView | null>(null)
const store = computed(() =>
  tenant.value?.stores.find((s) => s.slug === storeSlug.value) ?? null
)
const products = ref<PublicProductView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')

const filteredProducts = computed(() => {
  const q = search.value?.toLowerCase().trim() ?? ''
  let list = products.value
  if (store.value) {
    list = list.filter((p) => p.physicalStoreId === store.value!.id)
  }
  if (q) list = list.filter((p) => (p.title ?? '').toLowerCase().includes(q))
  return list
})

const bannerStyle = computed(() => {
  if (tenant.value?.bannerUrl) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(0,0,0,0.4), rgba(0,0,0,0.7)), url(${tenant.value.bannerUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  return {
    backgroundImage: `linear-gradient(135deg, ${primaryColor()}, ${accentColor()})`,
  }
})

function primaryColor(): string {
  return tenant.value?.theme?.primary ?? '#667eea'
}
function accentColor(): string {
  return tenant.value?.theme?.accent ?? '#764ba2'
}

function formatPrice(value: number, currency: string): string {
  try {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency }).format(value)
  } catch {
    return value.toFixed(2)
  }
}

function coverOf(p: PublicProductView): string | undefined {
  const cover = p.images.find((i) => i.isCover) || p.images[0]
  return cover?.url
}

async function load() {
  loading.value = true
  error.value = null
  if (!MOCK_CONFIG.useBackend) {
    const store = MOCK_STORES.find((s) => s.slug === slug.value)
    if (!store) { error.value = 'Parceiro não encontrado'; loading.value = false; return }
    const mockTenant: PublicTenantView = {
      id: store.id, slug: store.slug, tradeName: store.name,
      ...(store.description ? { description: store.description } : {}),
      ...(store.logo ? { logoUrl: store.logo } : {}),
      ...(store.banner ? { bannerUrl: store.banner } : {}),
      ...(store.phone ? { phone: store.phone, whatsapp: store.phone } : {}),
      ...(store.email ? { email: store.email } : {}),
      ...(store.website ? { website: store.website } : {}),
      theme: { primary: '#667eea', secondary: '#11998e', accent: '#764ba2' },
      stores: [{
        id: store.id, name: store.name, slug: store.slug,
        ...(store.phone ? { phone: store.phone } : {}),
        city: store.address.city, state: store.address.state,
        isMain: true,
        isBranch: false,
        gallery: [],
      }],
      activeProductsCount: MOCK_VEHICLES.filter((v) => v.store.id === store.id).length,
      realms: [],
      gallery: [],
    }
    tenant.value = mockTenant
    products.value = MOCK_VEHICLES
      .filter((v) => v.store.id === store.id)
      .map((v): PublicProductView => ({
        id: v.id,
        ...(v.title ? { title: v.title } : {}),
        price: v.price, currency: 'BRL',
        realm: v.type === 'Moto' ? 'MOTORCYCLE' : 'CAR',
        yearModel: v.year, mileageKm: v.mileage, fuel: v.fuel, transmission: v.transmission,
        brandName: v.brand, modelName: v.model, categoryName: v.subtype ?? v.type,
        physicalStoreId: store.id, physicalStoreName: store.name,
        physicalStoreCity: store.address.city, physicalStoreState: store.address.state,
        attributes: {},
        images: v.images.map((url, i) => ({ id: String(i), url, isCover: i === 0 })),
      }))
    loading.value = false
    return
  }
  try {
    const t = await partnerApi.getPartner(slug.value, 'BR')
    tenant.value = t
    products.value = await partnerApi.listProducts(slug.value, 'BR')
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 404
      ? 'Loja não encontrada.'
      : 'Não foi possível carregar a loja.'
  } finally {
    loading.value = false
  }
}

watch([slug, storeSlug], () => { void load() }, { immediate: false })
onMounted(load)
</script>

<style scoped lang="scss">
.store-hero {
  min-height: 200px;
  color: white;
  display: flex;
  align-items: flex-end;
}
.container { max-width: 1280px; margin: 0 auto; padding: 0 16px; }
.product-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}
.product-card-placeholder {
  height: 180px;
  background: linear-gradient(135deg, #f3f4f6, #e5e7eb);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
}
.ellipsis-2-lines {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
