<template>
  <q-page class="leads-page container q-pa-md">
    <h1 class="page-title">Funil de Leads</h1>
    <LeadDashboard :store-id="storeId" />
  </q-page>
</template>

<script setup lang="ts">
import LeadDashboard from 'components/business/LeadDashboard.vue'

const storeId = 's1'
</script>

<style scoped lang="scss">
.container { max-width: 1280px; margin: 0 auto; }
.page-title { font-size: 2rem; font-weight: 800; margin-bottom: 24px; }
</style>
