<template>
  <div class="store-profile">
    <!-- Banner -->
    <div class="store-banner" :style="bannerStyle">
      <div class="banner-overlay" />
      <div class="store-info row items-center q-pa-lg q-gutter-md">
        <q-avatar size="80px" class="store-avatar">
          <img v-if="store.logo" :src="store.logo" :alt="store.name" />
          <span v-else class="text-h4 text-weight-bold">{{ store.name.charAt(0) }}</span>
        </q-avatar>
        <div>
          <div class="row items-center q-gutter-xs">
            <p class="store-name text-white">{{ store.name }}</p>
            <q-icon v-if="store.verified" name="verified" color="light-blue-3" size="20px">
              <q-tooltip>Loja verificada</q-tooltip>
            </q-icon>
          </div>
          <div class="row items-center q-gutter-xs">
            <q-rating :model-value="store.rating" readonly size="16px" color="amber" />
            <span class="text-white text-caption">{{ store.rating }} · {{ store.totalSales }} vendas</span>
          </div>
          <p class="text-white-7 text-caption q-mb-none">{{ store.address.city }}, {{ store.address.state }}</p>
        </div>
        <q-space />
        <div class="store-actions">
          <q-btn outline color="white" label="Contato" icon="phone" :href="`tel:${store.phone}`" class="q-mr-sm" />
          <q-btn unelevated color="white" text-color="primary" label="Ver anúncios" :to="`/lojas/${store.slug}/produtos`" />
        </div>
      </div>
    </div>

    <!-- Description -->
    <div v-if="store.description" class="q-pa-md">
      <p class="text-body2">{{ store.description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { Store } from 'src/data/types'

interface Props { store: Store }
const props = defineProps<Props>()

const bannerStyle = computed(() => ({
  background: props.store.banner
    ? `url(${props.store.banner}) center/cover no-repeat`
    : `linear-gradient(135deg, ${props.store.theme.primaryColor} 0%, ${props.store.theme.secondaryColor} 100%)`,
}))
</script>

<style scoped lang="scss">
.store-banner {
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  min-height: 160px;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
}

.store-info {
  position: relative;
  z-index: 1;
  flex-wrap: wrap;
}

.store-avatar {
  border: 3px solid white;
  background: white;
  font-size: 32px;
  color: #667eea;
}

.store-name {
  font-size: 22px;
  font-weight: 800;
  margin: 0;
}

.store-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
