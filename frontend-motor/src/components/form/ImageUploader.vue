<template>
  <div
    class="image-uploader"
    :class="{ 'is-dragging': dragging }"
    @dragover.prevent="dragging = true"
    @dragleave.prevent="dragging = false"
    @drop.prevent="onDrop"
  >
    <!-- Drop zone -->
    <div v-if="!images.length" class="drop-zone flex flex-center column q-gutter-sm" @click="openPicker">
      <q-icon name="cloud_upload" size="48px" color="grey-4" />
      <p class="text-grey-5 q-mb-none">Arraste fotos ou clique para selecionar</p>
      <p class="text-caption text-grey-4">PNG, JPG até 10MB</p>
    </div>

    <!-- Gallery -->
    <div v-else>
      <div class="gallery-grid">
        <div
          v-for="(img, idx) in images"
          :key="img.preview"
          class="gallery-item"
          :class="{ 'is-main': idx === 0 }"
        >
          <img :src="img.preview" :alt="`Foto ${idx + 1}`" class="gallery-img" />
          <div class="gallery-overlay">
            <q-btn round flat icon="close" color="white" size="sm" @click="removeImage(idx)" />
            <q-btn v-if="idx > 0" round flat icon="star" color="white" size="sm" @click="setMain(idx)" title="Definir como principal" />
          </div>
          <q-badge v-if="idx === 0" color="primary" label="Principal" class="main-badge" />
        </div>
        <div class="gallery-add" @click="openPicker">
          <q-icon name="add_photo_alternate" size="32px" color="grey-4" />
        </div>
      </div>
    </div>

    <input ref="fileInput" type="file" accept="image/*" multiple hidden @change="onFileChange" />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface ImageItem { preview: string; file: File }

const emit = defineEmits<{ 'update:modelValue': [files: File[]] }>()

const dragging = ref(false)
const images = ref<ImageItem[]>([])
const fileInput = ref<HTMLInputElement>()

function openPicker() {
  fileInput.value?.click()
}

function onFileChange(e: Event) {
  const files = (e.target as HTMLInputElement).files
  if (files) addFiles(Array.from(files))
}

function onDrop(e: DragEvent) {
  dragging.value = false
  const files = Array.from(e.dataTransfer?.files ?? []).filter((f) => f.type.startsWith('image/'))
  addFiles(files)
}

function addFiles(files: File[]) {
  files.forEach((file) => {
    const preview = URL.createObjectURL(file)
    images.value.push({ preview, file })
  })
  emit('update:modelValue', images.value.map((i) => i.file))
}

function removeImage(idx: number) {
  URL.revokeObjectURL(images.value[idx]!.preview)
  images.value.splice(idx, 1)
  emit('update:modelValue', images.value.map((i) => i.file))
}

function setMain(idx: number) {
  const [item] = images.value.splice(idx, 1)
  if (item) {
    images.value.unshift(item)
    emit('update:modelValue', images.value.map((i) => i.file))
  }
}
</script>

<style scoped lang="scss">
.image-uploader {
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  padding: 16px;
  transition: all 0.2s ease;

  &.is-dragging {
    border-color: var(--q-primary);
    background: rgba(var(--q-primary-rgb, 25, 118, 210), 0.05);
  }
}

.drop-zone {
  min-height: 160px;
  cursor: pointer;
  &:hover { background: #fafafa; border-radius: 8px; }
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}

.gallery-item {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  aspect-ratio: 4/3;

  &.is-main { grid-column: span 2; }

  &:hover .gallery-overlay { opacity: 1; }
}

.gallery-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.main-badge {
  position: absolute;
  top: 6px;
  left: 6px;
}

.gallery-add {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 4/3;
  border: 2px dashed #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  &:hover { border-color: var(--q-primary); }
}
</style>
