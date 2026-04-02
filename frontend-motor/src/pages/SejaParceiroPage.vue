<template>
  <q-page class="parceiro-page">
    <!-- Hero -->
    <section class="parceiro-hero q-pa-xl text-center">
      <h1 class="hero-title">Seja um Parceiro <span class="gradient-text">Motorise</span></h1>
      <p class="hero-sub text-grey-6">Alcance milhares de compradores com planos flexíveis</p>
    </section>

    <!-- Plans -->
    <section class="plans-section container q-pa-lg">
      <div class="row q-gutter-lg justify-center">
        <q-card
          v-for="plan in plans"
          :key="plan.name"
          class="plan-card col-12 col-sm-5 col-md-3"
          :class="{ 'plan-card--featured': plan.featured }"
          flat
          bordered
        >
          <q-badge v-if="plan.featured" color="primary" label="Mais popular" class="plan-badge" floating />
          <q-card-section class="text-center q-pa-lg">
            <p class="plan-name">{{ plan.name }}</p>
            <p class="plan-price">
              <span class="plan-currency">R$</span>{{ plan.price }}
              <span class="plan-period">/mês</span>
            </p>
            <q-list dense class="q-mt-md text-left">
              <q-item v-for="feature in plan.features" :key="feature">
                <q-item-section avatar>
                  <q-icon name="check" color="positive" size="18px" />
                </q-item-section>
                <q-item-section>{{ feature }}</q-item-section>
              </q-item>
            </q-list>
            <q-btn
              :unelevated="plan.featured"
              :outline="!plan.featured"
              :color="plan.featured ? 'primary' : 'grey-6'"
              :label="plan.featured ? 'Começar agora' : 'Escolher plano'"
              full-width
              class="q-mt-lg"
              @click="selectPlan(plan.name)"
            />
          </q-card-section>
        </q-card>
      </div>
    </section>

    <!-- Registration CTA -->
    <section class="cta-section container q-pa-xl text-center">
      <h2 class="section-title">Pronto para começar?</h2>
      <p class="text-grey-6 q-mb-lg">Crie sua conta de vendedor em menos de 5 minutos</p>
      <q-btn unelevated color="primary" size="lg" label="Criar conta de vendedor" to="/registro" />
    </section>
  </q-page>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const plans = [
  {
    name: 'Básico',
    price: '99',
    featured: false,
    features: ['Até 10 anúncios', 'Painel de leads', 'Suporte por e-mail', 'Relatórios básicos'],
  },
  {
    name: 'Pro',
    price: '249',
    featured: true,
    features: ['Até 50 anúncios', 'Painel de leads avançado', 'Suporte prioritário', 'Relatórios completos', 'Destaque nos resultados', 'Página da loja personalizada'],
  },
  {
    name: 'Enterprise',
    price: '699',
    featured: false,
    features: ['Anúncios ilimitados', 'Multi-filiais', 'API de integração', 'Gerente dedicado', 'Todos os recursos Pro'],
  },
]

function selectPlan(name: string) {
  void router.push({ path: '/registro', query: { plan: name.toLowerCase() } })
}
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
.plan-price { font-size: 48px; font-weight: 900; margin: 0; line-height: 1; }
.plan-currency { font-size: 20px; vertical-align: top; margin-top: 8px; display: inline-block; }
.plan-period { font-size: 14px; color: #9e9e9e; }

.section-title { font-size: 1.8rem; font-weight: 800; margin-bottom: 12px; }
</style>
