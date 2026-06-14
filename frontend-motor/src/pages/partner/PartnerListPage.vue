<template>
  <q-page padding>
    <div class="partner-list-hero q-mb-lg">
      <h1 class="text-h4 q-my-none">Empresas parceiras</h1>
      <p class="text-body1 text-grey-7 q-mb-none q-mt-sm">
        Conheça as lojas e concessionárias que anunciam na Motorise.
      </p>
    </div>

    <div class="row q-col-gutter-md items-center q-mb-md">
      <q-input
        v-model="search"
        outlined
        dense
        placeholder="Buscar por nome ou cidade…"
        class="col-12 col-sm-6"
        clearable
      >
        <template #prepend>
          <q-icon name="search" />
        </template>
      </q-input>
      <q-select
        v-model="cityFilter"
        :options="cityOptions"
        label="Cidade"
        outlined
        dense
        clearable
        class="col-6 col-sm-3"
        emit-value
        map-options
      />
      <q-space class="col-12 col-sm-3" />
    </div>

    <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

    <div v-else-if="filtered.length === 0" class="text-center q-pa-xl text-grey-6">
      <q-icon name="store_mall_directory" size="64px" />
      <div class="text-h6 q-mt-md">Nenhum parceiro encontrado</div>
      <div class="text-caption">Tente outro filtro ou volte mais tarde.</div>
    </div>

    <div v-else class="row q-col-gutter-md">
      <div v-for="p in filtered" :key="p.id" class="col-12 col-sm-6 col-md-4">
        <q-card class="partner-card cursor-pointer full-height" flat bordered @click="open(p)">
          <q-img
            v-if="p.bannerUrl"
            :src="p.bannerUrl"
            :ratio="16/9"
            no-spinner
          />
          <div v-else class="partner-card-banner-placeholder" />
          <q-card-section>
            <div class="row items-center no-wrap">
              <q-avatar v-if="p.logoUrl" :src="p.logoUrl" size="48px" square />
              <q-avatar v-else color="primary" text-color="white" size="48px" square>
                {{ initials(p.tradeName) }}
              </q-avatar>
              <div class="q-ml-sm col">
                <div class="text-h6 ellipsis">{{ p.tradeName }}</div>
                <div class="text-caption text-grey-6">
                  {{ p.stores.length }} loja(s) · {{ p.activeProductsCount }} anúncio(s)
                </div>
              </div>
            </div>
            <p v-if="p.description" class="text-body2 text-grey-7 q-mt-sm ellipsis-3-lines">
              {{ p.description }}
            </p>
            <div v-if="citiesOf(p).length > 0" class="q-mt-sm">
              <q-chip
                v-for="c in citiesOf(p).slice(0, 3)"
                :key="c"
                size="sm"
                color="grey-3"
                text-color="grey-8"
                :label="c"
              />
              <q-chip
                v-if="citiesOf(p).length > 3"
                size="sm"
                color="grey-3"
                text-color="grey-8"
                :label="`+${citiesOf(p).length - 3}`"
              />
            </div>
          </q-card-section>
        </q-card>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { partnerApi, type PublicPartnerView } from 'src/services/api'

const router = useRouter()

const partners = ref<PublicPartnerView[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const search = ref('')
const cityFilter = ref<string | null>(null)

const cityOptions = computed(() => {
  const set = new Set<string>()
  for (const p of partners.value) {
    for (const s of p.stores) {
      const c = s.city ? `${s.city} - ${s.state ?? ''}`.trim() : null
      if (c) set.add(c)
    }
  }
  return Array.from(set).sort().map((c) => ({ label: c, value: c }))
})

const filtered = computed(() => {
  const q = search.value?.toLowerCase().trim() ?? ''
  return partners.value.filter((p) => {
    if (q && !p.tradeName.toLowerCase().includes(q) && !citiesOf(p).some((c) => c.toLowerCase().includes(q))) {
      return false
    }
    if (cityFilter.value && !citiesOf(p).includes(cityFilter.value)) {
      return false
    }
    return true
  })
})

function citiesOf(p: PublicPartnerView): string[] {
  return p.stores
    .map((s) => (s.city ? `${s.city} - ${s.state ?? ''}`.trim() : ''))
    .filter(Boolean)
}

function initials(name: string): string {
  return name.split(/\s+/).map((w) => w[0] ?? '').join('').slice(0, 2).toUpperCase()
}

function open(p: PublicPartnerView) {
  void router.push(`/parceiros/${p.slug}`)
}

async function load() {
  loading.value = true
  error.value = null
  try {
    partners.value = await partnerApi.listPartners('BR')
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    error.value = status === 404
      ? 'Endpoint ainda não disponível.'
      : 'Não foi possível carregar a lista de parceiros.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.partner-list-hero {
  text-align: center;
  padding: 24px 0;
}
.partner-card {
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.partner-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
}
.partner-card-banner-placeholder {
  height: 120px;
  background: linear-gradient(135deg, var(--q-primary), var(--q-accent, #764ba2));
}
.ellipsis-3-lines {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
