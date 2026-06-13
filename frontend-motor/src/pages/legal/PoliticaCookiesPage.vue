<template>
  <q-page padding>
    <div class="legal-container">
      <h1>Política de Cookies</h1>
      <p class="text-grey-7">Versão {{ version }} · Última atualização: {{ updatedAt }}</p>

      <p>
        Utilizamos cookies para garantir o funcionamento da plataforma e, com o
        seu consentimento, para melhorar a experiência e mensurar o uso.
      </p>

      <h2>Cookies essenciais</h2>
      <p>
        Necessários para autenticação, segurança e operação da plataforma.
        Não podem ser desativados.
      </p>

      <h2>Cookies de analytics</h2>
      <p>
        Utilizados para entender como os usuários usam a plataforma. Ativados
        somente após consentimento.
      </p>

      <h2>Cookies de marketing</h2>
      <p>
        Utilizados para campanhas em Meta e Google. Ativados somente após
        consentimento. Você pode revogar o consentimento a qualquer momento
        pelo botão "Privacidade" no rodapé.
      </p>
    </div>
  </q-page>
</template>

<script setup lang="ts">
defineProps<{ version: string; updatedAt: string }>()
</script>

<style scoped lang="scss">
.legal-container {
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.6;
}
.legal-container h1 { font-size: 1.8rem; font-weight: 800; margin: 0 0 8px; }
.legal-container h2 { font-size: 1.15rem; font-weight: 700; margin: 24px 0 8px; }
</style>
