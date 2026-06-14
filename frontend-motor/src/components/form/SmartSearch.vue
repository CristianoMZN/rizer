<template>
  <div class="smart-search" :class="{ compact }">
    <q-input
      ref="searchInputRef"
      v-model="query"
      :placeholder="placeholder"
      outlined
      :dense="compact"
      bg-color="white"
      class="search-input"
      @keyup.enter="handleSearch"
      @update:model-value="onInput"
      @focus="onFieldInteract"
      @click="onFieldInteract"
    >
      <template #prepend>
        <q-icon name="search" color="grey-6" />
      </template>
      <template #append>
        <q-btn
          v-if="query"
          flat
          round
          icon="close"
          size="sm"
          @click="query = ''"
          aria-label="Limpar busca"
        />
        <q-btn
          flat
          round
          icon="tune"
          color="primary"
          size="sm"
          aria-label="Filtros"
          @click.stop="showFilters = !showFilters"
        />
        <q-btn
          v-if="!compact"
          unelevated
          color="primary"
          label="Buscar"
          class="q-ml-sm"
          @click="handleSearch"
        />
      </template>
    </q-input>

    <q-menu
      v-model="showFilters"
      :target="searchInputRef?.$el"
      anchor="bottom middle"
      self="top middle"
      fit
      class="filters-menu"
    >
      <div class="q-pa-md column q-gutter-sm" style="min-width: 280px">
        <div class="text-subtitle2 text-weight-bold">Filtrar junto da busca</div>

        <q-select
          v-model="quickFilters.type"
          :options="typeOptions"
          outlined
          dense
          clearable
          label="Tipo"
        />

        <q-select
          v-model="quickFilters.brand"
          :options="brandOptions"
          outlined
          dense
          use-input
          fill-input
          clearable
          label="Marca"
        />

        <q-select
          v-model="quickFilters.fuel"
          :options="fuelOptions"
          outlined
          dense
          clearable
          label="Combustível"
        />

        <q-slider
          v-model="quickFilters.priceMax"
          :min="10000"
          :max="maxPrice"
          :step="5000"
          color="primary"
          label
          label-always
          class="q-mt-sm"
        />
        <div class="text-caption text-grey-7">
          Preço máximo: {{ quickFilters.priceMax ? `R$ ${quickFilters.priceMax.toLocaleString('pt-BR')}` : 'Qualquer' }}
        </div>

        <div class="row justify-end q-gutter-sm q-mt-sm">
          <q-btn flat label="Limpar" color="grey-7" @click="clearFilters" />
          <q-btn unelevated label="Aplicar" color="primary" @click="handleSearch" />
        </div>
      </div>
    </q-menu>

    <!-- Suggestions dropdown -->
    <q-menu
      v-if="suggestions.length && query.length >= 2"
      v-model="showSuggestions"
      no-parent-event
      no-focus
      fit
    >
      <q-list>
        <q-item
          v-for="s in suggestions"
          :key="s"
          clickable
          v-close-popup
          @click="selectSuggestion(s)"
        >
          <q-item-section avatar>
            <q-icon name="search" color="grey-5" />
          </q-item-section>
          <q-item-section>{{ s }}</q-item-section>
        </q-item>
      </q-list>
    </q-menu>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { QInput } from 'quasar'
import type { VehicleFilters } from 'src/data/types'

interface Props {
  compact?: boolean
  placeholder?: string
  modelValue?: string
}

type QuickFilters = Pick<VehicleFilters, 'type' | 'brand' | 'fuel' | 'priceMax'>

const props = withDefaults(defineProps<Props>(), {
  placeholder: 'Buscar veículos por marca, modelo, ano...',
  compact: false,
})

const emit = defineEmits<{
  search: [query: string]
  'search-advanced': [payload: { query: string; filters: QuickFilters }]
  'update:modelValue': [value: string]
}>()

const query = ref(props.modelValue ?? '')
const searchInputRef = ref<QInput | null>(null)
const showFilters = ref(false)
const showSuggestions = ref(false)
const suggestions = ref<string[]>([])
const quickFilters = ref<QuickFilters>({})

const typeOptions = ['Carro', 'Moto', 'Caminhão', 'Van/Furgão', 'Ônibus']
const brandOptions: string[] = []
const fuelOptions = ['Flex', 'Gasolina', 'Álcool', 'Diesel', 'GNV', 'Elétrico', 'Híbrido']
const maxPrice = 500000

watch(query, (val) => emit('update:modelValue', val))

function onInput(val: string | number | null) {
  const str = String(val ?? '')
  if (str.length < 2) {
    suggestions.value = []
    showSuggestions.value = false
    return
  }
  suggestions.value = []
  showSuggestions.value = false
}

function handleSearch() {
  showSuggestions.value = false
  showFilters.value = false
  emit('search', query.value)
  emit('search-advanced', { query: query.value, filters: { ...quickFilters.value } })
}

function onFieldInteract() {
  showFilters.value = true
}

function clearFilters() {
  quickFilters.value = {}
}

function selectSuggestion(s: string) {
  query.value = s
  handleSearch()
}
</script>

<style scoped lang="scss">
.smart-search {
  width: 100%;

  .search-input {
    border-radius: 12px;
    :deep(.q-field__control) {
      border-radius: 12px;
    }
  }

  &.compact .search-input {
    :deep(.q-field__control) {
      min-height: 40px;
    }
  }
}
</style>
