<template>
  <q-page class="lojas-parceiras-page container q-pa-md">
    <section class="hero q-pa-xl q-mb-lg">
      <h1 class="page-title">Lojas parceiras</h1>
      <p class="text-grey-7 q-mb-none">
        Rede de concessionárias e lojas credenciadas com histórico verificado.
      </p>
    </section>

    <div class="stores-grid">
      <q-card v-for="store in stores" :key="store.id" flat bordered class="store-card">
        <q-img :src="store.banner || fallbackBanner" height="180px" fit="cover" />

        <q-card-section class="row items-center q-gutter-md">
          <q-avatar size="60px" class="store-logo">
            <img :src="store.logo || fallbackLogo" :alt="store.name" />
          </q-avatar>
          <div class="col">
            <p class="store-name">{{ store.name }}</p>
            <p class="text-caption text-grey-6 q-mb-none">
              {{ store.address.city }}/{{ store.address.state }} · {{ store.totalSales }} vendas
            </p>
            <q-rating :model-value="store.rating" readonly size="16px" color="amber" />
          </div>
          <q-btn flat color="primary" icon="open_in_new" :to="`/lojas/${store.slug}`" />
        </q-card-section>

        <q-separator />

        <q-card-section>
          <p class="text-body2 q-mb-sm">{{ store.description }}</p>
          <div class="row q-gutter-sm">
            <q-chip dense color="primary" text-color="white" :label="`Plano: ${store.plan}`" />
            <q-chip dense outline color="green" icon="verified" label="Verificada" v-if="store.verified" />
          </div>
        </q-card-section>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { MOCK_STORES } from 'src/data/mock'

const stores = computed(() => MOCK_STORES)

const fallbackBanner = 'https://images.unsplash.com/photo-1486006920555-c77dcf18193c?auto=format&fit=crop&w=1400&q=80'
const fallbackLogo = 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=300&q=80'
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
</style>
