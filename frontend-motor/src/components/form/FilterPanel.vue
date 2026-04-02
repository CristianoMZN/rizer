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
          <!-- Vehicle type -->
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

          <!-- Brand -->
          <q-select
            v-model="filters.brand"
            :options="brandOptions"
            label="Marca"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Price range -->
          <div>
            <p class="filter-label">Preço: {{ formatPrice(filters.priceMin) }} – {{ formatPrice(filters.priceMax) }}</p>
            <q-range
              v-model="priceRange"
              :min="0"
              :max="500000"
              :step="5000"
              color="primary"
              @update:model-value="onPriceChange"
            />
          </div>

          <!-- Year range -->
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

          <!-- Mileage -->
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

          <!-- Fuel -->
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

          <!-- Transmission -->
          <q-select
            v-model="filters.transmission"
            :options="transmissionOptions"
            label="Câmbio"
            outlined
            dense
            clearable
            emit-value
            map-options
          />

          <!-- Actions -->
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
import { ref, reactive, watch } from 'vue'
import { useQuasar } from 'quasar'
import type { VehicleFilters, VehicleType } from 'src/data/types'
import { MOCK_BRANDS } from 'src/data/mock'

interface Props {
  modelValue?: VehicleFilters
}

const props = withDefaults(defineProps<Props>(), { modelValue: () => ({}) })
const emit = defineEmits<{ 'update:modelValue': [filters: VehicleFilters] }>()

const $q = useQuasar()

const expanded = ref(!$q.screen.lt.md)
const filters = reactive<VehicleFilters>({ ...props.modelValue })
const priceRange = ref({ min: filters.priceMin ?? 0, max: filters.priceMax ?? 500000 })

watch(
  () => $q.screen.lt.md,
  (isMobile) => {
    if (!isMobile) {
      expanded.value = true
    }
  },
)

const vehicleTypes: VehicleType[] = ['Carro', 'Moto', 'Van/Furgão', 'Caminhão', 'Ônibus']
const brandOptions = MOCK_BRANDS.map((b) => ({ label: b, value: b }))
const fuelOptions = ['Flex', 'Gasolina', 'Álcool', 'Diesel', 'Elétrico', 'Híbrido'].map((f) => ({ label: f, value: f }))
const transmissionOptions = ['Manual', 'Automático', 'Automatizado'].map((t) => ({ label: t, value: t }))

function toggleType(type: VehicleType) {
  if (filters.type === type) {
    delete filters.type
  } else {
    filters.type = type
  }
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
  delete filters.type
  delete filters.brand
  delete filters.priceMin
  delete filters.priceMax
  delete filters.mileageMax
  delete filters.fuel
  delete filters.transmission
  priceRange.value = { min: 0, max: 500000 }
  emit('update:modelValue', {})
}

function applyFilters() {
  emit('update:modelValue', { ...filters })
}
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
