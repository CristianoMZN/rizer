<template>
  <q-layout view="hHh Lpr lFf">
    <AppHeader
      :is-authenticated="isAuthenticated"
      :user-name="userName"
      @toggle-drawer="leftDrawerOpen = !leftDrawerOpen"
    />

    <q-drawer v-model="leftDrawerOpen" bordered overlay behavior="mobile" side="left" class="main-drawer lt-md">
      <q-scroll-area class="fit">
        <q-list>
          <q-item-label header>Navegar</q-item-label>
          <q-item v-if="isMenuVisible('home')" clickable v-ripple to="/" exact>
            <q-item-section avatar><q-icon name="home" /></q-item-section>
            <q-item-section>Início</q-item-section>
          </q-item>
          <q-item v-if="isMenuVisible('vehicles')" clickable v-ripple to="/produtos">
            <q-item-section avatar><q-icon name="directions_car" /></q-item-section>
            <q-item-section>Veículos</q-item-section>
          </q-item>
          <q-item v-if="isMenuVisible('partners')" clickable v-ripple to="/lojas-parceiras">
            <q-item-section avatar><q-icon name="store_mall_directory" /></q-item-section>
            <q-item-section>Lojas parceiras</q-item-section>
          </q-item>
          <q-item v-if="isMenuVisible('compare')" clickable v-ripple to="/comparar">
            <q-item-section avatar><q-icon name="compare" /></q-item-section>
            <q-item-section>Comparar</q-item-section>
          </q-item>
          <q-item v-if="isMenuVisible('favorites')" clickable v-ripple to="/favoritos">
            <q-item-section avatar><q-icon name="favorite_border" /></q-item-section>
            <q-item-section>Favoritos</q-item-section>
          </q-item>

          <template v-if="isMenuVisible('ads') || isMenuVisible('leads')">
            <q-separator class="q-my-sm" />
            <q-item-label header>Vendedor</q-item-label>
            <q-item v-if="isMenuVisible('ads')" clickable v-ripple to="/anuncios">
              <q-item-section avatar><q-icon name="campaign" /></q-item-section>
              <q-item-section>Meus Anúncios</q-item-section>
            </q-item>
            <q-item v-if="isMenuVisible('leads')" clickable v-ripple to="/leads">
              <q-item-section avatar><q-icon name="people" /></q-item-section>
              <q-item-section>Leads</q-item-section>
            </q-item>
          </template>

          <template v-if="isMenuVisible('partner-cta') || isMenuVisible('register')">
            <q-separator class="q-my-sm" />
            <q-item v-if="isMenuVisible('partner-cta')" clickable v-ripple to="/seja-parceiro">
              <q-item-section avatar><q-icon name="storefront" /></q-item-section>
              <q-item-section>Seja Parceiro</q-item-section>
            </q-item>
            <q-item v-if="isMenuVisible('register')" clickable v-ripple to="/registro">
              <q-item-section avatar><q-icon name="login" /></q-item-section>
              <q-item-section>Entrar / Cadastrar</q-item-section>
            </q-item>
          </template>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view />

      <footer class="app-footer q-pa-lg">
        <div class="footer-inner container row q-gutter-lg">
          <div class="col-12 col-sm-4">
            <img
              v-if="tenantLogoUrl"
              :src="tenantLogoUrl"
              :alt="tenantStoreName"
              class="footer-logo"
            />
            <p class="footer-brand">{{ tenantStoreName }}</p>
            <p class="text-grey-5 text-caption q-mb-none">{{ tenantFooterTagline }}</p>
          </div>
          <div
            v-if="isMenuVisible('vehicles') || isMenuVisible('partners') || isMenuVisible('partner-cta')"
            class="col"
          >
            <p class="footer-label">Navegação</p>
            <div class="column q-gutter-xs">
              <router-link v-if="isMenuVisible('vehicles')" to="/produtos" class="footer-link">Veículos</router-link>
              <router-link v-if="isMenuVisible('partners')" to="/lojas-parceiras" class="footer-link">Lojas parceiras</router-link>
              <router-link v-if="isMenuVisible('partner-cta')" to="/seja-parceiro" class="footer-link">Seja Parceiro</router-link>
            </div>
          </div>
          <div class="col">
            <p class="footer-label">Legal</p>
            <div class="column q-gutter-xs">
              <router-link to="/legal/termos-de-uso" class="footer-link">Termos de Uso</router-link>
              <router-link to="/legal/politica-de-privacidade" class="footer-link">Privacidade (LGPD)</router-link>
              <router-link to="/legal/politica-de-cookies" class="footer-link">Cookies</router-link>
            </div>
          </div>
        </div>
        <q-separator class="q-my-md footer-sep" />
        <p class="text-center text-grey-6 text-caption q-mb-none">
          © {{ new Date().getFullYear() }} {{ tenantStoreName }}. Todos os direitos reservados.
        </p>
      </footer>
    </q-page-container>
    <ConsentBanner />
    <LoadingOverlay />
  </q-layout>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppHeader from 'components/layout/AppHeader.vue'
import ConsentBanner from 'components/common/ConsentBanner.vue'
import LoadingOverlay from 'components/common/LoadingOverlay.vue'
import { useTenant } from 'src/composables/useTenant'
import { useAuthStore } from 'src/stores/authStore'
import { useConsent } from 'src/composables/useConsent'

const leftDrawerOpen = ref(false)
const auth = useAuthStore()
const consent = useConsent()
const isAuthenticated = computed(() => auth.isAuthenticated.value)
const userName = computed(() => auth.user.value?.name ?? '')

const {
  storeName: tenantStoreName,
  logoUrl: tenantLogoUrl,
  footerTagline: tenantFooterTagline,
  isMenuVisible,
} = useTenant()

onMounted(() => {
  consent.init()
})
</script>

<style scoped lang="scss">
.app-footer {
  background: #1a1a2e;
  color: white;
}

.container { max-width: 1280px; margin: 0 auto; }

.footer-logo {
  max-height: 44px;
  width: auto;
  object-fit: contain;
  display: block;
  margin: 0 0 10px;
}

.footer-brand {
  font-size: 1.4rem;
  font-weight: 900;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px;
}

.footer-label {
  font-size: 13px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.footer-link {
  color: rgba(255, 255, 255, 0.55);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.2s;

  &:hover { color: white; }
}

.footer-sep { background: rgba(255, 255, 255, 0.1); }
</style>
