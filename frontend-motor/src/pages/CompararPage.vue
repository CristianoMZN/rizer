<template>
  <q-page class="comparar-page container q-pa-md">
    <h1 class="page-title">Comparar Veículos</h1>

    <!-- Add vehicle -->
    <div v-if="selected.length < 3" class="row q-gutter-md q-mb-lg">
      <q-select
        v-model="pendingId"
        :options="availableOptions"
        label="Adicionar veículo"
        outlined
        dense
        emit-value
        map-options
        style="max-width: 400px"
        class="col-grow"
      />
      <q-btn unelevated color="primary" label="Adicionar" :disabled="!pendingId" @click="addVehicle" />
    </div>

    <div v-if="!selected.length" class="flex flex-center column q-py-xl text-grey-5">
      <q-icon name="compare" size="80px" color="grey-3" />
      <p class="text-h6">Selecione veículos para comparar</p>
    </div>

    <ComparisonTable
      v-else
      :vehicles="selected"
      @remove="removeVehicle"
    />
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { Vehicle } from 'src/data/types'
import { MOCK_VEHICLES } from 'src/data/mock'
import ComparisonTable from 'components/vehicle/ComparisonTable.vue'

const selected = ref<Vehicle[]>([])
const pendingId = ref<string | null>(null)

const availableOptions = computed(() =>
  MOCK_VEHICLES
    .filter((v) => !selected.value.find((s) => s.id === v.id))
    .map((v) => ({ label: v.title, value: v.id })),
)

function addVehicle() {
  if (!pendingId.value) return
  const v = MOCK_VEHICLES.find((x) => x.id === pendingId.value)
  if (v && selected.value.length < 3) selected.value.push(v)
  pendingId.value = null
}

function removeVehicle(id: string) {
  selected.value = selected.value.filter((v) => v.id !== id)
}
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.page-title { font-size: 2rem; font-weight: 800; margin-bottom: 24px; }
</style>
