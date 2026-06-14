<template>
  <q-page class="lojas-parceiras-page container q-pa-md">
    <section class="hero q-pa-xl q-mb-lg">
      <h1 class="page-title">Lojas parceiras</h1>
      <p class="text-grey-7 q-mb-none">
        Rede de concessionárias e lojas credenciadas.
      </p>
    </section>

    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <q-banner v-else-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

    <div v-else class="stores-grid">
      <q-card v-for="p in partners" :key="p.id" flat bordered class="store-card" :to="`/parceiros/${p.slug}`">
        <q-img :src="p.bannerUrl || fallbackBanner" height="180px" fit="cover" />
        <q-card-section class="row items-center q-gutter-md">
          <q-avatar size="60px" class="store-logo">
            <img :src="p.logoUrl || fallbackLogo" :alt="p.tradeName" />
          </q-avatar>
          <div class="col">
            <p class="store-name">{{ p.tradeName }}</p>
            <p class="text-caption text-grey-6 q-mb-none">
              {{ p.stores.map(s => s.city).filter(Boolean).join(', ') || '—' }} · {{ p.activeProductsCount }} anúncios
            </p>
          </div>
          <q-btn flat color="primary" icon="open_in_new" :to="`/parceiros/${p.slug}`" />
        </q-card-section>
        <q-card-section>
          <p v-if="p.description" class="text-body2 q-mb-sm ellipsis-3-lines">{{ p.description }}</p>
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { partnerApi, type PublicPartnerView } from 'src/services/api'

const partners = ref<PublicPartnerView[]>([])
const loading = ref(true)
const error = ref<string | null>(null)

const fallbackBanner = 'https://images.unsplash.com/photo-1486006920555-c77dcf18193c?auto=format&fit=crop&w=1400&q=80'
const fallbackLogo = 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=300&q=80'

onMounted(async () => {
  try {
    partners.value = await partnerApi.listPartners('BR')
  } catch {
    error.value = 'Não foi possível carregar as lojas.'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.container { max-width: 1200px; margin: 0 auto; }
.hero {
  border-radius: 16px;
  background: linear-gradient(135deg, #e9f2ff 0%, #f6f8ff 100%);
}
.page-title {
  margin: 0 0 8px;
  font-size: 2rem;
  font-weight: 900;
}
.stores-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}
.store-card {
  border-radius: 16px;
  overflow: hidden;
}
.store-logo {
  border: 2px solid #fff;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
  background: white;
}
.store-name {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}
.ellipsis-3-lines {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
