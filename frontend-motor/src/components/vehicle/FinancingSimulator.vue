<template>
  <div class="financing-simulator glass-card q-pa-lg">
    <div class="row items-center justify-between q-mb-md">
      <p class="sim-title q-mb-none">Financiamentos aprovados</p>
      <q-chip color="positive" text-color="white" icon="check_circle" dense>
        {{ approvedOffers.length }} ofertas
      </q-chip>
    </div>

    <div class="offer-list">
      <q-card
        v-for="offer in approvedOffers"
        :key="`${offer.bank}-${offer.installments}`"
        flat
        bordered
        class="offer-item"
        :class="offer.trendClass"
      >
        <q-card-section class="row items-center q-col-gutter-md">
          <div class="col-12 col-sm-3">
            <p class="offer-bank q-mb-none">{{ offer.bank }}</p>
            <p class="offer-meta q-mb-none">{{ offer.installments }}x · {{ offer.monthlyRate }}% a.m.</p>
          </div>
          <div class="col-12 col-sm-3">
            <p class="offer-label">Entrada</p>
            <p class="offer-value">{{ formatPrice(offer.downPayment) }}</p>
          </div>
          <div class="col-12 col-sm-3">
            <p class="offer-label">Parcela</p>
            <p class="offer-value" :class="offer.valueClass">{{ formatPrice(offer.monthlyPayment) }}/mês</p>
          </div>
          <div class="col-12 col-sm-3 text-sm-right">
            <q-badge :color="offer.badgeColor" :label="offer.badgeLabel" class="q-mb-xs" />
            <p class="offer-meta q-mb-none">Total: {{ formatPrice(offer.totalPaid) }}</p>
          </div>
        </q-card-section>
      </q-card>
    </div>

    <div class="summary-row row q-col-gutter-md q-mt-md">
      <div class="col-12 col-sm-4">
        <div class="summary-card positive">
          <p class="summary-label">Melhor Parcela</p>
          <p class="summary-value">{{ formatPrice(bestOffer?.monthlyPayment ?? 0) }}</p>
        </div>
      </div>
      <div class="col-12 col-sm-4">
        <div class="summary-card neutral">
          <p class="summary-label">Média de Juros</p>
          <p class="summary-value">{{ averageRate }}% a.m.</p>
        </div>
      </div>
      <div class="col-12 col-sm-4">
        <div class="summary-card negative">
          <p class="summary-label">Maior Parcela</p>
          <p class="summary-value">{{ formatPrice(worstOffer?.monthlyPayment ?? 0) }}</p>
        </div>
      </div>
    </div>

    <p class="text-caption text-grey-5 q-mt-sm">
      * Valores informativos. A aprovação final depende de análise de crédito da instituição.
    </p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FinancingOption } from 'src/data/types'

interface Props {
  vehiclePrice: number
  options: FinancingOption[]
}

const props = defineProps<Props>()

const approvedOffers = computed(() => {
  const sorted = [...props.options].sort((a, b) => a.monthlyPayment - b.monthlyPayment)
  return sorted.map((offer, index) => {
    const totalPaid = offer.monthlyPayment * offer.installments + offer.downPayment
    const isBest = index === 0
    const isWorst = index === sorted.length - 1
    return {
      ...offer,
      totalPaid,
      trendClass: isBest ? 'is-positive' : isWorst ? 'is-negative' : 'is-neutral',
      valueClass: isBest ? 'text-positive' : isWorst ? 'text-negative' : 'text-primary',
      badgeColor: isBest ? 'positive' : isWorst ? 'negative' : 'warning',
      badgeLabel: isBest ? 'Melhor custo' : isWorst ? 'Maior custo' : 'Intermediário',
    }
  })
})

const bestOffer = computed(() => approvedOffers.value[0])
const worstOffer = computed(() => approvedOffers.value[approvedOffers.value.length - 1])
const averageRate = computed(() => {
  if (!props.options.length) return 0
  const total = props.options.reduce((sum, o) => sum + o.monthlyRate, 0)
  return (total / props.options.length).toFixed(2)
})

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.financing-simulator {
  border-radius: 16px;
}

.sim-title {
  font-weight: 700;
  font-size: 18px;
}

.offer-list {
  display: grid;
  gap: 10px;
}

.offer-item {
  border-radius: 12px;
  transition: all 0.2s ease;

  &.is-positive {
    border-color: rgba(33, 186, 69, 0.5);
    background: rgba(33, 186, 69, 0.08);
  }

  &.is-neutral {
    border-color: rgba(242, 192, 55, 0.4);
    background: rgba(242, 192, 55, 0.08);
  }

  &.is-negative {
    border-color: rgba(193, 0, 21, 0.35);
    background: rgba(193, 0, 21, 0.08);
  }
}

.offer-bank {
  font-size: 16px;
  font-weight: 800;
}

.offer-label {
  font-size: 12px;
  color: #7a7a7a;
  margin: 0;
}

.offer-value {
  font-size: 18px;
  font-weight: 800;
  margin: 0;
}

.offer-meta {
  font-size: 12px;
  color: #7a7a7a;
}

.summary-card {
  border-radius: 12px;
  padding: 12px;

  &.positive {
    background: rgba(33, 186, 69, 0.1);
    border: 1px solid rgba(33, 186, 69, 0.4);
  }

  &.neutral {
    background: rgba(49, 204, 236, 0.1);
    border: 1px solid rgba(49, 204, 236, 0.4);
  }

  &.negative {
    background: rgba(193, 0, 21, 0.1);
    border: 1px solid rgba(193, 0, 21, 0.35);
  }
}

.summary-label {
  font-size: 12px;
  color: #6a6a6a;
  margin: 0;
}

.summary-value {
  font-size: 18px;
  font-weight: 800;
  margin: 4px 0 0;
}

@media (max-width: 599px) {
  .offer-item .q-card__section {
    padding-top: 10px;
    padding-bottom: 10px;
  }
}
</style>
