<template>
  <q-page class="produto-page">
    <LoadingSpinner v-if="loading" full-page />

    <div v-else-if="!vehicle" class="flex flex-center column q-py-xl">
      <q-icon name="error_outline" size="80px" color="grey-3" />
      <p class="text-h6 text-grey-5">Veículo não encontrado</p>
      <q-btn unelevated color="primary" to="/produtos">Ver todos os veículos</q-btn>
    </div>

    <div v-else class="container q-pa-md">
      <!-- Breadcrumb -->
      <q-breadcrumbs class="q-mb-md" active-color="primary">
        <q-breadcrumbs-el label="Início" to="/" />
        <q-breadcrumbs-el label="Veículos" to="/produtos" />
        <q-breadcrumbs-el :label="vehicle.title || 'Veículo'" />
      </q-breadcrumbs>

      <div class="row q-gutter-lg">
        <!-- Left: Images + details -->
        <div class="col-12 col-md-8">
          <!-- Main carousel -->
          <q-carousel
            v-model="activeSlide"
            animated
            arrows
            thumbnails
            infinite
            height="420px"
            class="main-carousel rounded-borders q-mb-lg"
          >
            <q-carousel-slide
              v-for="(img, idx) in images"
              :key="img.id"
              :name="idx"
              :img-src="img.url"
            />
            <q-carousel-slide
              v-if="!images.length"
              name="empty"
              :img-src="placeholderImg"
            />
          </q-carousel>

          <!-- Title + price on mobile -->
          <div class="lt-md q-mb-md">
            <p class="vehicle-title">{{ vehicle.title || 'Veículo' }}</p>
            <p class="vehicle-price text-primary">{{ formatPrice(vehicle.price) }}</p>
            <p class="text-caption text-grey-5">
              {{ vehicle.yearModel || '—' }} ·
              <span v-if="vehicle.mileageKm">{{ vehicle.mileageKm.toLocaleString('pt-BR') }} km · </span>
              {{ vehicle.fuel || '—' }}
            </p>
          </div>

          <!-- Description -->
          <q-card v-if="vehicle.description" flat bordered class="rounded-borders q-mb-lg">
            <q-card-section>
              <p class="spec-title">Descrição do vendedor</p>
              <p class="text-body2" style="white-space: pre-wrap">{{ vehicle.description }}</p>
            </q-card-section>
          </q-card>

          <!-- Specs: ficha técnica completa -->
          <q-card flat bordered class="rounded-borders q-mb-lg">
            <q-card-section>
              <p class="spec-title">Características</p>
              <AttributeRow
                v-for="row in attributeRows"
                :key="row.key"
                :label="row.label"
                :value="row.value"
                :enum-labels="row.enumLabels ?? {}"
              />
              <p v-if="!attributeRows.length" class="text-grey-6 text-caption q-mt-sm q-mb-none">
                O vendedor ainda não preencheu a ficha técnica.
              </p>
            </q-card-section>
          </q-card>

          <!-- Localização -->
          <q-card flat bordered class="rounded-borders q-mb-lg">
            <q-card-section>
              <p class="spec-title">Localização</p>
              <p class="row items-center q-gutter-xs">
                <q-icon name="location_on" color="primary" />
                <span class="text-body2">
                  {{ vehicle.physicalStoreName || 'Loja parceira' }} ·
                  {{ vehicle.physicalStoreCity || '—' }}/{{ vehicle.physicalStoreState || '—' }}
                </span>
              </p>
            </q-card-section>
          </q-card>
        </div>

        <!-- Right: price + contact -->
        <div class="col-12 col-md-4">
          <q-card flat bordered class="rounded-borders q-mb-md sticky-panel">
            <q-card-section>
              <div class="row items-start justify-between gt-sm">
                <div>
                  <p class="vehicle-title">{{ vehicle.title || 'Veículo' }}</p>
                  <p class="vehicle-price text-primary">{{ formatPrice(vehicle.price) }}</p>
                  <p class="text-caption text-grey-5">
                    {{ vehicle.yearModel || '—' }} ·
                    <span v-if="vehicle.mileageKm">{{ vehicle.mileageKm.toLocaleString('pt-BR') }} km · </span>
                    {{ vehicle.fuel || '—' }}
                  </p>
                </div>
                <q-btn
                  flat
                  round
                  :icon="isFav ? 'favorite' : 'favorite_border'"
                  :color="isFav ? 'red' : 'grey-5'"
                  :loading="togglingFav"
                  @click="toggleFav"
                >
                  <q-tooltip>{{ isFav ? 'Remover dos favoritos' : 'Favoritar' }}</q-tooltip>
                </q-btn>
              </div>

              <q-separator class="q-my-md" />

              <!-- Tenant + Seller CTA -->
              <div v-if="vehicle.tenantTradeName || vehicle.sellerName" class="q-mb-md">
                <div class="row items-center q-gutter-sm">
                  <q-avatar v-if="vehicle.tenantLogoUrl" size="32px" square>
                    <img :src="vehicle.tenantLogoUrl" :alt="vehicle.tenantTradeName">
                  </q-avatar>
                  <q-avatar v-else size="32px" color="primary" text-color="white" square>
                    {{ (vehicle.tenantTradeName || '?').charAt(0).toUpperCase() }}
                  </q-avatar>
                  <div>
                    <div class="text-caption text-weight-medium">
                      <router-link v-if="vehicle.tenantSlug" :to="`/parceiros/${vehicle.tenantSlug}`" class="text-primary">
                        {{ vehicle.tenantTradeName }}
                      </router-link>
                      <span v-else>{{ vehicle.tenantTradeName }}</span>
                      <span v-if="vehicle.physicalStoreName"> | {{ vehicle.physicalStoreName }}</span>
                    </div>
                    <div v-if="vehicle.sellerName" class="row items-center q-gutter-xs text-caption text-grey-6">
                      <q-avatar v-if="vehicle.sellerAvatarUrl" size="20px">
                        <img :src="vehicle.sellerAvatarUrl" :alt="vehicle.sellerName">
                      </q-avatar>
                      <q-avatar v-else size="20px" color="grey-3" text-color="grey-7">
                        {{ vehicle.sellerName.charAt(0).toUpperCase() }}
                      </q-avatar>
                      <span>{{ vehicle.sellerName }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <q-separator class="q-my-md" />

              <!-- Contact form -->
              <p class="text-weight-bold q-mb-sm">Tenho interesse</p>
              <q-input v-model="lead.name" label="Nome" outlined dense class="q-mb-sm" />
              <q-input v-model="lead.phone" label="Telefone" outlined dense mask="(##) #####-####" class="q-mb-sm" />
              <q-input v-model="lead.email" label="E-mail" outlined dense type="email" class="q-mb-sm" />
              <q-input v-model="lead.message" label="Mensagem (opcional)" outlined dense type="textarea" rows="3" class="q-mb-md" />

              <q-btn
                unelevated
                color="primary"
                label="Enviar mensagem"
                full-width
                :loading="sendingLead"
                @click="sendLead"
              />
              <q-btn
                outline
                color="positive"
                icon="fab fa-whatsapp"
                label="WhatsApp"
                full-width
                class="q-mt-sm"
                :href="whatsappLink"
                target="_blank"
              />
            </q-card-section>
          </q-card>
        </div>
      </div>

      <!-- Veículos parecidos -->
      <section v-if="similar.length" class="q-mt-xl">
        <h2 class="section-title q-mb-md">Veículos parecidos</h2>
        <div class="row q-gutter-md">
          <div v-for="v in similar" :key="v.id" class="col-12 col-sm-6 col-md-3">
            <SimilarVehicleCard :product="v" />
          </div>
        </div>
      </section>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import { useFavorites } from 'src/composables/useFavorites'
import { useAuthStore } from 'src/stores/authStore'
import { partnerApi, catalogApi, leadApi, type PublicProductView, type PublicProductImageView } from 'src/services/api'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'
import AttributeRow from 'components/vehicle/AttributeRow.vue'
import SimilarVehicleCard from 'components/vehicle/SimilarVehicleCard.vue'
import {
  ATTRIBUTE_LABELS,
  CONDITION_LABELS,
  DRIVETRAIN_LABELS,
  STEERING_LABELS,
  TRANSMISSION_DETAIL_LABELS,
  ARMOR_LEVEL_LABELS,
  BODY_TYPE_LABELS,
  CYLINDER_LAYOUT_LABELS,
  FUEL_SUPPLY_LABELS,
} from 'src/i18n/vehicle'

const route = useRoute()
const $q = useQuasar()
const auth = useAuthStore()
const favs = useFavorites()

const vehicle = ref<PublicProductView | null>(null)
const similar = ref<PublicProductView[]>([])
const loading = ref(true)
const activeSlide = ref(0)
const sendingLead = ref(false)
const togglingFav = ref(false)
const lead = ref({ name: '', phone: '', email: '', message: '' })

const isFav = computed(() => (vehicle.value ? favs.isFavorite(vehicle.value.id) : false))
const placeholderImg = 'https://placehold.co/800x500/1a1a2e/ffffff?text=Motorise'

const images = computed<PublicProductImageView[]>(() => vehicle.value?.images ?? [])

const attributeRows = computed<{ key: string; label: string; value: unknown; enumLabels?: Record<string, string>; unit?: string }[]>(() => {
  if (!vehicle.value) return []
  const attrs = vehicle.value.attributes ?? {}
  const rows: { key: string; label: string; value: unknown; enumLabels?: Record<string, string>; unit?: string }[] = []

  // Ordena pelas chaves em ATTRIBUTE_LABELS (ordem do brief) e adiciona
  // quaisquer chaves extras ao final em ordem alfabética.
  const orderedKeys = Object.keys(ATTRIBUTE_LABELS).filter((k) => k in attrs)
  const knownSet = new Set(orderedKeys)
  const extraKeys = Object.keys(attrs).filter((k) => !knownSet.has(k)).sort()

  for (const k of [...orderedKeys, ...extraKeys]) {
    const v = attrs[k]
    if (v === null || v === undefined || v === '') continue
    const enumMap = enumLabelsFor(k)
    if (enumMap) {
      rows.push({ key: k, label: ATTRIBUTE_LABELS[k] ?? prettifyKey(k), value: v, enumLabels: enumMap })
    } else {
      rows.push({ key: k, label: ATTRIBUTE_LABELS[k] ?? prettifyKey(k), value: v })
    }
  }
  return rows
})

function enumLabelsFor(k: string): Record<string, string> | null {
  switch (k) {
    case 'condition': return CONDITION_LABELS
    case 'drivetrain': return DRIVETRAIN_LABELS
    case 'steering': return STEERING_LABELS
    case 'transmission_detail': return TRANSMISSION_DETAIL_LABELS
    case 'armored_level': return ARMOR_LEVEL_LABELS
    case 'body_type': return BODY_TYPE_LABELS
    case 'cylinder_layout': return CYLINDER_LAYOUT_LABELS
    case 'fuel_supply': return FUEL_SUPPLY_LABELS
    default: return null
  }
}

function prettifyKey(k: string): string {
  return k.replace(/_/g, ' ').replace(/^./, (c) => c.toUpperCase())
}

onMounted(async () => {
  await load()
})

watch(() => route.params.id, () => load())

async function load() {
  loading.value = true
  const id = route.params.id as string
  try {
    const partners = await partnerApi.listPartners('BR').catch(() => [])
    let found: PublicProductView | null = null
    for (const p of partners) {
      const prods = await partnerApi.listProducts(p.slug, 'BR', 100).catch(() => [])
      const match = prods.find((x) => x.id === id)
      if (match) { found = match; break }
    }
    if (!found) {
      const prods = await catalogApi.searchProducts({ limit: 200 })
      found = prods.find((x) => x.id === id) ?? null
    }
    vehicle.value = found
    if (found?.categoryName && found.categoryId) {
      const more = await catalogApi.searchProducts({ categoryId: found.categoryId, limit: 8 }).catch(() => [])
      similar.value = more.filter((x) => x.id !== found.id).slice(0, 4)
    }
  } catch {
    vehicle.value = null
  }
  loading.value = false
  if (vehicle.value) {
    if (auth.isAuthenticated.value) await favs.loadIds()
  }
}

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}

const whatsappLink = computed(() => {
  if (!vehicle.value) return '#'
  const phone = (vehicle.value.tenantWhatsapp ?? vehicle.value.sellerWhatsapp ?? vehicle.value.physicalStoreName ?? '').replace(/\D/g, '')
  const text = encodeURIComponent(`Olá! Tenho interesse no ${vehicle.value.title || 'veículo'} anunciado no Motorise.`)
  return `https://wa.me/55${phone}?text=${text}`
})

async function toggleFav() {
  if (!vehicle.value) return
  if (!auth.isAuthenticated.value) {
    $q.notify({ message: 'Faça login para favoritar.', color: 'warning' })
    return
  }
  togglingFav.value = true
  try {
    await favs.toggle(vehicle.value.id)
  } catch {
    $q.notify({ message: 'Falha ao atualizar favorito.', color: 'negative' })
  } finally {
    togglingFav.value = false
  }
}

async function sendLead() {
  if (!vehicle.value || !lead.value.name || !lead.value.phone) {
    $q.notify({ message: 'Preencha nome e telefone.', color: 'warning' })
    return
  }
  sendingLead.value = true
  try {
    await leadApi.create({
      productId: vehicle.value.id,
      buyerName: lead.value.name,
      buyerEmail: lead.value.email,
      buyerPhone: lead.value.phone,
      message: lead.value.message,
      storeId: vehicle.value.physicalStoreId ?? '',
    })
    $q.notify({ message: 'Mensagem enviada! A loja entrará em contato.', color: 'positive', position: 'top' })
    lead.value = { name: '', phone: '', email: '', message: '' }
  } catch {
    $q.notify({ message: 'Falha ao enviar mensagem.', color: 'negative' })
  } finally {
    sendingLead.value = false
  }
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }

.main-carousel :deep(.q-carousel__slide) {
  background-size: cover;
  background-position: center;
}

.vehicle-title { font-size: 18px; font-weight: 700; margin: 0 0 4px; }
.vehicle-price { font-size: 28px; font-weight: 900; margin: 0; }
.spec-title { font-size: 16px; font-weight: 700; margin: 0 0 12px; }
.section-title { font-size: 1.5rem; font-weight: 800; margin: 0; }

.sticky-panel {
  position: sticky;
  top: 80px;
}
</style>
