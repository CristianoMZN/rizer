<template>
  <q-btn flat round :aria-label="`${unreadCount} notificações`">
    <q-icon name="notifications_none" />
    <q-badge
      v-if="unreadCount"
      color="red"
      :label="unreadCount > 9 ? '9+' : unreadCount"
      floating
    />

    <q-menu max-width="360px" max-height="480px">
      <div class="notifications-header row items-center justify-between q-pa-md">
        <span class="text-weight-bold">Notificações</span>
        <q-btn flat size="xs" label="Marcar tudo como lido" @click="markAllRead" />
      </div>
      <q-separator />

      <q-list v-if="notifications.length">
        <q-item
          v-for="n in notifications"
          :key="n.id"
          :class="{ 'unread-item': !n.read }"
          clickable
          v-close-popup
          @click="openNotification(n)"
        >
          <q-item-section avatar>
            <q-avatar :color="iconMap[n.type].color" text-color="white" size="36px" icon="">
              <q-icon :name="iconMap[n.type].icon" size="20px" />
            </q-avatar>
          </q-item-section>
          <q-item-section>
            <q-item-label class="text-weight-bold" :class="{ 'text-primary': !n.read }">
              {{ n.title }}
            </q-item-label>
            <q-item-label caption>{{ n.message }}</q-item-label>
            <q-item-label caption class="text-grey-4">{{ formatDate(n.createdAt) }}</q-item-label>
          </q-item-section>
          <q-item-section v-if="!n.read" side>
            <div class="unread-dot" />
          </q-item-section>
        </q-item>
      </q-list>

      <div v-else class="q-pa-xl flex flex-center column text-grey-5">
        <q-icon name="notifications_off" size="48px" />
        <p class="q-mt-sm">Nenhuma notificação</p>
      </div>
    </q-menu>
  </q-btn>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import type { Notification, NotificationType } from 'src/data/types'
import { api } from 'src/services/apiMock'

const router = useRouter()
const notifications = ref<Notification[]>([])

const iconMap: Record<NotificationType, { icon: string; color: string }> = {
  new_lead: { icon: 'person_add', color: 'primary' },
  price_drop: { icon: 'trending_down', color: 'positive' },
  message: { icon: 'message', color: 'secondary' },
  system: { icon: 'info', color: 'grey' },
}

const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)

onMounted(async () => {
  notifications.value = await api.getNotifications()
})

function markAllRead() {
  notifications.value.forEach((n) => { n.read = true })
}

function openNotification(n: Notification) {
  n.read = true
  if (n.vehicleId) void router.push(`/produto/${n.vehicleId}`)
}

function formatDate(iso: string) {
  const d = new Date(iso)
  return d.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped lang="scss">
.notifications-header {
  position: sticky;
  top: 0;
  background: white;
  z-index: 1;

  .body--dark & { background: var(--q-dark); }
}

.unread-item {
  background: rgba(var(--q-primary-rgb, 25, 118, 210), 0.04);
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--q-primary);
}
</style>
