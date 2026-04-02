<template>
  <div class="comparison-table">
    <div class="table-header row q-gutter-md q-mb-md">
      <div class="col-3" />
      <div
        v-for="vehicle in vehicles"
        :key="vehicle.id"
        class="col text-center"
      >
        <img
          :src="vehicle.images[0]"
          :alt="vehicle.title"
          class="compare-img"
        />
        <p class="compare-title">{{ vehicle.title }}</p>
        <p class="compare-price text-primary">{{ formatPrice(vehicle.price) }}</p>
        <q-btn
          flat
          round
          icon="close"
          size="sm"
          color="grey"
          @click="$emit('remove', vehicle.id)"
          aria-label="Remover da comparação"
        />
      </div>
    </div>

    <q-list separator>
      <q-item v-for="row in rows" :key="row.key">
        <q-item-section class="col-3 row-label">{{ row.label }}</q-item-section>
        <q-item-section
          v-for="vehicle in vehicles"
          :key="vehicle.id"
          class="col text-center"
          :class="{ 'best-value': isBest(row.key, vehicle) }"
        >
          {{ getValue(row.key, vehicle) }}
        </q-item-section>
      </q-item>
    </q-list>
  </div>
</template>

<script setup lang="ts">
import type { Vehicle } from 'src/data/types'

interface Row { key: string; label: string; lowerIsBetter?: boolean }

interface Props {
  vehicles: Vehicle[]
}

const props = defineProps<Props>()
defineEmits<{ remove: [id: string] }>()

const rows: Row[] = [
  { key: 'year', label: 'Ano' },
  { key: 'price', label: 'Preço', lowerIsBetter: true },
  { key: 'mileage', label: 'Quilometragem', lowerIsBetter: true },
  { key: 'fuel', label: 'Combustível' },
  { key: 'transmission', label: 'Câmbio' },
  { key: 'engine', label: 'Motor' },
  { key: 'fipePrice', label: 'Preço FIPE', lowerIsBetter: true },
]

function getValue(key: string, vehicle: Vehicle): string {
  const v = vehicle[key as keyof Vehicle]
  if (v === undefined || v === null) return '—'
  if (key === 'price' || key === 'fipePrice') return formatPrice(Number(v))
  if (key === 'mileage') return `${Number(v).toLocaleString('pt-BR')} km`
  if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') {
    return `${v}`
  }
  return '—'
}

function isBest(key: string, vehicle: Vehicle): boolean {
  if (props.vehicles.length < 2) return false
  const row = rows.find((r) => r.key === key)
  if (!row || typeof vehicle[key as keyof Vehicle] !== 'number') return false
  const values = props.vehicles.map((v) => Number(v[key as keyof Vehicle] ?? 0))
  const best = row.lowerIsBetter ? Math.min(...values) : Math.max(...values)
  return Number(vehicle[key as keyof Vehicle]) === best
}

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.compare-img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 8px;
}

.compare-title {
  font-weight: 600;
  font-size: 13px;
  margin: 0;
}

.compare-price {
  font-weight: 800;
  font-size: 16px;
  margin: 4px 0;
}

.row-label {
  font-weight: 600;
  color: #9e9e9e;
  font-size: 13px;
}

.best-value {
  background: rgba(33, 186, 69, 0.08);
  font-weight: 700;
  color: var(--q-positive);
  border-radius: 4px;
}
</style>
