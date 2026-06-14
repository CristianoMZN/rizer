<template>
  <div class="filter-panel">
    <q-expansion-item
      v-model="expanded"
      label="Filtros"
      icon="tune"
      header-class="filter-header text-weight-bold"
      :hide-expand-icon="!$q.screen.lt.md"
      :disable="!$q.screen.lt.md"
    >
      <q-card flat>
        <q-card-section class="q-gutter-md">
          <!-- Tipo -->
          <div>
            <p class="filter-label">Tipo</p>
            <div class="row q-gutter-sm">
              <q-btn
                v-for="type in vehicleTypes"
                :key="type"
                :outline="filters.type !== type"
                :color="filters.type === type ? 'primary' : 'grey-5'"
                :label="type"
                size="sm"
                rounded
                @click="toggleType(type)"
              />
            </div>
          </div>

          <!-- Marca -->
          <q-select
            v-model="filters.brand"
            :options="brandOptions"
            label="Marca"
            outlined
            dense
            clearable
            emit-value
            map-options
            use-input
            @filter="filterBrands"
          />

          <!-- Modelo -->
          <q-select
            v-if="modelOptions.length"
            v-model="filters.model"
            :options="modelOptions"
            label="Modelo"
            outlined
            dense
            clearable
            emit-value
            map-options
            use-input
            @filter="filterModels"
          />

          <!-- Versão -->
          <q-input v-model="filters.version" label="Versão" outlined dense clearable />

          <!-- Condição -->
          <q-select
            v-model="filters.condition"
            :options="conditionOptions"
            label="Condição"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Faixa de preço -->
          <div>
            <p class="filter-label">Preço: {{ formatPrice(filters.priceMin) }} – {{ formatPrice(filters.priceMax) }}</p>
            <q-range
              v-model="priceRange"
              :min="0"
              :max="600000"
              :step="5000"
              color="primary"
              @update:model-value="onPriceChange"
            />
          </div>

          <!-- Ano -->
          <div class="row q-gutter-sm">
            <q-input
              v-model.number="filters.yearMin"
              label="Ano de"
              type="number"
              outlined
              dense
              class="col"
            />
            <q-input
              v-model.number="filters.yearMax"
              label="Ano até"
              type="number"
              outlined
              dense
              class="col"
            />
          </div>

          <!-- Km máximo -->
          <div>
            <p class="filter-label">Km máximo: {{ filters.mileageMax?.toLocaleString('pt-BR') ?? 'Sem limite' }}</p>
            <q-slider
              v-model="filters.mileageMax"
              :min="0"
              :max="300000"
              :step="10000"
              color="primary"
            />
          </div>

          <!-- Combustível -->
          <q-select
            v-model="filters.fuel"
            :options="fuelOptions"
            label="Combustível"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Motorização -->
          <q-select
            v-model="filters.engine"
            :options="engineOptions"
            label="Motorização"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Cilindros -->
          <q-select
            v-model="filters.cylinders"
            :options="cylindersOptions"
            label="Cilindros"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Transmissão -->
          <q-select
            v-model="filters.transmission"
            :options="transmissionOptions"
            label="Transmissão"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Tração -->
          <q-select
            v-model="filters.drivetrain"
            :options="drivetrainOptions"
            label="Tração"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Direção -->
          <q-select
            v-model="filters.steering"
            :options="steeringOptions"
            label="Direção"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Carroceria -->
          <q-select
            v-model="filters.bodyType"
            :options="bodyTypeOptions"
            label="Tipo de carroceria"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Cor -->
          <q-select
            v-model="filters.color"
            :options="colorOptions"
            label="Cor"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- ABS -->
          <q-select
            v-model="filters.abs"
            :options="yesNoOptions"
            label="Freios ABS"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Blindado -->
          <q-select
            v-model="filters.armored"
            :options="yesNoOptions"
            label="Blindado"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Localização (texto livre) -->
          <q-input v-model="filters.city" label="Cidade" outlined dense clearable />
          <q-input v-model="filters.state" label="UF" outlined dense clearable maxlength="2" />

          <div class="row q-gutter-sm justify-end">
            <q-btn flat label="Limpar" color="grey-6" @click="clearFilters" />
            <q-btn unelevated label="Aplicar" color="primary" @click="applyFilters" />
          </div>
        </q-card-section>
      </q-card>
    </q-expansion-item>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import type { VehicleFilters, VehicleType } from 'src/data/types'
import { MOCK_BRANDS } from 'src/data/mock'
import { catalogApi, MOCK_CONFIG } from 'src/services/api'
import {
  COLOR_OPTIONS,
  ENGINE_DISPLACEMENT_OPTIONS,
  BODY_TYPE_LABELS,
  DRIVETRAIN_LABELS,
  STEERING_LABELS,
  TRANSMISSION_DETAIL_LABELS,
  CONDITION_LABELS,
} from 'src/i18n/vehicle'

interface Props {
  modelValue?: VehicleFilters
}

const props = withDefaults(defineProps<Props>(), { modelValue: () => ({}) })
const emit = defineEmits<{ 'update:modelValue': [filters: VehicleFilters] }>()

const $q = useQuasar()

const expanded = ref(!$q.screen.lt.md)
const filters = reactive<VehicleFilters>({ ...props.modelValue })
const priceRange = ref({ min: filters.priceMin ?? 0, max: filters.priceMax ?? 600000 })

watch(
  () => $q.screen.lt.md,
  (isMobile) => {
    if (!isMobile) expanded.value = true
  },
)

const vehicleTypes: VehicleType[] = ['Carro', 'Moto', 'Van/Furgão', 'Caminhão', 'Ônibus']
const brandOptions = ref(MOCK_BRANDS.map((b) => ({ label: b, value: b })))
const modelOptions = ref<{ label: string; value: string }[]>([])

const conditionOptions = Object.entries(CONDITION_LABELS).map(([k, v]) => ({ label: v, value: k }))
const fuelOptions = ['Flex', 'Gasolina', 'Álcool', 'Diesel', 'GNV', 'Elétrico', 'Híbrido', 'Híbrido plug-in'].map((f) => ({ label: f, value: f }))
const transmissionOptions = Object.entries(TRANSMISSION_DETAIL_LABELS).map(([k, v]) => ({ label: v, value: k }))
const drivetrainOptions = Object.entries(DRIVETRAIN_LABELS).map(([k, v]) => ({ label: v, value: k }))
const steeringOptions = Object.entries(STEERING_LABELS).map(([k, v]) => ({ label: v, value: k }))
const bodyTypeOptions = Object.entries(BODY_TYPE_LABELS).map(([k, v]) => ({ label: v, value: k }))
const colorOptions = COLOR_OPTIONS.map((c) => ({ label: c, value: c }))
const engineOptions = ENGINE_DISPLACEMENT_OPTIONS.map((e) => ({ label: e, value: e }))
const cylindersOptions = [1, 2, 3, 4, 5, 6, 8, 10, 12, 16].map((n) => ({ label: String(n), value: n }))
const yesNoOptions = [
  { label: 'Sim', value: true },
  { label: 'Não', value: false },
]

function filterBrands(val: string, update: (cb: () => void) => void) {
  update(() => {
    if (!val) {
      brandOptions.value = MOCK_BRANDS.map((b) => ({ label: b, value: b }))
      return
    }
    const needle = val.toLowerCase()
    brandOptions.value = MOCK_BRANDS
      .filter((b) => b.toLowerCase().includes(needle))
      .map((b) => ({ label: b, value: b }))
  })
}

function filterModels(val: string, update: (cb: () => void) => void) {
  update(() => {
    if (!val) {
      return
    }
    const needle = val.toLowerCase()
    modelOptions.value = modelOptions.value.filter((m) => m.label.toLowerCase().includes(needle))
  })
}

watch(
  () => filters.brand,
  async (newBrand) => {
    delete filters.model
    if (!newBrand) {
      modelOptions.value = []
      return
    }
    if (!MOCK_CONFIG.useBackend) {
      modelOptions.value = []
      return
    }
    try {
      const map = (MOCK_BRANDS as unknown as { __ids__?: Record<string, number> }).__ids__
      const brandId = map?.[newBrand]
      if (!brandId) return
      const models = await catalogApi.listModels(brandId)
      modelOptions.value = models.map((m) => ({ label: m.name, value: m.name }))
    } catch {
      modelOptions.value = []
    }
  },
)

function toggleType(type: VehicleType) {
  if (filters.type === type) delete filters.type
  else filters.type = type
}

function onPriceChange(v: { min: number; max: number }) {
  filters.priceMin = v.min
  filters.priceMax = v.max
}

function formatPrice(val?: number) {
  if (!val) return 'R$ 0'
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}

function clearFilters() {
  Object.keys(filters).forEach((k) => {
    delete filters[k as keyof VehicleFilters]
  })
  priceRange.value = { min: 0, max: 600000 }
  emit('update:modelValue', {})
}

function applyFilters() {
  emit('update:modelValue', { ...filters })
}

onMounted(() => {
  expanded.value = !$q.screen.lt.md
})
</script>

<style scoped lang="scss">
.filter-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--q-dark);
  margin: 0 0 8px;
}
.body--dark .filter-label {
  color: rgba(241, 245, 255, 0.9);
}
</style>
