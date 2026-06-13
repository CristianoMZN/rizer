<template>
  <q-page padding>
    <h1 class="text-h4 q-mb-md">Admin · Dashboard</h1>
    <p class="text-grey-7 q-mb-lg">Olá, {{ auth.user.value?.name }}.</p>
    <q-banner class="bg-info text-white">
      <template #avatar>
        <q-icon name="info" />
      </template>
      Bem-vindo ao painel administrativo. As próximas fases habilitam a
      criação de tenants, gestão de pagamentos e auditoria.
    </q-banner>
  </q-page>
</template>

<script setup lang="ts">
import { useAuthStore } from 'src/stores/authStore'
const auth = useAuthStore()
</script>
