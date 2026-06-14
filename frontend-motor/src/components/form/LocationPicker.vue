<template>
  <div class="location-picker">
    <div class="row q-gutter-md">
      <q-input
        v-model="cepInput"
        label="CEP"
        outlined
        dense
        mask="#####-###"
        class="col-12 col-sm-4"
        :loading="loading"
        @update:model-value="onCepChange"
      >
        <template #append>
          <q-icon name="location_on" color="primary" />
        </template>
      </q-input>

      <q-input
        v-model="location.street"
        label="Rua"
        outlined
        dense
        class="col-12 col-sm-8"
        readonly
      />

      <q-input
        v-model="location.neighborhood"
        label="Bairro"
        outlined
        dense
        class="col-12 col-sm-4"
        readonly
      />

      <q-input
        v-model="location.city"
        label="Cidade"
        outlined
        dense
        class="col-12 col-sm-4"
      />

      <q-input
        v-model="location.state"
        label="Estado"
        outlined
        dense
        class="col-12 col-sm-2"
        maxlength="2"
      />
    </div>

    <q-banner v-if="error" class="q-mt-sm" dense rounded type="negative">
      CEP não encontrado. Verifique e tente novamente.
    </q-banner>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { utilApi } from 'src/services/api'

interface LocationData {
  zipCode: string
  street: string
  neighborhood: string
  city: string
  state: string
}

interface Props {
  modelValue?: Partial<LocationData>
}

const props = withDefaults(defineProps<Props>(), { modelValue: () => ({}) })
const emit = defineEmits<{ 'update:modelValue': [loc: LocationData] }>()

const cepInput = ref(props.modelValue?.zipCode ?? '')
const loading = ref(false)
const error = ref(false)

const location = reactive<LocationData>({
  zipCode: props.modelValue?.zipCode ?? '',
  street: props.modelValue?.street ?? '',
  neighborhood: props.modelValue?.neighborhood ?? '',
  city: props.modelValue?.city ?? '',
  state: props.modelValue?.state ?? '',
})

watch(location, (val) => emit('update:modelValue', { ...val }))

async function onCepChange(val: string | number | null) {
  const cep = String(val ?? '').replace(/\D/g, '')
  if (cep.length !== 8) return
  loading.value = true
  error.value = false
  try {
    const data = await utilApi.cepLookup(cep)
    if (data) {
      location.zipCode = cep
      location.street = data.street
      location.neighborhood = data.neighborhood
      location.city = data.city
      location.state = data.state
    } else {
      error.value = true
    }
  } finally {
    loading.value = false
  }
}
</script>
