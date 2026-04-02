<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useAppStore } from 'src/stores/appStore';

const appStore = useAppStore();

onMounted(() => {
  // Remove o loading overlay quando a app estiver pronta
  const overlay = document.getElementById('app-loading-overlay');
  if (overlay) {
    overlay.classList.add('hidden');
    // Remove do DOM após a transição
    setTimeout(() => {
      overlay.remove();
    }, 400);
  }
  appStore.markAppReady();
});
</script>
