<template>
  <ProductWizardPage v-if="id" :edit-id="id" />
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import ProductWizardPage from 'pages/app/ProductWizardPage.vue'

const route = useRoute()
const id = (route.params.id as string) || null
</script>
