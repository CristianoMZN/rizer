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
        <q-breadcrumbs-el :label="vehicle.title" />
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
              v-for="(img, idx) in vehicle.images"
              :key="idx"
              :name="idx"
              :img-src="img"
            />
          </q-carousel>

          <!-- Specs grid -->
          <q-card flat bordered class="rounded-borders q-mb-lg">
            <q-card-section>
              <p class="spec-title">Especificações</p>
              <div class="specs-grid">
                <div v-for="spec in specs" :key="spec.label" class="spec-item">
                  <q-icon :name="spec.icon" color="primary" />
                  <div>
                    <p class="spec-label">{{ spec.label }}</p>
                    <p class="spec-value">{{ spec.value }}</p>
                  </div>
                </div>
              </div>
            </q-card-section>
          </q-card>

          <!-- Optional features -->
          <q-card flat bordered class="rounded-borders q-mb-lg" v-if="vehicle.features.length">
            <q-card-section>
              <p class="spec-title">Opcionais</p>
              <div class="row q-gutter-xs">
                <q-chip
                  v-for="f in vehicle.features"
                  :key="f"
                  icon="check"
                  color="primary"
                  text-color="white"
                  size="sm"
                >{{ f }}</q-chip>
              </div>
            </q-card-section>
          </q-card>

          <!-- Price chart -->
          <PriceChart
            v-if="vehicle.fipePrice"
            :price="vehicle.price"
            :fipe-price="vehicle.fipePrice"
            class="q-mb-lg"
          />

          <!-- Verification -->
          <q-card flat bordered class="rounded-borders q-mb-lg">
            <q-card-section>
              <VehicleVerification :verification="vehicle.verification" />
            </q-card-section>
          </q-card>

          <!-- Financing -->
          <FinancingSimulator
            v-if="vehicle.financing.length"
            :vehicle-price="vehicle.price"
            :options="vehicle.financing"
            class="q-mb-lg"
          />

          <!-- Comparison -->
          <div class="q-mb-lg">
            <q-btn
              outline
              color="primary"
              icon="compare"
              label="Adicionar à Comparação"
              @click="addToCompare"
            />
          </div>
        </div>

        <!-- Right: price + contact -->
        <div class="col-12 col-md-4">
          <q-card flat bordered class="rounded-borders q-mb-md sticky-panel">
            <q-card-section>
              <div class="row items-start justify-between">
                <div>
                  <p class="vehicle-title">{{ vehicle.title }}</p>
                  <p class="vehicle-price text-primary">{{ formatPrice(vehicle.price) }}</p>
                  <p class="text-caption text-grey-5">
                    {{ vehicle.year }} · {{ vehicle.mileage.toLocaleString('pt-BR') }} km · {{ vehicle.fuel }}
                  </p>
                </div>
                <q-btn
                  flat
                  round
                  :icon="wishlisted ? 'favorite' : 'favorite_border'"
                  :color="wishlisted ? 'red' : 'grey-5'"
                  @click="wishlisted = !wishlisted"
                />
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

          <!-- Store card -->
          <StoreProfile :store="vehicle.store" />
        </div>
      </div>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useQuasar } from 'quasar'
import type { Vehicle } from 'src/data/types'
import { api } from 'src/services/api'
import LoadingSpinner from 'components/layout/LoadingSpinner.vue'
import VehicleVerification from 'components/vehicle/VehicleVerification.vue'
import FinancingSimulator from 'components/vehicle/FinancingSimulator.vue'
import PriceChart from 'components/vehicle/PriceChart.vue'
import StoreProfile from 'components/business/StoreProfile.vue'

const route = useRoute()
const $q = useQuasar()

const vehicle = ref<Vehicle | null>(null)
const loading = ref(true)
const wishlisted = ref(false)
const activeSlide = ref(0)
const sendingLead = ref(false)
const lead = ref({ name: '', phone: '', email: '', message: '' })

onMounted(async () => {
  vehicle.value = (await api.getVehicleById(route.params.id as string)) ?? null
  loading.value = false
})

const specs = computed(() => {
  if (!vehicle.value) return []
  return [
    { label: 'Ano', value: vehicle.value.year, icon: 'calendar_today' },
    { label: 'Quilometragem', value: `${vehicle.value.mileage.toLocaleString('pt-BR')} km`, icon: 'speed' },
    { label: 'Combustível', value: vehicle.value.fuel, icon: 'local_gas_station' },
    { label: 'Câmbio', value: vehicle.value.transmission, icon: 'settings' },
    { label: 'Cor', value: vehicle.value.color ?? '—', icon: 'palette' },
    { label: 'Portas', value: vehicle.value.doors ?? '—', icon: 'door_back' },
    { label: 'Motor', value: vehicle.value.engine ?? '—', icon: 'engineering' },
    { label: 'Localização', value: `${vehicle.value.location.city}/${vehicle.value.location.state}`, icon: 'location_on' },
  ]
})

const whatsappLink = computed(() => {
  if (!vehicle.value) return '#'
  const phone = vehicle.value.store.phone.replace(/\D/g, '')
  const text = encodeURIComponent(`Olá! Tenho interesse no ${vehicle.value.title} anunciado no Motorise.`)
  return `https://wa.me/55${phone}?text=${text}`
})

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}

async function sendLead() {
  if (!vehicle.value || !lead.value.name || !lead.value.phone) {
    $q.notify({ message: 'Preencha nome e telefone.', color: 'warning' })
    return
  }
  sendingLead.value = true
  await api.createLead({
    vehicleId: vehicle.value.id,
    buyerName: lead.value.name,
    buyerEmail: lead.value.email,
    buyerPhone: lead.value.phone,
    message: lead.value.message,
    storeId: vehicle.value.store.id,
  })
  sendingLead.value = false
  $q.notify({ message: 'Mensagem enviada! A loja entrará em contato.', color: 'positive', position: 'top' })
  lead.value = { name: '', phone: '', email: '', message: '' }
}

function addToCompare() {
  $q.notify({ message: 'Veículo adicionado à comparação.', color: 'info', position: 'bottom' })
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

.spec-title { font-size: 16px; font-weight: 700; margin-bottom: 12px; }

.specs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.spec-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.spec-label { font-size: 11px; color: #9e9e9e; margin: 0; }
.spec-value { font-size: 14px; font-weight: 600; margin: 2px 0 0; }

.sticky-panel {
  position: sticky;
  top: 80px;
}
</style>
