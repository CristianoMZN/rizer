<template>
  <q-dialog v-model="bannerOpen" position="bottom" persistent seamless>
    <q-card flat bordered class="consent-card">
      <q-card-section>
        <div class="row items-start">
          <q-icon name="cookie" size="32px" color="primary" class="q-mr-sm" />
          <div>
            <div class="text-subtitle1">Sua privacidade</div>
            <p class="text-body2 text-grey-8 q-mt-xs q-mb-none">
              Usamos cookies essenciais para o funcionamento da plataforma. Com seu consentimento,
              também usamos cookies de analytics e marketing para melhorar a experiência e
              mensurar o uso. Você pode revisar suas escolhas a qualquer momento pelo
              botão "Privacidade" no rodapé.
            </p>
          </div>
        </div>
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md q-pt-none">
        <q-btn flat label="Configurar" @click="openConfig" />
        <q-btn flat color="negative" label="Recusar não-essenciais" @click="onReject" />
        <q-btn unelevated color="primary" label="Aceitar tudo" @click="onAccept" />
      </q-card-actions>
    </q-card>
  </q-dialog>

  <q-dialog v-model="configOpen" position="bottom" seamless>
    <q-card flat bordered class="consent-card" style="width: 100%; max-width: 640px;">
      <q-card-section>
        <div class="text-h6">Configurar privacidade</div>
        <p class="text-caption text-grey-7">
          Cookies essenciais são obrigatórios. Os demais você pode ligar ou desligar.
        </p>
      </q-card-section>
      <q-card-section class="q-pt-none">
        <q-list>
          <q-item tag="label" v-ripple>
            <q-item-section>
              <q-item-label>Essenciais</q-item-label>
              <q-item-label caption>Login, sessão, segurança.</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle v-model="local.terms_of_use" disable />
            </q-item-section>
          </q-item>
          <q-item tag="label" v-ripple>
            <q-item-section>
              <q-item-label>Analytics</q-item-label>
              <q-item-label caption>Métricas agregadas de uso.</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle v-model="local.cookies_analytics" />
            </q-item-section>
          </q-item>
          <q-item tag="label" v-ripple>
            <q-item-section>
              <q-item-label>Marketing</q-item-label>
              <q-item-label caption>Meta Pixel, Google Ads.</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle v-model="local.cookies_marketing" />
            </q-item-section>
          </q-item>
          <q-item tag="label" v-ripple>
            <q-item-section>
              <q-item-label>E-mails de marketing</q-item-label>
              <q-item-label caption>Novidades, dicas e ofertas da Motorise.</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle v-model="local.marketing_emails" />
            </q-item-section>
          </q-item>
          <q-item tag="label" v-ripple>
            <q-item-section>
              <q-item-label>Compartilhar com integrações</q-item-label>
              <q-item-label caption>Permite sincronizar seus dados com Meta/Google para anúncios.</q-item-label>
            </q-item-section>
            <q-item-section side>
              <q-toggle v-model="local.data_sharing_integrations" />
            </q-item-section>
          </q-item>
        </q-list>
      </q-card-section>
      <q-card-actions align="right" class="q-pa-md">
        <q-btn flat label="Cancelar" v-close-popup />
        <q-btn unelevated color="primary" label="Salvar preferências" @click="onSave" v-close-popup />
      </q-card-actions>
    </q-card>
  </q-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useConsent } from 'src/composables/useConsent'

const consent = useConsent()
const bannerOpen = computed({
  get: () => consent.visible.value,
  set: (v) => { if (!v) consent.closeConfig() },
})
const configOpen = computed({
  get: () => consent.configOpen.value,
  set: (v) => { if (v) consent.openConfig(); else consent.closeConfig() },
})

const local = ref<Record<string, boolean>>({
  terms_of_use: true,
  privacy_policy: true,
  cookies_essential: true,
  cookies_analytics: false,
  cookies_marketing: false,
  marketing_emails: false,
  data_sharing_integrations: false,
})

watch(() => consent.configOpen.value, (open) => {
  if (open) {
    // Pré-popula com os valores já salvos
    local.value = {
      terms_of_use: true,
      privacy_policy: true,
      cookies_essential: true,
      cookies_analytics: consent.consents.value.cookies_analytics ?? false,
      cookies_marketing: consent.consents.value.cookies_marketing ?? false,
      marketing_emails: consent.consents.value.marketing_emails ?? false,
      data_sharing_integrations: consent.consents.value.data_sharing_integrations ?? false,
    }
  }
})

function onAccept() { consent.acceptAll() }
function onReject() { consent.rejectNonEssential() }
function onSave() { consent.saveCustom({ ...local.value }) }
function openConfig() { consent.openConfig() }
</script>

<style scoped>
.consent-card { width: 100%; max-width: 720px; border-radius: 16px; }
</style>
