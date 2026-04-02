import { defineBoot } from '#q-app/wrappers';
import { useAppStore } from 'src/stores/appStore';

export default defineBoot(() => {
  const appStore = useAppStore();

  // Fallback: marca como pronta após 2s se não for chamado
  setTimeout(() => {
    if (!appStore.isAppReady.value) {
      appStore.markAppReady();
    }
  }, 2000);
});
