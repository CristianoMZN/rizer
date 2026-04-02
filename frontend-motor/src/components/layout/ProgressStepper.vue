<template>
  <div class="progress-stepper">
    <div
      v-for="(step, index) in steps"
      :key="step.label"
      class="stepper-item"
      :class="{
        'is-completed': index < currentStep,
        'is-active': index === currentStep,
      }"
      @click="canNavigate(index) && $emit('go-to', index)"
    >
      <div class="step-connector" v-if="index > 0" />
      <div class="step-circle" :aria-label="`Passo ${index + 1}: ${step.label}`">
        <q-icon v-if="index < currentStep" name="check" size="18px" />
        <span v-else>{{ index + 1 }}</span>
      </div>
      <span class="step-label gt-xs">{{ step.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
interface Step {
  label: string
  icon?: string
}

interface Props {
  steps: Step[]
  currentStep?: number
  allowBack?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  currentStep: 0,
  allowBack: true,
})

defineEmits<{ 'go-to': [index: number] }>()

function canNavigate(index: number): boolean {
  return props.allowBack && index < props.currentStep
}
</script>

<style scoped lang="scss">
.progress-stepper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px 0;
  position: relative;
}

.stepper-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  position: relative;
  flex: 1;
  max-width: 160px;

  &.is-completed {
    .step-circle {
      background: var(--q-positive);
      color: white;
      border-color: var(--q-positive);
    }
    cursor: pointer;
  }

  &.is-active {
    .step-circle {
      background: var(--q-primary);
      color: white;
      border-color: var(--q-primary);
      box-shadow: 0 0 0 4px rgba(var(--q-primary-rgb, 25, 118, 210), 0.2);
    }
    .step-label {
      color: var(--q-primary);
      font-weight: 600;
    }
  }
}

.step-connector {
  position: absolute;
  top: 18px;
  left: calc(-50% + 18px);
  right: calc(50% + 18px);
  height: 2px;
  background: #e0e0e0;
  z-index: 0;

  .is-completed & {
    background: var(--q-positive);
  }
}

.step-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #e0e0e0;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
  color: #9e9e9e;
  z-index: 1;
  transition: all 0.3s ease;
}

.step-label {
  font-size: 12px;
  color: #9e9e9e;
  text-align: center;
  transition: color 0.3s ease;
}
</style>
