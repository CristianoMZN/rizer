<template>
  <q-page padding>
    <h1 class="text-h4 q-mb-sm">Bem-vindo, {{ auth.user.value?.name }}</h1>
    <p class="text-grey-7 q-mb-lg">
      Você tem {{ auth.user.value?.memberships.length ?? 0 }} vínculo(s) com tenants.
    </p>

    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Você ainda não selecionou um tenant. Acesse um convite ou peça ao
      administrador para incluí-lo.
    </q-banner>

    <div v-if="auth.currentTenantId.value" class="q-gutter-md">
      <q-card flat bordered>
        <q-card-section>
          <div class="text-overline">Próximos passos</div>
          <div class="text-h6">Cadastre sua primeira loja</div>
          <div class="text-caption text-grey-6 q-mt-xs">
            Lojas são as filiais da sua empresa onde os veículos ficam expostos.
            No plano PRO você pode ter até 3 lojas ativas.
          </div>
        </q-card-section>
        <q-card-actions>
          <q-btn flat color="primary" :to="{ name: 'app-stores' }" label="Ir para Lojas" />
        </q-card-actions>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { useAuthStore } from 'src/stores/authStore'
const auth = useAuthStore()
</script>
