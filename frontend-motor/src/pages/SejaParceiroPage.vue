<template>
  <q-page class="parceiro-page">
    <section class="parceiro-hero q-pa-xl text-center">
      <h1 class="hero-title">Seja um Parceiro <span class="gradient-text">Motorise</span></h1>
      <p class="hero-sub text-grey-6">Alcance milhares de compradores com planos flexíveis</p>
    </section>

    <section class="plans-section container q-pa-lg">
      <q-spinner v-if="loading" color="primary" size="3em" class="block q-mx-auto q-mt-xl" />
      <div v-else class="row q-gutter-lg justify-center">
        <q-card
          v-for="plan in plans"
          :key="plan.code"
          class="plan-card col-12 col-sm-6 col-md-3"
          :class="{ 'plan-card--featured': plan.code === 'PRO' }"
          flat
          bordered
        >
          <q-badge v-if="plan.code === 'PRO'" color="primary" label="Mais popular" class="plan-badge" floating />
          <q-card-section class="text-center q-pa-lg">
            <p class="plan-name">{{ plan.name }}</p>
            <p class="plan-price">
              <span class="plan-currency">{{ currencySymbol(plan.currency) }}</span>{{ formatInt(plan.price) }}
              <span class="plan-period">/mês</span>
            </p>
            <p v-if="plan.description" class="text-caption text-grey-6 q-mt-sm">{{ plan.description }}</p>
            <q-list dense class="q-mt-md text-left">
              <q-item dense>
                <q-item-section avatar>
                  <q-icon :name="plan.maxPhysicalStores ? 'check' : 'all_inclusive'" :color="plan.maxPhysicalStores ? 'positive' : 'primary'" size="18px" />
                </q-item-section>
                <q-item-section>
                  {{ plan.maxPhysicalStores ? `Até ${plan.maxPhysicalStores} loja(s)` : 'Lojas ilimitadas' }}
                </q-item-section>
              </q-item>
              <q-item v-if="plan.hasPartnerPage" dense>
                <q-item-section avatar><q-icon name="check" color="positive" size="18px" /></q-item-section>
                <q-item-section>Página de parceiro</q-item-section>
              </q-item>
              <q-item v-if="plan.hasCustomDomain" dense>
                <q-item-section avatar><q-icon name="check" color="positive" size="18px" /></q-item-section>
                <q-item-section>Domínio customizado</q-item-section>
              </q-item>
              <q-item v-if="plan.hasInstagram" dense>
                <q-item-section avatar><q-icon name="check" color="positive" size="18px" /></q-item-section>
                <q-item-section>Integração com Instagram</q-item-section>
              </q-item>
              <q-item v-if="plan.hasMetaDpa" dense>
                <q-item-section avatar><q-icon name="check" color="positive" size="18px" /></q-item-section>
                <q-item-section>META Dynamic Product Ads</q-item-section>
              </q-item>
              <q-item v-if="plan.hasGoogleShopping" dense>
                <q-item-section avatar><q-icon name="check" color="positive" size="18px" /></q-item-section>
                <q-item-section>Google Shopping</q-item-section>
              </q-item>
            </q-list>
            <div v-if="plan.trialDays > 0" class="text-caption text-info q-mt-sm">
              <q-icon name="schedule" size="14px" /> {{ plan.trialDays }} dias grátis
            </div>
            <q-btn
              :unelevated="plan.code === 'PRO'"
              :outline="plan.code !== 'PRO'"
              :color="plan.code === 'PRO' ? 'primary' : 'grey-6'"
              label="Assinar"
              full-width
              class="q-mt-lg"
              @click="onSubscribe(plan.code)"
            />
          </q-card-section>
        </q-card>
      </div>
    </section>

    <section class="cta-section container q-pa-xl text-center">
      <h2 class="section-title">Pronto para começar?</h2>
      <p class="text-grey-6 q-mb-lg">
        Após a assinatura, o admin Motorise provisiona seu tenant e você recebe o acesso ao painel.
      </p>
      <q-btn unelevated color="primary" size="lg" label="Falar com vendas" to="/entrar" />
    </section>
  </q-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { billingApi, type PlanView } from 'src/services/api'

const $q = useQuasar()
const router = useRouter()

const plans = ref<PlanView[]>([])
const loading = ref(false)

function formatInt(v: number): string {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 0 }).format(v)
}

function currencySymbol(c: string): string {
  try { return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: c, maximumFractionDigits: 0 })
    .formatToParts(0).find((p) => p.type === 'currency')?.value ?? c + ' ' }
  catch { return c + ' ' }
}

async function load() {
  loading.value = true
  try {
    plans.value = await billingApi.listPublicPlans('BR')
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    $q.notify({ message: detail || 'Não foi possível carregar os planos.', color: 'negative' })
  } finally {
    loading.value = false
  }
}

function onSubscribe(planCode: string) {
  $q.notify({
    message: 'Para assinar, fale com nosso time de vendas — um admin vai provisionar seu tenant.',
    color: 'info',
    position: 'top',
  })
  void router.push({ path: '/entrar', query: { plan: planCode } })
}

onMounted(load)
</script>

<style scoped lang="scss">
.container { max-width: 1100px; margin: 0 auto; }
.parceiro-hero {
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 80px 24px;
}
.hero-title {
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 900;
  margin-bottom: 12px;
}
.gradient-text {
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-sub { font-size: 1.1rem; }
.plan-card {
  border-radius: 20px;
  position: relative;
  transition: transform 0.2s ease;
  &:hover { transform: translateY(-4px); }
  &--featured {
    border-color: var(--q-primary);
    box-shadow: 0 8px 32px rgba(102, 126, 234, 0.25);
  }
}
.plan-badge { top: -8px; right: 16px; }
.plan-name { font-weight: 700; font-size: 18px; margin-bottom: 8px; }
.plan-price { font-size: 42px; font-weight: 900; margin: 0; line-height: 1; }
.plan-currency { font-size: 20px; vertical-align: top; margin-top: 8px; display: inline-block; }
.plan-period { font-size: 14px; color: #9e9e9e; }
.section-title { font-size: 1.8rem; font-weight: 800; margin-bottom: 12px; }
</style>
