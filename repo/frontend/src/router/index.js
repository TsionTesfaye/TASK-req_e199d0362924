import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session.js'

const routes = [
  {
    path: '/bootstrap',
    name: 'Bootstrap',
    component: () => import('../features/auth/BootstrapView.vue'),
    meta: { public: true },
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../features/auth/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../features/dashboard/DashboardView.vue'),
  },
  {
    path: '/patients',
    name: 'Patients',
    component: () => import('../features/patients/PatientListView.vue'),
    meta: { roles: ['FRONT_DESK', 'CLINICIAN', 'ADMIN'] },
  },
  {
    path: '/patients/:id',
    name: 'PatientDetail',
    component: () => import('../features/patients/PatientDetailView.vue'),
    meta: { roles: ['FRONT_DESK', 'CLINICIAN', 'ADMIN'] },
  },
  {
    path: '/visits',
    name: 'Visits',
    component: () => import('../features/visits/VisitListView.vue'),
  },
  {
    path: '/appointments',
    name: 'Appointments',
    component: () => import('../features/appointments/AppointmentView.vue'),
  },
  {
    path: '/incidents',
    name: 'Incidents',
    component: () => import('../features/incidents/IncidentListView.vue'),
  },
  {
    path: '/incidents/:id',
    name: 'IncidentDetail',
    component: () => import('../features/incidents/IncidentDetailView.vue'),
  },
  {
    path: '/search',
    name: 'Search',
    component: () => import('../features/search/SearchView.vue'),
  },
  {
    path: '/bulletins',
    name: 'Bulletins',
    component: () => import('../features/bulletins/BulletinView.vue'),
  },
  {
    path: '/billing',
    name: 'Billing',
    component: () => import('../features/billing/BillingListView.vue'),
    meta: { roles: ['BILLING', 'ADMIN'] },
  },
  {
    path: '/billing/:id',
    name: 'InvoiceDetail',
    component: () => import('../features/billing/InvoiceDetailView.vue'),
    meta: { roles: ['BILLING', 'ADMIN'] },
  },
  {
    path: '/daily-close',
    name: 'DailyClose',
    component: () => import('../features/billing/DailyCloseView.vue'),
    meta: { roles: ['BILLING', 'ADMIN'] },
  },
  {
    path: '/quality',
    name: 'Quality',
    component: () => import('../features/quality/QualityView.vue'),
    meta: { roles: ['QUALITY', 'ADMIN'] },
  },
  {
    path: '/sampling',
    name: 'Sampling',
    component: () => import('../features/quality/SamplingView.vue'),
    meta: { roles: ['QUALITY', 'ADMIN'] },
  },
  {
    path: '/exports',
    name: 'Exports',
    component: () => import('../features/exports/ExportView.vue'),
    meta: { roles: ['BILLING', 'ADMIN'] },
  },
  {
    path: '/backups',
    name: 'Backups',
    component: () => import('../features/backups/BackupView.vue'),
    meta: { roles: ['ADMIN'] },
  },
  {
    path: '/ranking',
    name: 'Ranking',
    component: () => import('../features/ranking/RankingView.vue'),
    meta: { roles: ['MODERATOR', 'ADMIN'] },
  },
  {
    path: '/audit',
    name: 'Audit',
    component: () => import('../features/audit/AuditView.vue'),
    meta: { roles: ['ADMIN'] },
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../features/admin/AdminUsersView.vue'),
    meta: { roles: ['ADMIN'] },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

import apiClient from '../api/client.js'

let bootstrapChecked = false
let systemInitialized = false

async function checkBootstrapStatus() {
  if (bootstrapChecked) return systemInitialized
  try {
    const payload = await apiClient.get('/bootstrap/status')
    systemInitialized = !!payload.initialized
  } catch {
    systemInitialized = true // fail open — if status endpoint errors, fall through to normal auth
  }
  bootstrapChecked = true
  return systemInitialized
}

router.beforeEach(async (to) => {
  const session = useSessionStore()
  const initialized = await checkBootstrapStatus()

  if (!initialized) {
    if (to.name !== 'Bootstrap') return { name: 'Bootstrap' }
    return
  }

  if (to.name === 'Bootstrap') return { name: 'Login' }

  if (!to.meta.public && !session.token) {
    return { name: 'Login' }
  }

  if (session.token && !session.user) {
    await session.loadMe()
  }

  if (to.meta.roles && session.user) {
    if (!to.meta.roles.includes(session.user.role)) {
      return { path: '/' }
    }
  }
})

router.afterEach(() => {
  // allow re-check on next navigation if bootstrap just completed
})

export function invalidateBootstrapCache() {
  bootstrapChecked = false
}

export default router
