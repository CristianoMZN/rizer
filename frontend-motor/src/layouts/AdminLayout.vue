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
          <q-item-label header>Administração</q-item-label>
          <q-item clickable v-ripple :to="{ name: 'admin-dashboard' }" exact>
            <q-item-section avatar><q-icon name="dashboard" /></q-item-section>
            <q-item-section>Dashboard</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'admin-tenants' }">
            <q-item-section avatar><q-icon name="business" /></q-item-section>
            <q-item-section>Tenants</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'admin-users' }">
            <q-item-section avatar><q-icon name="group" /></q-item-section>
            <q-item-section>Usuários</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'admin-plans' }">
            <q-item-section avatar><q-icon name="card_membership" /></q-item-section>
            <q-item-section>Planos</q-item-section>
          </q-item>
          <q-item clickable v-ripple :to="{ name: 'admin-payments' }">
            <q-item-section avatar><q-icon name="receipt_long" /></q-item-section>
            <q-item-section>Pagamentos</q-item-section>
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

const leftDrawerOpen = ref(false)
const auth = useAuthStore()
const isAuthenticated = computed(() => auth.isAuthenticated.value)
const userName = computed(() => auth.user.value?.name ?? '')
</script>
