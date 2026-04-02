<template>
  <div class="tag-input">
    <p v-if="label" class="tag-input-label">{{ label }}</p>

    <!-- Selected tags -->
    <div class="tags-container q-mb-sm" v-if="selected.length">
      <q-chip
        v-for="tag in selected"
        :key="tag"
        removable
        color="primary"
        text-color="white"
        size="sm"
        @remove="removeTag(tag)"
      >
        {{ tag }}
      </q-chip>
    </div>

    <!-- Category groups -->
    <div v-for="(items, category) in groupedOptions" :key="category" class="q-mb-md">
      <p class="category-label">{{ category }}</p>
      <div class="row q-gutter-xs">
        <q-btn
          v-for="item in items"
          :key="item"
          :outline="!selected.includes(item)"
          :color="selected.includes(item) ? 'primary' : 'grey-5'"
          :label="item"
          size="xs"
          rounded
          dense
          @click="toggleTag(item)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { OPTIONAL_FEATURES } from 'src/data/types'

interface Props {
  modelValue?: string[]
  label?: string
  options?: Record<string, string[]>
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  options: () => OPTIONAL_FEATURES,
})

const emit = defineEmits<{ 'update:modelValue': [tags: string[]] }>()

const selected = ref<string[]>([...props.modelValue])
const groupedOptions = props.options

watch(() => props.modelValue, (v) => { selected.value = [...v] })

function toggleTag(tag: string) {
  const idx = selected.value.indexOf(tag)
  if (idx >= 0) {
    selected.value.splice(idx, 1)
  } else {
    selected.value.push(tag)
  }
  emit('update:modelValue', [...selected.value])
}

function removeTag(tag: string) {
  selected.value = selected.value.filter((t) => t !== tag)
  emit('update:modelValue', [...selected.value])
}
</script>

<style scoped lang="scss">
.tag-input-label {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
}

.category-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  color: #9e9e9e;
  letter-spacing: 0.5px;
  margin: 0 0 4px;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
</style>
