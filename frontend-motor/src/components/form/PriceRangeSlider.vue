<template>
  <div class="price-range-slider glass-card q-pa-md">
    <div class="row items-center justify-between q-mb-sm">
      <span class="price-label">{{ formatPrice(range.min) }}</span>
      <span class="price-separator text-grey-5">até</span>
      <span class="price-label">{{ formatPrice(range.max) }}</span>
    </div>
    <q-range
      v-model="range"
      :min="min"
      :max="max"
      :step="step"
      color="primary"
      track-color="grey-3"
      @update:model-value="emit('update:modelValue', $event)"
    />
    <div class="row justify-between">
      <span class="text-caption text-grey-5">{{ formatPrice(min) }}</span>
      <span class="text-caption text-grey-5">{{ formatPrice(max) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

interface RangeValue { min: number; max: number }

interface Props {
  modelValue?: RangeValue
  min?: number
  max?: number
  step?: number
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  max: 500000,
  step: 5000,
})

const emit = defineEmits<{ 'update:modelValue': [value: RangeValue] }>()

const range = ref<RangeValue>(props.modelValue ?? { min: props.min, max: props.max })

watch(() => props.modelValue, (v) => { if (v) range.value = v })

function formatPrice(val: number) {
  return val.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 })
}
</script>

<style scoped lang="scss">
.price-range-slider {
  border-radius: 12px;
}

.price-label {
  font-weight: 700;
  font-size: 15px;
  color: var(--q-primary);
}

.price-separator {
  font-size: 12px;
}
</style>
