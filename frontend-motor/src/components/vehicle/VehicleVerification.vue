<template>
  <div class="vehicle-verification">
    <p class="section-title">Verificação do Veículo</p>
    <div class="verification-grid">
      <div
        v-for="item in items"
        :key="item.key"
        class="verification-item"
        :class="item.ok ? 'is-ok' : 'is-alert'"
      >
        <q-icon :name="item.ok ? 'check_circle' : 'warning'" :color="item.ok ? 'positive' : 'warning'" size="20px" />
        <span class="item-label">{{ item.label }}</span>
        <q-tooltip>{{ item.ok ? item.okText : item.alertText }}</q-tooltip>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { VehicleVerification } from 'src/data/types'

interface Props {
  verification: VehicleVerification
}

const props = defineProps<Props>()

const items = computed(() => [
  {
    key: 'debts',
    label: 'Débitos',
    ok: !props.verification.hasDebts,
    okText: 'Sem débitos pendentes',
    alertText: 'Possui débitos',
  },
  {
    key: 'auction',
    label: 'Leilão',
    ok: !props.verification.wasAuction,
    okText: 'Não foi de leilão',
    alertText: 'Passou por leilão',
  },
  {
    key: 'rental',
    label: 'Locadora',
    ok: !props.verification.wasRental,
    okText: 'Não foi de locadora',
    alertText: 'Era de locadora',
  },
  {
    key: 'stolen',
    label: 'Roubo',
    ok: !props.verification.wasStolen,
    okText: 'Sem ocorrência de roubo',
    alertText: 'Possui ocorrência de roubo',
  },
  {
    key: 'flooded',
    label: 'Inundação',
    ok: !props.verification.wasFlooded,
    okText: 'Sem registro de inundação',
    alertText: 'Passou por inundação',
  },
  {
    key: 'financed',
    label: 'Financiamento',
    ok: !props.verification.isFinanced,
    okText: 'Sem financiamento ativo',
    alertText: 'Possui financiamento ativo',
  },
])
</script>

<style scoped lang="scss">
.section-title {
  font-weight: 700;
  font-size: 16px;
  margin-bottom: 12px;
}

.verification-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 8px;
}

.verification-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: default;

  &.is-ok {
    background: rgba(33, 186, 69, 0.08);
    border: 1px solid rgba(33, 186, 69, 0.2);
  }

  &.is-alert {
    background: rgba(242, 192, 55, 0.1);
    border: 1px solid rgba(242, 192, 55, 0.3);
  }
}

.item-label {
  font-size: 13px;
  font-weight: 500;
}
</style>
