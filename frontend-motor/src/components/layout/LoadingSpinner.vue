<template>
  <div class="loading-spinner flex flex-center column q-gutter-md" :style="containerStyle">
    <div class="spinner-ring" :style="ringStyle" aria-label="Carregando" role="status" />
    <p v-if="label" class="spinner-label text-grey-6">{{ label }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  size?: 'sm' | 'md' | 'lg'
  color?: string
  label?: string
  fullPage?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  color: 'primary',
})

const sizeMap = { sm: 40, md: 64, lg: 96 }

const ringStyle = computed(() => ({
  width: `${sizeMap[props.size]}px`,
  height: `${sizeMap[props.size]}px`,
  borderTopColor: `var(--q-${props.color})`,
}))

const containerStyle = computed(() =>
  props.fullPage ? { minHeight: '60vh' } : {},
)
</script>

<style scoped lang="scss">
.spinner-ring {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.spinner-label {
  font-size: 14px;
  margin: 0;
}

.body--dark .spinner-ring {
  border-color: rgba(255, 255, 255, 0.1);
}

.body--dark .spinner-label {
  color: rgba(241, 245, 255, 0.82);
}
</style>
