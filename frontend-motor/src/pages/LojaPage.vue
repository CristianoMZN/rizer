<template>
  <q-page class="loja-page">
    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <template v-else-if="tenant">
      <div class="store-hero" :style="bannerStyle">
        <div class="container q-py-md">
          <div class="row items-end q-col-gutter-md">
            <q-avatar v-if="tenant.logoUrl" :src="tenant.logoUrl" size="80px" square class="shadow-4" />
            <q-avatar v-else color="white" text-color="primary" size="80px" square class="shadow-4">
              <span class="text-h4">{{ tenant.tradeName.charAt(0) }}</span>
            </q-avatar>
            <div class="col">
              <h1 class="text-h4 text-white q-my-none">{{ tenant.tradeName }}</h1>
              <div class="text-subtitle2 text-white" style="opacity: 0.9;">
                {{ tenant.activeProductsCount }} anúncio(s) ativo(s) · {{ tenant.stores.length }} loja(s)
              </div>
            </div>
            <div class="col-auto">
              <q-btn
                v-if="tenant.whatsapp" unelevated color="positive" icon="phone"
                :href="`https://wa.me/55${tenant.whatsapp.replace(/\D/g, '')}`"
                target="_blank" label="WhatsApp"
              />
            </div>
          </div>
        </div>
      </div>

      <div class="container q-py-lg">
        <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

        <section>
          <div class="row items-center justify-between q-mb-md">
            <h2 class="text-h6 q-my-none">Veículos disponíveis</h2>
          </div>

          <div v-if="products.length === 0" class="text-center q-pa-xl text-grey-6">
            <q-icon name="directions_car" size="64px" />
            <div class="text-h6 q-mt-md">Sem veículos no momento</div>
          </div>

          <div v-else class="row q-col-gutter-md">
            <div v-for="p in products" :key="p.id" class="col-12 col-sm-6 col-md-4">
              <PublicProductCard :product="p" />
            </div>
          </div>
        </section>
      </div>
    </template>

    <q-banner v-else-if="error" class="bg-negative text-white q-ma-md">{{ error }}</q-banner>
    <div v-else class="text-center q-pa-xl text-grey-6">Loja não encontrada.</div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { partnerApi, type PublicTenantView, type PublicProductView } from 'src/services/api'
import PublicProductCard from 'components/vehicle/PublicProductCard.vue'

const route = useRoute()
const slug = route.params.slug as string
const tenant = ref<PublicTenantView | null>(null)
const products = ref<PublicProductView[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const bannerStyle = computed(() => {
  if (tenant.value?.bannerUrl) {
    return {
      backgroundImage: `linear-gradient(180deg, rgba(0,0,0,0.4), rgba(0,0,0,0.7)), url(${tenant.value.bannerUrl})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
    }
  }
  const primary = tenant.value?.theme?.primary ?? '#667eea'
  const accent = tenant.value?.theme?.accent ?? '#764ba2'
  return { backgroundImage: `linear-gradient(135deg, ${primary}, ${accent})` }
})

onMounted(async () => {
  loading.value = true
  try {
    const [t, p] = await Promise.all([
      partnerApi.getPartner(slug, 'BR'),
      partnerApi.listProducts(slug, 'BR'),
    ])
    tenant.value = t
    products.value = p
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 404 ? 'Loja não encontrada.' : 'Não foi possível carregar a loja.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.store-hero {
  min-height: 200px;
  color: white;
  display: flex;
  align-items: flex-end;
}
.container { max-width: 1280px; margin: 0 auto; padding: 0 16px; }
</style>
