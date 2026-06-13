<template>
  <q-dialog v-model="visible" persistent seamless position="top">
    <q-card flat class="loading-overlay-card">
      <q-card-section class="row items-center no-wrap q-py-md">
        <q-spinner-dots color="primary" size="2em" />
        <span class="q-ml-md text-body1">{{ message }}</span>
      </q-card-section>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { useLoading } from 'src/composables/useLoading'

const { visible, message } = useLoading()
</script>

<style scoped>
.loading-overlay-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  min-width: 280px;
  max-width: 90vw;
}
</style>
