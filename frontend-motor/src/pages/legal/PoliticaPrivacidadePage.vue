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
        <a href="mailto:dpo@riser.com">dpo@riser.com</a>. Responderemos em até
        15 dias.
      </p>

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
defineProps<{ version: string; updatedAt: string }>()
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
