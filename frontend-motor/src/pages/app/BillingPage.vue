<template>
  <q-page padding>
    <q-banner v-if="!auth.currentTenantId.value" class="bg-warning text-black q-mb-md">
      Selecione um tenant antes de gerenciar a assinatura.
    </q-banner>

    <div v-else>
      <h1 class="text-h5 q-mb-md">Assinatura</h1>

      <q-banner v-if="error" class="bg-negative text-white q-mb-md">{{ error }}</q-banner>

      <q-spinner v-if="loading && !subscription" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />

      <!-- Plano atual -->
      <q-card v-if="subscription" flat bordered class="q-mb-md">
        <q-card-section>
          <div class="row items-center">
            <div class="col">
              <div class="text-overline">Plano atual</div>
              <div class="text-h5">{{ subscription.planName }}</div>
              <div class="text-caption text-grey-7">
                {{ formatPrice(subscription.price, subscription.currency) }} /
                {{ subscription.source === 'trial' ? 'trial' : 'mês' }}
              </div>
            </div>
            <div class="col-auto">
              <q-badge :color="statusColor(subscription.status)" :label="statusLabel(subscription.status)" />
            </div>
          </div>

          <q-banner
            v-if="subscription.status === 'trialing' && subscription.trialDaysRemaining != null"
            class="bg-info text-white q-mt-md"
          >
            Você está em trial. Restam <strong>{{ subscription.trialDaysRemaining }}</strong> dia(s).
            <q-btn flat dense color="white" label="Assinar agora" to="/app/assinatura" class="q-ml-sm" />
          </q-banner>

          <q-banner
            v-if="subscription.isInGracePeriod"
            class="bg-warning text-black q-mt-md"
          >
            Pagamento falhou. Você ainda tem acesso por alguns dias (grace period).
            <q-btn flat dense color="black" label="Atualizar cartão" @click="openPortal" class="q-ml-sm" />
          </q-banner>

          <q-banner
            v-if="subscription.cancelAtPeriodEnd"
            class="bg-orange text-white q-mt-md"
          >
            Sua assinatura será cancelada em {{ formatDate(subscription.currentPeriodEnd) }}.
            <q-btn flat dense color="white" label="Reativar" @click="onResume" class="q-ml-sm" />
          </q-banner>

          <q-separator class="q-my-md" />
          <div class="row q-col-gutter-md">
            <div class="col-6 col-sm-3">
              <div class="text-caption text-grey-6">Início do período</div>
              <div>{{ formatDate(subscription.currentPeriodStart) }}</div>
            </div>
            <div class="col-6 col-sm-3">
              <div class="text-caption text-grey-6">Fim do período</div>
              <div>{{ formatDate(subscription.currentPeriodEnd) }}</div>
            </div>
            <div class="col-6 col-sm-3" v-if="subscription.trialEnd">
              <div class="text-caption text-grey-6">Fim do trial</div>
              <div>{{ formatDate(subscription.trialEnd) }}</div>
            </div>
            <div class="col-6 col-sm-3" v-if="subscription.stripeCustomerId">
              <div class="text-caption text-grey-6">Cliente Stripe</div>
              <div class="text-caption">{{ subscription.stripeCustomerId }}</div>
            </div>
          </div>

          <q-card-actions class="q-mt-md">
            <q-btn
              v-if="!subscription.cancelAtPeriodEnd && subscription.source === 'stripe'"
              outline
              color="primary"
              icon="credit_card"
              label="Gerenciar no portal"
              @click="openPortal"
            />
            <q-btn
              v-if="!subscription.cancelAtPeriodEnd"
              outline
              color="negative"
              icon="cancel"
              label="Cancelar ao final do período"
              @click="onCancel"
            />
            <q-space />
            <q-btn
              flat
              color="primary"
              :label="subscription.cancelAtPeriodEnd ? 'Reativar' : 'Trocar plano'"
              @click="subscription.cancelAtPeriodEnd ? onResume() : (showPlanSelector = !showPlanSelector)"
            />
          </q-card-actions>
        </q-card-section>
      </q-card>

      <q-card v-else flat bordered class="q-mb-md">
        <q-card-section>
          <div class="text-h6">Sem assinatura ativa</div>
          <p class="text-body2 text-grey-7">
            Comece com um trial gratuito de 7 dias (Básico) ou 14 dias (PRO / Platinum).
          </p>
          <q-btn unelevated color="primary" label="Ver planos" to="/seja-parceiro" />
        </q-card-section>
      </q-card>

      <!-- Plano selector -->
      <q-dialog v-model="showPlanSelector" position="bottom">
        <q-card style="width: 100%; max-width: 800px;">
          <q-card-section>
            <div class="text-h6">Escolha um plano</div>
          </q-card-section>
          <q-card-section class="q-pt-none row q-col-gutter-md">
            <div
              v-for="p in plans"
              :key="p.code"
              class="col-12 col-sm-4"
            >
              <q-card
                flat
                bordered
                :class="['plan-card', subscription?.planCode === p.code ? 'plan-card-current' : 'cursor-pointer']"
                @click="onSwitch(p.code)"
              >
                <q-card-section>
                  <div class="text-h6">{{ p.name }}</div>
                  <div class="text-h5 text-primary">{{ formatPrice(p.price, p.currency) }}<span class="text-caption text-grey-6">/mês</span></div>
                  <div class="text-caption text-grey-7 q-mt-xs">
                    {{ p.maxPhysicalStores ? `Até ${p.maxPhysicalStores} loja(s)` : 'Lojas ilimitadas' }}
                  </div>
                  <q-list dense class="q-mt-sm">
                    <q-item v-if="p.hasPartnerPage" dense>
                      <q-icon name="check" color="positive" size="16px" class="q-mr-xs" />
                      <q-item-section>Página de parceiro</q-item-section>
                    </q-item>
                    <q-item v-if="p.hasCustomDomain" dense>
                      <q-icon name="check" color="positive" size="16px" class="q-mr-xs" />
                      <q-item-section>Domínio customizado</q-item-section>
                    </q-item>
                    <q-item v-if="p.hasInstagram" dense>
                      <q-icon name="check" color="positive" size="16px" class="q-mr-xs" />
                      <q-item-section>Instagram (post por anúncio)</q-item-section>
                    </q-item>
                    <q-item v-if="p.hasMetaDpa" dense>
                      <q-icon name="check" color="positive" size="16px" class="q-mr-xs" />
                      <q-item-section>META Dynamic Product Ads</q-item-section>
                    </q-item>
                    <q-item v-if="p.hasGoogleShopping" dense>
                      <q-icon name="check" color="positive" size="16px" class="q-mr-xs" />
                      <q-item-section>Google Shopping</q-item-section>
                    </q-item>
                  </q-list>
                  <div v-if="p.trialDays > 0" class="text-caption text-info q-mt-sm">
                    Inclui {{ p.trialDays }} dias de teste grátis
                  </div>
                </q-card-section>
              </q-card>
            </div>
          </q-card-section>
          <q-card-actions align="right">
            <q-btn flat label="Fechar" v-close-popup />
          </q-card-actions>
        </q-card>
      </q-dialog>

      <!-- Histórico de pagamentos -->
      <q-card v-if="subscription" flat bordered class="q-mt-md">
        <q-card-section>
          <div class="text-h6">Histórico de pagamentos</div>
        </q-card-section>
        <q-table
          :rows="payments"
          :columns="paymentColumns"
          row-key="id"
          :loading="loadingPayments"
          flat
          :rows-per-page-options="[10, 20, 50]"
        >
          <template #body-cell-amount="props">
            <q-td :props="props">{{ formatPrice(props.row.amount, props.row.currency) }}</q-td>
          </template>
          <template #body-cell-method="props">
            <q-td :props="props">
              <q-chip size="sm" :label="methodLabel(props.row.method)" />
            </q-td>
          </template>
          <template #body-cell-status="props">
            <q-td :props="props">
              <q-badge :color="paymentStatusColor(props.row.status)" :label="props.row.status" />
            </q-td>
          </template>
          <template #body-cell-paidAt="props">
            <q-td :props="props">
              {{ props.row.paidAt ? formatDate(props.row.paidAt) : '—' }}
            </q-td>
          </template>
          <template #no-data>
            <div class="full-width text-center q-pa-md text-grey-6">
              Nenhum pagamento registrado ainda.
            </div>
          </template>
        </q-table>
      </q-card>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useQuasar, type QTableColumn } from 'quasar'
import { useRoute } from 'vue-router'
import { useAuthStore } from 'src/stores/authStore'
import {
  billingApi,
  type PlanView, type SubscriptionView, type PaymentView,
} from 'src/services/api'

const $q = useQuasar()
const route = useRoute()
const auth = useAuthStore()

const subscription = ref<SubscriptionView | null>(null)
const plans = ref<PlanView[]>([])
const payments = ref<PaymentView[]>([])
const loading = ref(false)
const loadingPayments = ref(false)
const error = ref<string | null>(null)
const showPlanSelector = ref(false)

const paymentColumns: QTableColumn<PaymentView>[] = [
  { name: 'paidAt', label: 'Pago em', field: 'paidAt', align: 'left' },
  { name: 'amount', label: 'Valor', field: 'amount', align: 'right' },
  { name: 'method', label: 'Método', field: 'method', align: 'left' },
  { name: 'status', label: 'Status', field: 'status', align: 'left' },
  { name: 'description', label: 'Descrição', field: 'description', align: 'left' },
]

function statusColor(s: string): string {
  switch (s) {
    case 'active': return 'positive'
    case 'trialing': return 'info'
    case 'past_due': return 'warning'
    case 'paused': return 'grey'
    case 'unpaid': return 'orange'
    case 'canceled': return 'negative'
    case 'incomplete': return 'orange'
    default: return 'grey'
  }
}
function statusLabel(s: string): string {
  switch (s) {
    case 'active': return 'Ativa'
    case 'trialing': return 'Em trial'
    case 'past_due': return 'Pagamento pendente'
    case 'paused': return 'Pausada'
    case 'unpaid': return 'Inadimplente'
    case 'canceled': return 'Cancelada'
    case 'incomplete': return 'Incompleta'
    case 'incomplete_expired': return 'Expirada'
    default: return s
  }
}
function methodLabel(m: string): string {
  const map: Record<string, string> = {
    stripe_card: 'Stripe cartão',
    stripe_pix: 'Stripe Pix',
    stripe_boleto: 'Stripe boleto',
    manual_cash: 'Dinheiro',
    manual_bank_transfer: 'Transferência',
    manual_pix_external: 'Pix externo',
    manual_bonus: 'Bônus',
    manual_courtesy: 'Cortesia',
    manual_other: 'Outro (manual)',
  }
  return map[m] ?? m
}
function paymentStatusColor(s: string): string {
  switch (s) {
    case 'succeeded': return 'positive'
    case 'pending': return 'warning'
    case 'failed': return 'negative'
    case 'refunded': return 'info'
    case 'voided': return 'grey'
    case 'chargeback': return 'negative'
    default: return 'grey'
  }
}

function formatPrice(v: number, c: string): string {
  try { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: c }).format(v) }
  catch { return v.toFixed(2) }
}
function formatDate(d: string): string {
  if (!d) return '—'
  try { return new Date(d).toLocaleDateString('pt-BR') } catch { return d }
}

async function load() {
  if (!auth.currentTenantId.value) return
  loading.value = true
  error.value = null
  try {
    const [sub, pl] = await Promise.all([
      billingApi.getSubscription().catch((e: { response?: { status?: number } }) => {
        if (e?.response?.status === 404) return null
        throw e
      }),
      billingApi.listPlans(),
    ])
    subscription.value = sub
    plans.value = pl
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    error.value = detail || 'Não foi possível carregar a assinatura.'
  } finally {
    loading.value = false
  }
  await loadPayments()
}

async function loadPayments() {
  if (!auth.currentTenantId.value) return
  loadingPayments.value = true
  try {
    const res = await billingApi.listMyPayments()
    payments.value = res.content
  } catch {
    payments.value = []
  } finally {
    loadingPayments.value = false
  }
}

async function onSwitch(planCode: string) {
  if (subscription.value?.planCode === planCode) {
    showPlanSelector.value = false
    return
  }
  try {
    const checkout = await billingApi.checkout(planCode)
    if (checkout.checkoutUrl) {
      window.location.href = checkout.checkoutUrl
    }
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao iniciar checkout.', color: 'negative' })
  }
}

async function openPortal() {
  try {
    const res = await billingApi.portal()
    if (res.portalUrl) window.open(res.portalUrl, '_blank')
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Portal não disponível no momento.', color: 'negative' })
  }
}

function onCancel() {
  $q.dialog({
    title: 'Cancelar assinatura',
    message: 'Sua assinatura será cancelada ao final do período corrente. Continuar?',
    cancel: true,
    color: 'negative',
  }).onOk(() => {
    void (async () => {
      try {
        subscription.value = await billingApi.cancel()
        $q.notify({ message: 'Cancelamento agendado.', color: 'info' })
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao cancelar.', color: 'negative' })
      }
    })()
  })
}

async function onResume() {
  try {
    subscription.value = await billingApi.resume()
    $q.notify({ message: 'Assinatura reativada.', color: 'positive' })
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Falha ao reativar.', color: 'negative' })
  }
}

onMounted(async () => {
  // Trata retorno simulado do Stripe (modo dev): ?session_id=…&plan=…&simulated=1
  const sim = route.query.simulated === '1'
  const sessionId = route.query.session_id as string | undefined
  const planCode = route.query.plan as string | undefined
  if (sim && sessionId && planCode && auth.currentTenantId.value) {
    try {
      await http_post_simulate(sessionId, planCode)
      $q.notify({ message: 'Checkout simulado aplicado.', color: 'positive' })
    } catch {
      $q.notify({ message: 'Falha ao simular checkout.', color: 'negative' })
    }
  }
  await load()
})

// Helper local para não importar `http` aqui fora do escopo do `api.ts`.
import { http } from 'src/services/api'
async function http_post_simulate(sessionId: string, planCode: string) {
  await http.post('/billing/webhooks/simulate', {
    tenantId: auth.currentTenantId.value,
    planCode,
    sessionId,
    customerId: 'cus_dev_' + sessionId.slice(0, 12),
  })
}
</script>

<style scoped>
.plan-card { transition: transform 0.2s ease; }
.plan-card:hover { transform: translateY(-2px); }
.plan-card-current { border-color: var(--q-primary); border-width: 2px; }
</style>
