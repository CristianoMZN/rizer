<template>
  <div>
    <div class="row items-center q-mb-md">
      <div class="col">
        <h2 class="text-h6 q-my-none">Galeria</h2>
        <p class="text-caption text-grey-7 q-mb-none">
          Fotos da fachada, interior, pátio e equipe. Visíveis na página do parceiro.
        </p>
      </div>
    </div>

    <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

    <q-file
      v-model="file"
      label="Selecionar foto"
      outlined dense
      accept="image/*"
      class="q-mb-md"
      @update:model-value="onUpload"
    >
      <template #prepend><q-icon name="photo_camera" /></template>
    </q-file>

    <div v-if="images.length === 0" class="text-center text-grey-6 q-pa-lg">
      Nenhuma imagem ainda. Envie fotos para começar.
    </div>

    <div v-else class="row q-gutter-sm">
      <div
        v-for="(img, i) in images"
        :key="img.id"
        class="position-relative"
      >
        <q-img :src="img.url" :ratio="4/3" style="width: 180px" />
        <q-badge v-if="img.isCover" color="primary" class="absolute" style="top: 4px; left: 4px;">
          Capa
        </q-badge>
        <div class="absolute" style="bottom: 4px; right: 4px;">
          <q-btn-group flat>
            <q-btn flat dense round icon="arrow_upward" size="sm" color="white" :disable="i === 0" @click="move(i, -1)">
              <q-tooltip>Subir</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="arrow_downward" size="sm" color="white" :disable="i === images.length - 1" @click="move(i, 1)">
              <q-tooltip>Descer</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="star" size="sm" :color="img.isCover ? 'primary' : 'white'" :disable="img.isCover" @click="setCover(img)">
              <q-tooltip>Definir como capa</q-tooltip>
            </q-btn>
            <q-btn flat dense round icon="delete" size="sm" color="negative" @click="confirmDelete(img)">
              <q-tooltip>Excluir</q-tooltip>
            </q-btn>
          </q-btn-group>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { tenantGalleryApi, type GalleryImageView } from 'src/services/api'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const auth = useAuthStore()

const images = ref<GalleryImageView[]>([])
const file = ref<File | null>(null)
const error = ref<string | null>(null)
const uploading = ref(false)
const busy = ref(false)

async function load() {
  try {
    images.value = await tenantGalleryApi.list()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Não foi possível carregar a galeria.'
  }
}

async function onUpload(f: File | null) {
  if (!f) return
  uploading.value = true
  error.value = null
  try {
    await tenantGalleryApi.upload(f)
    $q.notify({ message: 'Imagem enviada.', color: 'positive' })
    await load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload.', color: 'negative' })
  } finally {
    uploading.value = false
    file.value = null
  }
}

async function move(i: number, dir: -1 | 1) {
  if (busy.value) return
  busy.value = true
  try {
    const next = [...images.value]
    const j = i + dir
    if (j < 0 || j >= next.length) return
    const a = next[i] as GalleryImageView
    const b = next[j] as GalleryImageView
    next[i] = b
    next[j] = a
    images.value = next
    await tenantGalleryApi.reorder(next.map((x) => x.id))
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao reordenar.', color: 'negative' })
    void load()
  } finally {
    busy.value = false
  }
}

async function setCover(img: GalleryImageView) {
  if (busy.value) return
  busy.value = true
  try {
    await tenantGalleryApi.setCover(img.id)
    $q.notify({ message: 'Imagem definida como capa.', color: 'positive' })
    void load()
  } finally {
    busy.value = false
  }
}

function confirmDelete(img: GalleryImageView) {
  $q.dialog({
    title: 'Excluir imagem',
    message: 'Tem certeza? Esta ação não pode ser desfeita.',
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await tenantGalleryApi.delete(img.id)
        $q.notify({ message: 'Imagem excluída.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao excluir.', color: 'negative' })
      }
    })()
  })
}

void uploading // unused but referenced in template context
onMounted(() => {
  if (auth.currentTenantId.value) void load()
})
</script>

<style scoped>
.position-relative { position: relative; }
.absolute { position: absolute; }
</style>
