<template>
  <q-page padding>
    <div class="legal-container">
      <h1>Termos de Uso</h1>
      <p class="text-grey-7">Versão {{ version }} · Última atualização: {{ updatedAt }}</p>

      <p>
        Bem-vindo à Motorise. Ao utilizar a plataforma você concorda com os termos
        descritos abaixo. Este é um texto-modelo e deve ser revisado pelo time
        jurídico da Rizer antes de entrar em produção.
      </p>

      <h2>1. Aceitação</h2>
      <p>
        O uso da plataforma Motorise implica aceitação integral destes Termos.
        Caso discorde, não utilize o serviço.
      </p>

      <h2>2. Cadastro</h2>
      <p>
        O cadastro na plataforma é realizado exclusivamente por administrador da
        Rizer ou da empresa parceira. O usuário é responsável por manter suas
        credenciais em segurança.
      </p>

      <h2>3. Planos e pagamentos</h2>
      <p>
        A Motorise oferece os planos Básico, PRO e Platinum. O pagamento é
        processado via Stripe (cartão) ou manualmente, conforme acordado entre
        a Rizer e a empresa parceira.
      </p>

      <h2>4. Conteúdo do usuário</h2>
      <p>
        O parceiro é responsável pelos anúncios publicados e pelas imagens
        enviadas. É proibido anunciar veículos sem propriedade legal ou com
        informações falsas.
      </p>

      <h2>5. Suspensão e cancelamento</h2>
      <p>
        A Motorise pode suspender contas inadimplentes ou que violem estes
        Termos. Cancelamentos seguem o ciclo vigente da assinatura.
      </p>

      <h2>6. Limitação de responsabilidade</h2>
      <p>
        A Motorise não se responsabiliza por negociações entre compradores e
        vendedores, nem por danos decorrentes do uso da plataforma.
      </p>

      <h2>7. Foro</h2>
      <p>Fica eleito o foro da comarca de São Paulo/SP para dirimir quaisquer questões.</p>
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
