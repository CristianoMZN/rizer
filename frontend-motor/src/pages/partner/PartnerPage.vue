<template>
  <q-page>
    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <template v-else-if="tenant">
      <!-- Hero / Banner -->
      <div
        class="partner-hero"
        :style="bannerStyle"
      >
        <div class="partner-hero-overlay">
          <div class="container">
            <q-btn
              flat
              dense
              color="white"
              icon="arrow_back"
              label="Voltar para parceiros"
              :to="{ name: 'parceiros' }"
              class="q-mb-md"
            />
            <div class="row items-end q-col-gutter-md">
              <q-avatar
                v-if="tenant.logoUrl"
                :src="tenant.logoUrl"
                size="96px"
                square
                class="shadow-4"
              />
              <q-avatar v-else color="white" text-color="primary" size="96px" square class="shadow-4">
                <span class="text-h4">{{ initials(tenant.tradeName) }}</span>
              </q-avatar>
              <div class="col">
                <h1 class="text-h4 text-white q-my-none">{{ tenant.tradeName }}</h1>
                <div class="text-subtitle1 text-white" style="opacity: 0.9;">
                  {{ tenant.activeProductsCount }} anúncio(s) ativo(s) · {{ tenant.stores.length }} loja(s)
                </div>
              </div>
              <div class="col-auto">
                <q-btn
                  v-if="tenant.whatsapp"
                  unelevated
                  color="positive"
                  icon="phone"
                  :label="`WhatsApp ${tenant.phone ?? ''}`"
                  :href="whatsappLink"
                  target="_blank"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="container q-py-lg">
        <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

        <!-- Sobre -->
        <section v-if="tenant.description" class="q-mb-xl">
          <h2 class="text-h6 q-mb-sm">Sobre a empresa</h2>
          <p class="text-body1 text-grey-8" style="white-space: pre-wrap;">{{ tenant.description }}</p>
        </section>

        <!-- Galeria -->
        <section v-if="tenant.gallery && tenant.gallery.length > 0" class="q-mb-xl">
          <h2 class="text-h6 q-mb-md">Galeria</h2>
          <div class="row q-gutter-sm">
            <div v-for="img in tenant.gallery" :key="img.id" class="position-relative">
              <q-img :src="img.url" :ratio="4/3" style="width: 200px" />
              <q-badge v-if="img.isCover" color="primary" class="absolute" style="top: 4px; left: 4px;">Capa</q-badge>
            </div>
          </div>
        </section>

        <!-- Lojas físicas -->
        <section class="q-mb-xl">
          <h2 class="text-h6 q-mb-md">Lojas físicas</h2>
          <div class="row q-col-gutter-md">
            <div
              v-for="store in tenant.stores"
              :key="store.id"
              class="col-12 col-sm-6 col-md-4"
            >
                  <q-card flat bordered class="full-height">
                <q-img
                  v-if="store.bannerUrl"
                  :src="store.bannerUrl"
                  :ratio="16/9"
                />
                <q-card-section>
                  <div class="row items-center">
                    <q-icon
                      :name="store.isMain ? 'star' : 'store'"
                      :color="store.isMain ? 'primary' : 'grey-7'"
                      size="20px"
                    />
                    <span class="text-subtitle1 q-ml-sm">{{ store.name }}</span>
                    <q-chip
                      v-if="store.isMain"
                      size="sm"
                      color="primary"
                      text-color="white"
                      label="Principal"
                      class="q-ml-sm"
                    />
                  </div>
                  <div v-if="store.city" class="text-caption text-grey-7 q-mt-sm">
                    <q-icon name="place" size="16px" />
                    {{ store.city }} - {{ store.state }}
                  </div>
                  <div v-if="store.phone" class="text-caption text-grey-7 q-mt-xs">
                    <q-icon name="phone" size="16px" /> {{ store.phone }}
                  </div>
                  <div v-if="store.whatsapp" class="text-caption q-mt-xs">
                    <q-btn
                      flat
                      dense
                      color="positive"
                      icon="chat"
                      label="Conversar"
                      :href="`https://wa.me/55${store.whatsapp.replace(/\D/g, '')}`"
                      target="_blank"
                      size="sm"
                    />
                  </div>
                </q-card-section>
                <q-card-actions>
                  <q-btn
                    flat
                    color="primary"
                    label="Ver veículos desta loja"
                    :to="{ name: 'parceiro-loja', params: { slug: tenant.slug, storeSlug: store.slug } }"
                  />
                </q-card-actions>
                <div v-if="store.gallery && store.gallery.length > 0" class="q-pa-md row q-gutter-xs">
                  <q-img
                    v-for="g in store.gallery.slice(0, 4)"
                    :key="g.id"
                    :src="g.url"
                    :ratio="1"
                    style="width: 56px"
                  />
                </div>
              </q-card>
            </div>
          </div>
        </section>

        <!-- Filtros de produtos -->
        <section>
          <div class="row items-center q-mb-md">
            <h2 class="text-h6 q-my-none col">Veículos disponíveis</h2>
            <q-input
              v-model="search"
              outlined
              dense
              placeholder="Filtrar por título…"
              class="col-12 col-sm-4"
              clearable
            >
              <template #prepend>
                <q-icon name="search" />
              </template>
            </q-input>
          </div>

          <div v-if="filteredProducts.length === 0" class="text-center q-pa-xl text-grey-6">
            <q-icon name="directions_car" size="64px" />
            <div class="text-h6 q-mt-md">Sem veículos no momento</div>
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
                  <div class="row items-center justify-between q-mt-sm">
                    <div class="text-h6 text-primary">{{ formatPrice(p.price, p.currency) }}</div>
                    <q-chip
                      v-if="p.yearModel"
                      size="sm"
                      color="grey-3"
                      text-color="grey-8"
                      :label="`${p.yearModel}`"
                    />
                  </div>
                  <div class="row q-gutter-xs q-mt-xs">
                    <q-chip v-if="p.mileageKm" size="sm" color="grey-3" text-color="grey-8" :label="`${p.mileageKm.toLocaleString('pt-BR')} km`" />
                    <q-chip v-if="p.fuel" size="sm" color="grey-3" text-color="grey-8" :label="p.fuel" />
                    <q-chip v-if="p.transmission" size="sm" color="grey-3" text-color="grey-8" :label="p.transmission" />
                  </div>
                  <div v-if="p.physicalStoreCity" class="text-caption text-grey-6 q-mt-sm">
                    <q-icon name="place" size="14px" />
                    {{ p.physicalStoreName }} — {{ p.physicalStoreCity }}
                  </div>
                </q-card-section>
                <q-card-actions>
                  <q-btn
                    flat
                    color="primary"
                    label="Ver detalhes"
                    :to="{ name: 'produto', params: { id: p.id } }"
                    no-caps
                  />
                  <q-space />
                  <q-btn
                    v-if="p.physicalStoreId"
                    flat
                    color="primary"
                    icon="chat"
                    :href="`https://wa.me/55${(tenant.phone ?? tenant.whatsapp ?? '').replace(/\D/g, '')}`"
                    target="_blank"
                    no-caps
                  >
                    <q-toippet>WhatsApp</q-toippet>
                  </q-btn>
                </q-card-actions>
              </q-card>
            </div>
          </div>
        </section>
      </div>
    </template>

    <q-banner v-else-if="error" class="bg-negative text-white q-ma-md">{{ error }}</q-banner>
    <div v-else class="text-center q-pa-xl text-grey-6">Parceiro não encontrado.</div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { partnerApi, type PublicTenantView, type PublicProductView } from 'src/services/api'
import { useTenant } from 'src/composables/useTenant'

const route = useRoute()
const tenantComposable = useTenant()

const slug = computed(() => route.params.slug as string)
const tenant = ref<PublicTenantView | null>(null)
const products = ref<PublicProductView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')

const filteredProducts = computed(() => {
  const q = search.value?.toLowerCase().trim() ?? ''
  if (!q) return products.value
  return products.value.filter((p) => (p.title ?? '').toLowerCase().includes(q))
})

const bannerStyle = computed(() => {
  if (tenant.value?.bannerUrl) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(0,0,0,0.45), rgba(0,0,0,0.7)), url(${tenant.value.bannerUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  return {
    backgroundImage: `linear-gradient(135deg, ${primaryColor()}, ${accentColor()})`,
  }
})

const whatsappLink = computed(() => {
  if (!tenant.value?.whatsapp) return '#'
  return `https://wa.me/55${tenant.value.whatsapp.replace(/\D/g, '')}`
})

function primaryColor(): string {
  return tenant.value?.theme?.primary ?? '#667eea'
}
function accentColor(): string {
  return tenant.value?.theme?.accent ?? '#764ba2'
}

function initials(name: string): string {
  return name.split(/\s+/).map((w) => w[0] ?? '').join('').slice(0, 2).toUpperCase()
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

function applyTheme(t: PublicTenantView) {
  if (typeof document === 'undefined') return
  const root = document.documentElement
  if (t.theme.primary) root.style.setProperty('--q-primary', t.theme.primary)
  if (t.theme.secondary) root.style.setProperty('--q-secondary', t.theme.secondary)
  if (t.theme.accent) root.style.setProperty('--q-accent', t.theme.accent)
  if (t.theme.dark) root.style.setProperty('--q-dark', t.theme.dark)
  if (t.theme.darkPage) root.style.setProperty('--q-dark-page', t.theme.darkPage)
  root.style.setProperty(
    '--gradient-primary',
    `linear-gradient(135deg, ${t.theme.primary ?? '#667eea'} 0%, ${t.theme.accent ?? '#764ba2'} 100%)`
  )
  // Garante que o composable useTenant enxergue o slug
  tenantComposable.tenant.value = {
    ...tenantComposable.tenant.value,
    mode: 'store',
    storeSlug: t.slug,
    storeName: t.tradeName,
  }
}

function resetTheme() {
  if (typeof document === 'undefined') return
  // Reaplica tema default do composable
  tenantComposable.initFromHostname('')
}

async function load() {
  loading.value = true
  error.value = null
  try {
    const [t, p] = await Promise.all([
      partnerApi.getPartner(slug.value, 'BR'),
      partnerApi.listProducts(slug.value, 'BR'),
    ])
    tenant.value = t
    products.value = p
    applyTheme(t)
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 404
      ? 'Parceiro não encontrado ou não está com página pública ativa.'
      : 'Não foi possível carregar o parceiro.'
  } finally {
    loading.value = false
  }
}

watch(slug, () => { void load() })
onMounted(load)
onBeforeUnmount(resetTheme)
</script>

<style scoped lang="scss">
.position-relative { position: relative; }
.absolute { position: absolute; }

.partner-hero {
  min-height: 280px;
  color: white;
  display: flex;
  align-items: flex-end;
}
.partner-hero-overlay {
  width: 100%;
  background: linear-gradient(0deg, rgba(0,0,0,0.3), transparent);
  padding: 32px 0 16px;
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
