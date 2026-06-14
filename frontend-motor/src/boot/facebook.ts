import { defineBoot } from '#q-app/wrappers'

declare global {
  interface Window {
    FB?: {
      init: (params: { appId: string; cookie?: boolean; xfbml?: boolean; version: string }) => void
      login: (cb: (response: { authResponse?: { accessToken?: string }; status?: string }) => void, params?: { scope?: string }) => void
      getLoginStatus?: (cb: (response: { status: string; authResponse?: { accessToken?: string } }) => void) => void
    }
    fbAsyncInit?: () => void
  }
}

const FB_APP_ID = (import.meta.env as Record<string, string | undefined> | undefined)?.VITE_FACEBOOK_APP_ID

/**
 * Carrega o SDK do Facebook de forma lazy. Só inicializa se
 * VITE_FACEBOOK_APP_ID estiver definida. Se não estiver, o botão
 * "Continuar com Facebook" exibe uma mensagem amigável.
 */
export default defineBoot(() => {
  if (!FB_APP_ID) {
    // SDK não configurado: app funciona sem o login Facebook
    return
  }

  if (typeof window === 'undefined') return

  // Se já existe, não faz nada
  if (window.FB) return

  window.fbAsyncInit = () => {
    if (!window.FB) return
    window.FB.init({
      appId: FB_APP_ID,
      cookie: true,
      xfbml: true,
      version: 'v19.0',
    })
  }

  const script = document.createElement('script')
  script.id = 'facebook-jssdk'
  script.src = 'https://connect.facebook.net/pt_BR/sdk.js'
  script.async = true
  script.defer = true
  script.onerror = () => {
    console.warn('[fb-sdk] não foi possível carregar')
  }
  document.head.appendChild(script)

})
