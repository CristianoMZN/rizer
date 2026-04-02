<template>
  <q-page class="registro-page flex flex-center">
    <q-card class="registro-card" flat bordered>
      <q-card-section class="text-center q-pb-none">
        <p class="reg-logo">Motorise</p>
        <p class="reg-title">{{ isLogin ? 'Entrar' : 'Criar conta' }}</p>
      </q-card-section>

      <q-card-section class="q-pa-lg q-gutter-md">
        <template v-if="!isLogin">
          <q-input v-model="form.name" label="Nome completo" outlined dense />
        </template>
        <q-input v-model="form.email" label="E-mail" outlined dense type="email" />
        <q-input
          v-model="form.password"
          label="Senha"
          outlined
          dense
          :type="showPass ? 'text' : 'password'"
        >
          <template #append>
            <q-icon :name="showPass ? 'visibility_off' : 'visibility'" class="cursor-pointer" @click="showPass = !showPass" />
          </template>
        </q-input>

        <q-btn
          unelevated
          color="primary"
          :label="isLogin ? 'Entrar' : 'Criar conta'"
          full-width
          class="q-mt-md"
          @click="submit"
        />

        <q-separator>
          <q-icon name="or" />
        </q-separator>

        <q-btn outline full-width icon="google" label="Continuar com Google" color="grey-7" />
      </q-card-section>

      <q-card-actions class="justify-center q-pb-lg">
        <span class="text-grey-6">{{ isLogin ? 'Não tem conta?' : 'Já tem conta?' }}</span>
        <q-btn flat color="primary" :label="isLogin ? 'Cadastre-se' : 'Entrar'" @click="isLogin = !isLogin" />
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useQuasar } from 'quasar'

const router = useRouter()
const $q = useQuasar()

const isLogin = ref(true)
const showPass = ref(false)
const form = reactive({ name: '', email: '', password: '' })

function submit() {
  $q.notify({ message: isLogin.value ? 'Login realizado!' : 'Conta criada!', color: 'positive', position: 'top' })
  void router.push('/')
}
</script>

<style scoped lang="scss">
.registro-card {
  width: 100%;
  max-width: 420px;
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
</style>
