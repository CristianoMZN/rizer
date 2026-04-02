<template>
  <div class="price-chart q-pa-md glass-card">
    <p class="chart-title">Preço vs FIPE</p>
    <div class="bars-container">
      <!-- Market price bar -->
      <div class="bar-row">
        <span class="bar-label">Anúncio</span>
        <div class="bar-track">
          <div
            class="bar-fill bar-market"
            :style="{ width: `${marketPct}%` }"
          />
        </div>
        <span class="bar-value text-primary">{{ formatPrice(price) }}</span>
      </div>
      <!-- FIPE bar -->
      <div class="bar-row" v-if="fipePrice">
        <span class="bar-label">FIPE</span>
        <div class="bar-track">
          <div
            class="bar-fill bar-fipe"
            :style="{ width: `${fipePct}%` }"
          />
        </div>
        <span class="bar-value">{{ formatPrice(fipePrice) }}</span>
      </div>
    </div>

    <q-banner
      v-if="fipePrice"
      :class="isBelowFipe ? 'bg-positive' : 'bg-warning'"
      class="q-mt-md text-white rounded-borders"
      dense
    >
      <template #avatar>
        <q-icon :name="isBelowFipe ? 'trending_down' : 'trending_up'" />
      </template>
      {{ isBelowFipe
        ? `${Math.abs(diffPct)}% abaixo da tabela FIPE`
        : `${diffPct}% acima da tabela FIPE`
      }}
    </q-banner>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  price: number
  fipePrice?: number
}

const props = defineProps<Props>()

const maxVal = computed(() => Math.max(props.price, props.fipePrice ?? 0) * 1.1)
const marketPct = computed(() => Math.round((props.price / maxVal.value) * 100))
const fipePct = computed(() =>
  props.fipePrice ? Math.round((props.fipePrice / maxVal.value) * 100) : 0,
)
const isBelowFipe = computed(() => !!props.fipePrice && props.price <= props.fipePrice)
const diffPct = computed(() => {
  if (!props.fipePrice) return 0
  return Math.abs(Math.round(((props.price - props.fipePrice) / props.fipePrice) * 100))
})

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.chart-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 16px;
}

.bars-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-label {
  width: 64px;
  font-size: 13px;
  font-weight: 600;
  color: #9e9e9e;
  flex-shrink: 0;
}

.bar-track {
  flex: 1;
  height: 12px;
  background: #f0f0f0;
  border-radius: 6px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 6px;
  transition: width 0.6s ease;
}

.bar-market { background: var(--gradient-primary); }
.bar-fipe { background: #e0e0e0; }

.bar-value {
  width: 100px;
  font-size: 14px;
  font-weight: 700;
  text-align: right;
  flex-shrink: 0;
}
</style>
