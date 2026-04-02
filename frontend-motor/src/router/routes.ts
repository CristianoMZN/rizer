import type { RouteRecordRaw } from 'vue-router';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    children: [
      // ─── Public ─────────────────────────────────────────────────────
      { path: '', component: () => import('pages/HomePage.vue') },
      { path: 'produtos', component: () => import('pages/ProdutosPage.vue') },
      { path: 'produto/:id', component: () => import('pages/ProdutoPage.vue') },
      { path: 'comparar', component: () => import('pages/CompararPage.vue') },
      { path: 'lojas-parceiras', component: () => import('pages/LojasParceirasPage.vue') },
      { path: 'seja-parceiro', component: () => import('pages/SejaParceiroPage.vue') },
      { path: 'registro', component: () => import('pages/RegistroPage.vue') },

      // ─── Store-themed ────────────────────────────────────────────────
      { path: 'lojas/:slug', component: () => import('pages/LojaPage.vue') },
      { path: 'lojas/:slug/produtos', component: () => import('pages/LojaPage.vue') },

      // ─── Authenticated ───────────────────────────────────────────────
      { path: 'favoritos', component: () => import('pages/FavoritosPage.vue') },
      { path: 'leads', component: () => import('pages/LeadsPage.vue') },
      { path: 'anuncios', component: () => import('pages/AnunciosPage.vue') },
    ],
  },

  // Always leave this as last one
  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
