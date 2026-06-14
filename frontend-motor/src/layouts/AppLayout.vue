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
          <q-item-label header>{{ tenantStoreName }}</q-item-label>
          <q-item clickable v-ripple :to="{ name: 'app-dashboard' }" exact>
            <q-item-section avatar><q-icon name="dashboard" /></q-item-section>
            <q-item-section>Dashboard</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-company' }">
            <q-item-section avatar><q-icon name="business" /></q-item-section>
            <q-item-section>Minha Empresa</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-products' }">
            <q-item-section avatar><q-icon name="campaign" /></q-item-section>
            <q-item-section>Anúncios</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-leads' }">
            <q-item-section avatar><q-icon name="people" /></q-item-section>
            <q-item-section>Leads</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-sellers' }">
            <q-item-section avatar><q-icon name="badge" /></q-item-section>
            <q-item-section>Vendedores</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-members' }">
            <q-item-section avatar><q-icon name="group" /></q-item-section>
            <q-item-section>Membros</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-billing' }">
            <q-item-section avatar><q-icon name="credit_card" /></q-item-section>
            <q-item-section>Assinatura</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'app-integrations' }">
            <q-item-section avatar><q-icon name="share" /></q-item-section>
            <q-item-section>Integrações</q-item-section>
          </q-item>
        </q-list>
      </q-scroll-area>
    </q-drawer>

    <q-page-container>
      <router-view />
    </q-page-container>
  </q-layout>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import AppHeader from 'components/layout/AppHeader.vue'
import { useAuthStore } from 'src/stores/authStore'
import { useTenant } from 'src/composables/useTenant'

const leftDrawerOpen = ref(false)
const auth = useAuthStore()
const isAuthenticated = computed(() => auth.isAuthenticated.value)
const userName = computed(() => auth.user.value?.name ?? '')

const { storeName: tenantStoreName } = useTenant()
</script>
