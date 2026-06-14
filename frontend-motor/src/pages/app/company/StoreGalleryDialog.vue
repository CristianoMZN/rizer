<template>
  <q-dialog v-model="openProxy">
    <q-card style="min-width: 700px; max-width: 900px">
      <q-card-section class="row items-center">
        <div class="text-h6">Galeria da loja</div>
        <q-space />
        <q-btn icon="close" flat round dense v-close-popup />
      </q-card-section>
      <q-card-section>
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
        <div v-if="loading" class="text-center q-pa-md"><q-spinner /></div>
        <div v-else-if="images.length === 0" class="text-center text-grey-6 q-pa-lg">
          Nenhuma imagem ainda.
        </div>
        <div v-else class="row q-gutter-sm">
          <div v-for="(img, i) in images" :key="img.id" class="position-relative">
            <q-img :src="img.url" :ratio="4/3" style="width: 160px" />
            <q-badge v-if="img.isCover" color="primary" class="absolute" style="top: 4px; left: 4px;">Capa</q-badge>
            <div class="absolute" style="bottom: 4px; right: 4px;">
              <q-btn-group flat>
                <q-btn flat dense round icon="arrow_upward" size="sm" color="white" :disable="i === 0" @click="move(i, -1)" />
                <q-btn flat dense round icon="arrow_downward" size="sm" color="white" :disable="i === images.length - 1" @click="move(i, 1)" />
                <q-btn flat dense round icon="star" size="sm" :color="img.isCover ? 'primary' : 'white'" :disable="img.isCover" @click="setCover(img)" />
                <q-btn flat dense round icon="delete" size="sm" color="negative" @click="confirmDelete(img)" />
              </q-btn-group>
            </div>
          </div>
        </div>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useQuasar } from 'quasar'
import { storeGalleryApi, type GalleryImageView } from 'src/services/api'

interface Props {
  modelValue: boolean
  storeId: string | null
}
const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const $q = useQuasar()
const openProxy = ref(props.modelValue)
watch(() => props.modelValue, (v) => openProxy.value = v)
watch(openProxy, (v) => emit('update:modelValue', v))

const images = ref<GalleryImageView[]>([])
const file = ref<File | null>(null)
const loading = ref(false)
const error = ref<string | null>(null)
const busy = ref(false)

async function load() {
  if (!props.storeId) return
  loading.value = true
  error.value = null
  try {
    images.value = await storeGalleryApi.list(props.storeId)
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Não foi possível carregar a galeria.'
  } finally {
    loading.value = false
  }
}

watch(() => props.storeId, () => {
  if (props.storeId) void load()
})

watch(() => props.modelValue, (v) => {
  if (v) void load()
})

async function onUpload(f: File | null) {
  if (!f || !props.storeId) return
  try {
    await storeGalleryApi.upload(props.storeId, f)
    $q.notify({ message: 'Imagem enviada.', color: 'positive' })
    void load()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha no upload.', color: 'negative' })
  } finally {
    file.value = null
  }
}

async function move(i: number, dir: -1 | 1) {
  if (busy.value || !props.storeId) return
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
    await storeGalleryApi.reorder(props.storeId, next.map((x) => x.id))
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao reordenar.', color: 'negative' })
    void load()
  } finally {
    busy.value = false
  }
}

async function setCover(img: GalleryImageView) {
  if (busy.value || !props.storeId) return
  busy.value = true
  try {
    await storeGalleryApi.setCover(props.storeId, img.id)
    $q.notify({ message: 'Capa definida.', color: 'positive' })
    void load()
  } finally {
    busy.value = false
  }
}

function confirmDelete(img: GalleryImageView) {
  if (!props.storeId) return
  $q.dialog({
    title: 'Excluir imagem',
    message: 'Tem certeza?',
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        await storeGalleryApi.delete(props.storeId!, img.id)
        $q.notify({ message: 'Imagem excluída.', color: 'positive' })
        void load()
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao excluir.', color: 'negative' })
      }
    })()
  })
}
</script>

<style scoped>
.position-relative { position: relative; }
.absolute { position: absolute; }
</style>
