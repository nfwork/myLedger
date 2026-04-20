import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'

const LoginView = () => import('../views/LoginView.vue')
const RegisterView = () => import('../views/RegisterView.vue')
const MainLayout = () => import('../layouts/MainLayout.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const EntriesView = () => import('../views/EntriesView.vue')
const EntryFormView = () => import('../views/EntryFormView.vue')
const CategoriesView = () => import('../views/CategoriesView.vue')
const StatisticsView = () => import('../views/StatisticsView.vue')
const ProfileView = () => import('../views/ProfileView.vue')
const ChangePasswordView = () => import('../views/ChangePasswordView.vue')
const AccountsView = () => import('../views/AccountsView.vue')

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/register', name: 'register', component: RegisterView, meta: { public: true } },
    {
      path: '/',
      component: MainLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: { name: 'dashboard' } },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '概览' } },
        { path: 'entries', name: 'entries', component: EntriesView, meta: { title: '流水' } },
        {
          path: 'entry/new',
          name: 'entry-new',
          component: EntryFormView,
          meta: { title: '记一笔', hideTab: true, showBack: true },
        },
        {
          path: 'entry/:id/edit',
          name: 'entry-edit',
          component: EntryFormView,
          meta: { title: '编辑流水', hideTab: true, showBack: true },
        },
        { path: 'stats', name: 'stats', component: StatisticsView, meta: { title: '统计' } },
        {
          path: 'categories',
          name: 'categories',
          component: CategoriesView,
          meta: { title: '分类管理', hideTab: true, showBack: true },
        },
        { path: 'profile', name: 'profile', component: ProfileView, meta: { title: '我的' } },
        {
          path: 'password',
          name: 'password',
          component: ChangePasswordView,
          meta: { title: '修改密码', hideTab: true, showBack: true },
        },
        {
          path: 'accounts',
          name: 'accounts',
          component: AccountsView,
          meta: { title: '资金账户', hideTab: true, showBack: true },
        },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  await auth.bootstrap()

  if (to.meta.public && auth.isLoggedIn.value) {
    if (to.name === 'login' || to.name === 'register') {
      return { name: 'dashboard' }
    }
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
