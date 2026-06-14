<template>
  <q-page class="entrar-page flex flex-center">
    <q-card class="entrar-card" flat bordered>
      <q-card-section class="text-center q-pb-none">
        <p class="reg-logo">Motorise</p>
        <p class="reg-title">Entrar na plataforma</p>
        <p class="reg-sub text-grey-6">
          Acesse o painel da sua empresa para gerenciar lojas, anúncios e assinatura.
        </p>
      </q-card-section>

      <q-card-section class="q-pa-lg q-gutter-md">
        <q-form @submit.prevent="onSubmit" autofocus>
          <q-input
            v-model="form.email"
            label="E-mail"
            type="email"
            outlined
            dense
            autocomplete="email"
            class="q-mb-sm"
            lazy-rules
            :rules="[(v) => !!v || 'E-mail é obrigatório']"
          />
          <q-input
            v-model="form.password"
            label="Senha"
            :type="showPass ? 'text' : 'password'"
            outlined
            dense
            autocomplete="current-password"
            class="q-mb-md"
            lazy-rules
            :rules="[(v) => !!v || 'Senha é obrigatória']"
          >
            <template #append>
              <q-icon
                :name="showPass ? 'visibility_off' : 'visibility'"
                class="cursor-pointer"
                @click="showPass = !showPass"
              />
            </template>
          </q-input>

          <q-btn
            type="submit"
            unelevated
            color="primary"
            label="Entrar"
            full-width
            :loading="loading"
            class="q-mt-sm"
          />
        </q-form>

        <q-separator class="q-my-md">
          <q-icon name="more_horiz" />
        </q-separator>

        <q-btn
          outline
          full-width
          icon="img:https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg"
          label="Continuar com Google"
          color="grey-9"
          :loading="loadingGoogle"
          @click="onGoogle"
        />
        <q-btn
          outline
          full-width
          icon="img:https://static.xx.fbcdn.net/rsrc.php/yv/r/h8Eun16b4M5.svg"
          label="Continuar com Facebook"
          color="primary"
          class="q-mt-sm"
          :loading="loadingFacebook"
          @click="onFacebook"
        />

        <div class="q-mt-md text-center text-caption text-grey-6">
          Não tem conta?
          <router-link to="/registro" class="text-primary">Criar conta</router-link>
        </div>

      </q-card-section>

      <q-card-actions class="justify-center q-pb-lg q-pt-none">
        <span class="text-grey-6 text-caption">
          Esqueceu sua senha? Fale com o administrador da sua empresa.
        </span>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useQuasar } from 'quasar'
import { useAuthStore } from 'src/stores/authStore'

const $q = useQuasar()
const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const form = reactive({ email: '', password: '' })
const showPass = ref(false)
const loading = ref(false)
const loadingGoogle = ref(false)
const loadingFacebook = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    await auth.login(form.email, form.password)
    $q.notify({ message: `Bem-vindo, ${auth.user.value?.name ?? ''}`, color: 'positive', position: 'top' })
    const redirect = (route.query.redirect as string) || '/app'
    void router.push(redirect)
  } catch (e: unknown) {
    const status = (e as { response?: { status?: number } })?.response?.status
    const msg =
      status === 401
        ? 'E-mail ou senha inválidos.'
        : status === 403
          ? 'Conta inativa. Fale com o administrador.'
          : 'Não foi possível entrar. Tente novamente.'
    $q.notify({ message: msg, color: 'negative', position: 'top' })
  } finally {
    loading.value = false
  }
}

async function onGoogle() {
  loadingGoogle.value = true
  try {
    await auth.loginWithGoogle()
  } catch {
    loadingGoogle.value = false
  }
}

async function onFacebook() {
  loadingFacebook.value = true
  try {
    await auth.loginWithFacebook()
    const redirect = (route.query.redirect as string) || '/'
    void router.push(redirect)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : 'Falha no login com Facebook.'
    $q.notify({ message: msg, color: 'negative', position: 'top' })
  } finally {
    loadingFacebook.value = false
  }
}
</script>

<style scoped lang="scss">
.entrar-card {
  width: 100%;
  max-width: 440px;
  border-radius: 24px;
}

.reg-logo {
  font-size: 1.8rem;
  font-weight: 900;
  background: var(--gradient-primary);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.reg-title {
  font-size: 1.4rem;
  font-weight: 700;
  margin: 8px 0 0;
}

.reg-sub {
  font-size: 0.85rem;
  margin: 6px 0 0;
}
</style>
