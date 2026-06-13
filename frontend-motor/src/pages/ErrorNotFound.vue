<template>
  <q-page class="flex flex-center column error-page">
    <q-icon name="error_outline" size="120px" color="primary" class="q-mb-md" />
    <h1 class="text-h3 q-my-none">404</h1>
    <p class="text-h6 text-grey-7">Página não encontrada</p>
    <p class="text-body2 text-grey-6 q-mb-lg">
      {{ errorMessage || 'O endereço acessado não existe ou foi removido.' }}
    </p>
    <q-btn unelevated color="primary" label="Voltar para o início" to="/" />
  </q-page>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const errorMessage = computed(() => (route.query.message as string) || '')
</script>

<style scoped>
.error-page { min-height: 70vh; }
</style>
