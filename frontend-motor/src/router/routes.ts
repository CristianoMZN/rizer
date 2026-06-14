import type { RouteRecordRaw } from 'vue-router';
import type { SystemRole } from 'src/stores/authStore';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
    requiredRoles?: SystemRole[];
    layout?: 'public' | 'app' | 'admin';
    title?: string;
  }
}

const routes: RouteRecordRaw[] = [
  // ─── Público ─────────────────────────────────────────────────────
  {
    path: '/',
    component: () => import('layouts/MainLayout.vue'),
    meta: { layout: 'public' },
    children: [
      { path: '', component: () => import('pages/HomePage.vue') },
      { path: 'produtos', component: () => import('pages/ProdutosPage.vue') },
      { path: 'produto/:id', component: () => import('pages/ProdutoPage.vue') },
      { path: 'comparar', component: () => import('pages/CompararPage.vue') },
      { path: 'lojas-parceiras', redirect: '/parceiros' },
      { path: 'parceiros', name: 'parceiros', component: () => import('pages/partner/PartnerListPage.vue') },
      { path: 'parceiros/:slug', name: 'parceiro', component: () => import('pages/partner/PartnerPage.vue') },
      { path: 'parceiros/:slug/loja/:storeSlug', name: 'parceiro-loja', component: () => import('pages/partner/PartnerStorePage.vue') },
      { path: 'seja-parceiro', component: () => import('pages/SejaParceiroPage.vue') },
      { path: 'registro', component: () => import('pages/RegistroPage.vue') },
      { path: 'entrar', component: () => import('pages/EntrarPage.vue') },
      { path: 'perfil', name: 'perfil', component: () => import('pages/PerfilPage.vue'), meta: { requiresAuth: true } },
      { path: 'lojas/:slug', component: () => import('pages/LojaPage.vue') },
      { path: 'lojas/:slug/produtos', component: () => import('pages/LojaPage.vue') },
      { path: 'favoritos', component: () => import('pages/FavoritosPage.vue') },
      { path: 'leads', component: () => import('pages/LeadsPage.vue') },
      { path: 'anuncios', component: () => import('pages/AnunciosPage.vue') },

      // Páginas legais
      {
        path: 'legal/termos-de-uso',
        component: () => import('pages/legal/TermosDeUsoPage.vue'),
        props: { version: 'v1.0', updatedAt: '13/06/2026' },
      },
      {
        path: 'legal/politica-de-privacidade',
        component: () => import('pages/legal/PoliticaPrivacidadePage.vue'),
        props: { version: 'v1.0', updatedAt: '13/06/2026' },
      },
      {
        path: 'legal/politica-de-cookies',
        component: () => import('pages/legal/PoliticaCookiesPage.vue'),
        props: { version: 'v1.0', updatedAt: '13/06/2026' },
      },
    ],
  },

  // ─── App autenticado (por tenant) ────────────────────────────────
  {
    path: '/app',
    component: () => import('layouts/AppLayout.vue'),
    meta: { requiresAuth: true, layout: 'app' },
    children: [
      {
        path: '',
        name: 'app-dashboard',
        component: () => import('pages/app/DashboardPage.vue'),
        meta: { title: 'Dashboard' },
      },
      {
        path: 'lojas',
        name: 'app-stores',
        component: () => import('pages/app/StoresPage.vue'),
        meta: { title: 'Lojas' },
      },
      {
        path: 'empresa',
        name: 'app-company',
        component: () => import('pages/app/CompanyPage.vue'),
        meta: { title: 'Minha Empresa' },
      },
      {
        path: 'vendedores',
        name: 'app-sellers',
        component: () => import('pages/app/SellersPage.vue'),
        meta: { title: 'Vendedores' },
      },
      {
        path: 'anuncios',
        name: 'app-products',
        component: () => import('pages/app/ProductsPage.vue'),
        meta: { title: 'Anúncios' },
      },
      {
        path: 'anuncios/novo',
        name: 'app-product-new',
        component: () => import('pages/app/ProductWizardPage.vue'),
        meta: { title: 'Novo anúncio' },
      },
      {
        path: 'anuncios/:id/editar',
        name: 'app-product-edit',
        component: () => import('pages/app/ProductEditPage.vue'),
        meta: { title: 'Editar anúncio' },
      },
      {
        path: 'leads',
        name: 'app-leads',
        component: () => import('pages/app/LeadsPage.vue'),
        meta: { title: 'Leads' },
      },
      {
        path: 'membros',
        name: 'app-members',
        component: () => import('pages/app/MembersPage.vue'),
        meta: { title: 'Membros' },
      },
      {
        path: 'assinatura',
        name: 'app-billing',
        component: () => import('pages/app/BillingPage.vue'),
        meta: { title: 'Assinatura' },
      },
      {
        path: 'integracoes',
        name: 'app-integrations',
        component: () => import('pages/app/IntegrationsPage.vue'),
        meta: { title: 'Integrações' },
      },
      {
        path: 'integracoes/instagram/callback',
        name: 'app-integrations-callback',
        component: () => import('pages/app/IntegrationsPage.vue'),
        meta: { title: 'Integrações · Callback' },
      },
      {
        path: 'configuracoes',
        name: 'app-settings',
        component: () => import('pages/app/SettingsPage.vue'),
        meta: { title: 'Configurações' },
      },
    ],
  },

  // ─── Admin (sys_admin) ───────────────────────────────────────────
  {
    path: '/admin',
    component: () => import('layouts/AdminLayout.vue'),
    meta: {
      requiresAuth: true,
      requiredRoles: ['sys_admin', 'sys_manager', 'sys_employee'],
      layout: 'admin',
    },
    children: [
      {
        path: '',
        name: 'admin-dashboard',
        component: () => import('pages/admin/AdminDashboardPage.vue'),
        meta: { title: 'Admin · Dashboard' },
      },
      {
        path: 'tenants',
        name: 'admin-tenants',
        component: () => import('pages/admin/AdminTenantsPage.vue'),
        meta: { title: 'Admin · Tenants' },
      },
      {
        path: 'users',
        name: 'admin-users',
        component: () => import('pages/admin/AdminUsersPage.vue'),
        meta: { title: 'Admin · Usuários' },
      },
      {
        path: 'plans',
        name: 'admin-plans',
        component: () => import('pages/admin/AdminPlansPage.vue'),
        meta: { title: 'Admin · Planos' },
      },
      {
        path: 'payments',
        name: 'admin-payments',
        component: () => import('pages/admin/AdminPaymentsPage.vue'),
        meta: { title: 'Admin · Pagamentos' },
      },
    ],
  },

  {
    path: '/:catchAll(.*)*',
    component: () => import('pages/ErrorNotFound.vue'),
  },
];

export default routes;
