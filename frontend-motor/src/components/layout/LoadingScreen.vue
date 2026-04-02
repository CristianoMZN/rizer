<template>
  <transition
    enter-active-class="animate__animated animate__fadeOut"
    leave-active-class="animate__animated animate__fadeOut"
  >
    <div v-if="!appStore.isAppReady.value" class="loading-screen">
      <div class="loading-content">
        <div class="spinner-wrapper">
          <q-spinner color="primary" size="60px" />
        </div>
        <p class="loading-text">Carregando...</p>
        <div class="loading-dots">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { useAppStore } from 'src/stores/appStore';

const appStore = useAppStore();
</script>

<style scoped lang="scss">
.loading-screen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  backdrop-filter: blur(10px);

  .loading-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24px;

    .spinner-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .loading-text {
      color: white;
      font-size: 18px;
      font-weight: 500;
      margin: 0;
      letter-spacing: 0.5px;
    }

    .loading-dots {
      display: flex;
      gap: 8px;

      span {
        width: 8px;
        height: 8px;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.7);
        animation: bounce 1.4s infinite ease-in-out both;

        &:nth-child(1) {
          animation-delay: -0.32s;
        }

        &:nth-child(2) {
          animation-delay: -0.16s;
        }
      }
    }
  }
}

@keyframes bounce {
  0%,
  80%,
  100% {
    opacity: 0.7;
    transform: scale(0.8);
  }

  40% {
    opacity: 1;
    transform: scale(1.2);
  }
}

// Light mode
:root .loading-screen {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

// Dark mode
.body--dark .loading-screen {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}
</style>
