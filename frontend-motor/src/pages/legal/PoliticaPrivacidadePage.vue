<template>
  <q-page padding>
    <div class="legal-container">
      <h1>Política de Privacidade</h1>
      <p class="text-grey-7">Versão {{ version }} · Última atualização: {{ updatedAt }}</p>

      <p>
        A Motorise, operada pela Rizer, valoriza a privacidade dos seus usuários
        e está comprometida com a Lei Geral de Proteção de Dados (LGPD – Lei
        nº 13.709/2018). Este documento explica quais dados coletamos, por que
        coletamos e como você pode exercer seus direitos.
      </p>

      <h2>1. Dados que coletamos</h2>
      <ul>
        <li>Cadastro: nome, e-mail, telefone, CNPJ.</li>
        <li>Endereço da loja (para geolocalização de anúncios).</li>
        <li>Anúncios publicados: título, descrição, preço, fotos.</li>
        <li>Logs de acesso e uso da plataforma (para segurança).</li>
        <li>Cookies essenciais e, mediante consentimento, de analytics.</li>
      </ul>

      <h2>2. Finalidades</h2>
      <ul>
        <li>Operar a plataforma e exibir anúncios aos compradores.</li>
        <li>Cumprir obrigações legais e fiscais.</li>
        <li>Prevenir fraudes e garantir segurança.</li>
        <li>Enviar comunicações operacionais e, se consentido, marketing.</li>
      </ul>

      <h2>3. Bases legais</h2>
      <p>
        Tratamos dados com base em execução de contrato, cumprimento de
        obrigação legal, legítimo interesse e, quando aplicável, consentimento
        (cookies opcionais, marketing).
      </p>

      <h2>4. Compartilhamento</h2>
      <p>
        Compartilhamos dados com prestadores de serviço (Stripe para
        pagamentos, Magalu Cloud para armazenamento, Meta e Google para
        integrações de marketing) sempre sob contrato e somente o necessário.
      </p>

      <h2>5. Direitos do titular</h2>
      <p>
        Você pode solicitar acesso, correção, anonimização, portabilidade ou
        exclusão dos seus dados enviando e-mail para
        <a href="mailto:dpo@riser.com">dpo@riser.com</a> ou usando os botões abaixo
        (requer login). Responderemos em até 15 dias.
      </p>

      <q-banner v-if="!isAuthenticated" class="bg-info text-white q-mb-md">
        <q-icon name="info" class="q-mr-sm" />
        Faça <router-link to="/entrar" class="text-white text-weight-bold">login</router-link>
        para acessar seus direitos de export e exclusão de conta.
      </q-banner>

      <div v-else class="q-mb-lg">
        <h3 class="text-h6">Seus direitos (LGPD art. 18)</h3>

        <q-spinner v-if="exporting" color="primary" size="2em" />
        <q-banner v-if="exportMessage" class="bg-positive text-white q-mb-md">{{ exportMessage }}</q-banner>
        <q-banner v-if="exportError" class="bg-negative text-white q-mb-md">{{ exportError }}</q-banner>

        <q-list bordered>
          <q-item-label header>Histórico de exports</q-item-label>
          <q-item v-for="r in exports" :key="r.id">
            <q-item-section>
              <q-item-label>
                <q-badge :color="statusColor(r.status)" :label="r.status" class="q-mr-sm" />
                {{ formatDate(r.requestedAt) }}
              </q-item-label>
              <q-item-label v-if="r.completedAt" caption>
                Concluído em {{ formatDate(r.completedAt) }} · URL expira {{ formatDate(r.urlExpiresAt!) }}
              </q-item-label>
              <q-item-label v-if="r.errorMessage" caption class="text-negative">
                {{ r.errorMessage }}
              </q-item-label>
            </q-item-section>
            <q-item-section side v-if="r.downloadUrl">
              <q-btn flat color="primary" icon="download" label="Baixar" :href="r.downloadUrl" target="_blank" />
            </q-item-section>
          </q-item>
          <q-item v-if="exports.length === 0">
            <q-item-section>
              <q-item-label caption>Nenhum export solicitado ainda.</q-item-label>
            </q-item-section>
          </q-item>
        </q-list>

        <div class="q-mt-md q-gutter-md">
          <q-btn
            unelevated
            color="primary"
            icon="download"
            label="Solicitar cópia dos meus dados"
            :loading="exporting"
            @click="onExport"
          />
          <q-btn
            outline
            color="negative"
            icon="delete_forever"
            label="Excluir minha conta"
            @click="onDelete"
          />
        </div>
      </div>

      <h2>6. Retenção</h2>
      <p>
        Mantemos dados de anúncios enquanto a conta estiver ativa e por até 5
        anos após o cancelamento, para fins legais e fiscais. Leads sem
        interação são anonimizados após 2 anos.
      </p>

      <h2>7. Encarregado (DPO)</h2>
      <p>Rizer Marketplaces · <a href="mailto:dpo@riser.com">dpo@riser.com</a></p>
    </div>
  </q-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'
import { lgpdApi, type DataExportRequestView } from 'src/services/api'

defineProps<{ version: string; updatedAt: string }>()

const $q = useQuasar()
const auth = useAuthStore()
const isAuthenticated = computed(() => auth.isAuthenticated.value)

const exports = ref<DataExportRequestView[]>([])
const exporting = ref(false)
const exportMessage = ref<string | null>(null)
const exportError = ref<string | null>(null)

function statusColor(s: string): string {
  switch (s) {
    case 'ready': return 'positive'
    case 'pending': case 'processing': return 'warning'
    case 'failed': return 'negative'
    case 'expired': return 'grey'
    default: return 'grey'
  }
}

function formatDate(d: string): string {
  if (!d) return '—'
  try { return new Date(d).toLocaleString('pt-BR') } catch { return d }
}

async function loadExports() {
  if (!isAuthenticated.value) return
  try {
    exports.value = await lgpdApi.myExports()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    exportError.value = detail || 'Não foi possível carregar exports.'
  }
}

async function onExport() {
  exporting.value = true
  exportError.value = null
  exportMessage.value = null
  try {
    const req = await lgpdApi.requestDataExport()
    exportMessage.value = `Solicitação criada. Status: ${req.status}. Você receberá o link em alguns minutos.`
    await loadExports()
  } catch (e: unknown) {
    const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
    exportError.value = detail || 'Falha ao solicitar export.'
  } finally {
    exporting.value = false
  }
}

function onDelete() {
  $q.dialog({
    title: 'Excluir conta',
    message: 'Tem certeza? Seus dados pessoais serão anonimizados e a conta será desativada. Esta ação não pode ser desfeita.',
    prompt: { model: '', isValid: (v) => v.length >= 4, label: 'Digite EXCLUIR para confirmar', type: 'text' },
    cancel: true,
    color: 'negative',
  }).onOk((prompt) => {
    void (async () => {
      try {
        const res = await lgpdApi.deleteAccount(prompt.value)
        $q.notify({ message: res.message, color: 'positive' })
        await auth.logout()
        void (window.location.href = '/')
      } catch (e: unknown) {
        const detail = (e as { response?: { data?: { detail?: string } } })?.response?.data?.detail
        $q.notify({ message: detail || 'Falha ao excluir conta.', color: 'negative' })
      }
    })()
  })
}

onMounted(loadExports)
</script>

<style scoped lang="scss">
.legal-container {
  max-width: 800px;
  margin: 0 auto;
  line-height: 1.6;
}
.legal-container h1 { font-size: 1.8rem; font-weight: 800; margin: 0 0 8px; }
.legal-container h2 { font-size: 1.15rem; font-weight: 700; margin: 24px 0 8px; }
</style>
