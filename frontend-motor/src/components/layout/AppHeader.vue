<template>
  <q-header class="app-header" :class="themeClass">
    <q-toolbar class="header-toolbar">
      <q-btn
        flat
        round
        icon="menu"
        class="lt-md"
        aria-label="Menu"
        @click="$emit('toggle-drawer')"
      />

      <!-- Logo -->
      <router-link to="/" class="header-logo">
        <img v-if="tenantLogoUrl" :src="tenantLogoUrl" :alt="tenantStoreName" class="logo-img" />
        <img v-else-if="storeLogo" :src="storeLogo" :alt="storeName || tenantStoreName" class="logo-img" />
        <span v-else class="logo-text">{{ storeName || tenantStoreName }}</span>
      </router-link>

      <div class="desktop-nav gt-sm">
        <q-btn v-if="isMenuVisible('home')" flat no-caps to="/" label="Home" class="nav-btn" />
        <q-btn v-if="isMenuVisible('vehicles')" flat no-caps to="/produtos" label="Veículos" class="nav-btn" />
        <q-btn v-if="isMenuVisible('partners')" flat no-caps to="/lojas-parceiras" label="Lojas parceiras" class="nav-btn" />
      </div>

      <q-space />

      <!-- Smart Search (desktop) -->
      <div class="header-search gt-sm">
        <SmartSearch compact @search-advanced="onSearchAdvanced" />
      </div>

      <q-space />

      <!-- Actions -->
      <div class="header-actions row items-center q-gutter-sm">
        <NotificationBell v-if="isAuthenticated" />

        <q-btn
          flat
          round
          :icon="$q.dark.isActive ? 'light_mode' : 'dark_mode'"
          aria-label="Alternar tema"
          @click="$q.dark.toggle()"
        />

        <q-btn flat round icon="favorite_border" aria-label="Favoritos" to="/favoritos" />

        <q-btn
          v-if="!isAuthenticated"
          flat
          label="Entrar"
          class="btn-login"
          to="/registro"
        />

        <q-btn
          v-else
          flat
          round
          aria-label="Perfil"
        >
          <q-avatar size="32px" color="primary" text-color="white">
            {{ userInitial }}
          </q-avatar>
          <q-menu>
            <q-list style="min-width: 160px">
              <q-item clickable v-close-popup to="/perfil">
                <q-item-section>Meu Perfil</q-item-section>
              </q-item>
              <q-item clickable v-close-popup to="/favoritos">
                <q-item-section>Favoritos</q-item-section>
              </q-item>
              <q-item clickable v-close-popup to="/mensagens">
                <q-item-section>Mensagens</q-item-section>
              </q-item>
              <q-separator />
              <q-item clickable v-close-popup @click="logout">
                <q-item-section>Sair</q-item-section>
              </q-item>
            </q-list>
          </q-menu>
        </q-btn>

      </div>
    </q-toolbar>

    <!-- Mobile Search Row -->
    <div class="header-search-mobile lt-md q-pb-sm q-px-md">
      <SmartSearch compact @search-advanced="onSearchAdvanced" />
    </div>
  </q-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import SmartSearch from 'components/form/SmartSearch.vue'
import NotificationBell from 'components/business/NotificationBell.vue'
import { useTenant } from 'src/composables/useTenant'
import type { VehicleFilters } from 'src/data/types'

interface Props {
  storeName?: string
  storeLogo?: string
  themeClass?: string
  isAuthenticated?: boolean
  userName?: string
}

const props = withDefaults(defineProps<Props>(), {
  storeName: '',
  themeClass: '',
  isAuthenticated: false,
})

defineEmits<{ 'toggle-drawer': [] }>()

const router = useRouter()
const { storeName: tenantStoreName, logoUrl: tenantLogoUrl, isMenuVisible } = useTenant()

const userInitial = computed(() =>
  props.userName ? props.userName.charAt(0).toUpperCase() : 'U',
)

function onSearchAdvanced(payload: { query: string; filters: Pick<VehicleFilters, 'type' | 'brand' | 'fuel' | 'priceMax'> }) {
  const queryParams: Record<string, string> = {}
  if (payload.query) queryParams.search = payload.query
  if (payload.filters.type) queryParams.type = payload.filters.type
  if (payload.filters.brand) queryParams.brand = payload.filters.brand
  if (payload.filters.fuel) queryParams.fuel = payload.filters.fuel
  if (payload.filters.priceMax) queryParams.priceMax = String(payload.filters.priceMax)

  void router.push({ path: '/produtos', query: queryParams })
}

function logout() {
  void router.push('/')
}
</script>

<style scoped lang="scss">
.app-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(12px);
  color: var(--q-dark);
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);

  .header-toolbar {
    min-height: 64px;
    padding: 0 16px;
  }

  .logo-text {
    font-size: 1.4rem;
    font-weight: 700;
    background: var(--gradient-primary);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    white-space: nowrap;
  }

  .logo-img {
    height: 36px;
    object-fit: contain;
  }

  .header-search {
    flex: 0 1 480px;
  }

  .desktop-nav {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-left: 12px;

    .nav-btn {
      font-weight: 600;
      color: #444;
    }
  }

  .btn-login {
    font-weight: 600;
  }
}

.body--dark .app-header {
  background: rgba(29, 29, 29, 0.95);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  color: #f1f5ff;

  .desktop-nav .nav-btn {
    color: rgba(241, 245, 255, 0.9);
  }

  .btn-login {
    color: rgba(241, 245, 255, 0.92);
  }
}
</style>
