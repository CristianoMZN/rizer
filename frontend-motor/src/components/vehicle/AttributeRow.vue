<template>
  <div class="attr-row">
    <span class="attr-label">{{ label }}</span>
    <span v-if="kind === 'bool'" class="attr-value">
      <q-icon
        :name="displayValue ? 'check_circle' : 'cancel'"
        :color="displayValue ? 'positive' : 'grey-5'"
        size="20px"
      />
      <span class="q-ml-xs">{{ displayValue ? 'Sim' : 'Não' }}</span>
    </span>
    <span v-else-if="displayValue === null || displayValue === undefined || displayValue === ''" class="attr-value attr-empty">
      —
    </span>
    <span v-else class="attr-value">{{ displayValue }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label: string
  value: unknown
  enumLabels?: Record<string, string>
  unit?: string
}>()

const kind = computed<'bool' | 'number' | 'string'>(() => {
  if (typeof props.value === 'boolean') return 'bool'
  if (typeof props.value === 'number') return 'number'
  return 'string'
})

const displayValue = computed<string | number | boolean | null>(() => {
  const v = props.value
  if (v === null || v === undefined || v === '') return null
  if (typeof v === 'boolean') return v
  if (typeof v === 'number') return props.unit ? `${v} ${props.unit}` : v
  if (typeof v === 'string') {
    if (props.enumLabels && Object.prototype.hasOwnProperty.call(props.enumLabels, v)) {
      return props.enumLabels[v] ?? v
    }
    return v
  }
  return JSON.stringify(v)
})
</script>

<style scoped lang="scss">
.attr-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  gap: 12px;
}
.body--dark .attr-row {
  border-bottom-color: rgba(255, 255, 255, 0.05);
}
.attr-row:last-child { border-bottom: none; }
.attr-label {
  font-size: 13px;
  color: var(--q-grey-7);
  flex: 0 1 60%;
}
.body--dark .attr-label { color: rgba(241, 245, 255, 0.7); }
.attr-value {
  font-size: 14px;
  font-weight: 600;
  text-align: right;
  flex: 1 1 40%;
}
.attr-empty { color: #bdbdbd; font-weight: 400; }
.body--dark .attr-empty { color: #555; }
</style>
