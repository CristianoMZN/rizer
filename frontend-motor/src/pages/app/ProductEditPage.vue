<template>
  <q-page padding>
    <h1 class="text-h5 q-mb-md">Editar anúncio</h1>
    <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>
    <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />
    <div v-else-if="product">
      <q-input v-model="product.title" label="Título" outlined dense class="q-mb-sm" />
      <q-input v-model="product.description" label="Descrição" outlined dense type="textarea" autogrow class="q-mb-sm" />
      <q-input
        v-model.number="product.price"
        type="number"
        label="Preço (R$)"
        outlined
        dense
        class="q-mb-sm"
      />
      <q-select
        v-model="product.status"
        :options="['DRAFT', 'ACTIVE', 'INACTIVE', 'ARCHIVED', 'SOLD']"
        label="Status"
        outlined
        dense
        class="q-mb-sm"
      />
      <q-btn unelevated color="primary" label="Salvar" :loading="saving" @click="onSave" />
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useRoute, useRouter } from 'vue-router'
import { tenantProductApi, type ProductView } from 'src/services/api'

const $q = useQuasar()
const route = useRoute()
const router = useRouter()

const id = route.params.id as string
const product = ref<ProductView | null>(null)
const loading = ref(true)
const saving = ref(false)
const error = ref<string | null>(null)

async function load() {
  loading.value = true
  try {
    product.value = await tenantProductApi.get(id)
  } catch {
    error.value = 'Anúncio não encontrado.'
  } finally {
    loading.value = false
  }
}

async function onSave() {
  if (!product.value) return
  saving.value = true
  try {
    const patch: Parameters<typeof tenantProductApi.update>[1] = {
      price: product.value.price,
      status: product.value.status,
    }
    if (product.value.title !== undefined) patch.title = product.value.title
    if (product.value.description !== undefined) patch.description = product.value.description
    await tenantProductApi.update(id, patch)
    $q.notify({ message: 'Salvo.', color: 'positive' })
    void router.push({ name: 'app-products' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao salvar.', color: 'negative' })
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>
