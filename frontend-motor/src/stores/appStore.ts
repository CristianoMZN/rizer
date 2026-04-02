import { ref } from 'vue';

const isAppReady = ref(false);

export function useAppStore() {
  const markAppReady = () => {
    isAppReady.value = true;
  };

  return {
    isAppReady,
    markAppReady,
  };
}

