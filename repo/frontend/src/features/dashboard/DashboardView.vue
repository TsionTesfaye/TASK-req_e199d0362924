<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Dashboard</h1>
        <p class="page-subtitle">System overview · {{ today }}</p>
      </div>
    </div>

    <div class="dash-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-label mono">{{ stat.label }}</div>
        <div class="stat-value" :class="stat.color ? `text-${stat.color}` : ''">
          <span v-if="loadingStats" class="dash-spinner" />
          <span v-else>{{ stat.value ?? '—' }}</span>
        </div>
        <div class="stat-sub mono">{{ stat.sub }}</div>
      </div>
    </div>

    <div class="dash-row">
      <div class="card dash-col">
        <div class="card-label mono">Recent Bulletins</div>
        <LoadingState v-if="loadingBulletins" label="Loading bulletins..." />
        <ErrorState v-else-if="bulletinError" :message="bulletinError" />
        <div v-else-if="bulletins.length === 0" class="text-muted mono" style="font-size:12px;padding:12px 0;">No bulletins.</div>
        <ul v-else class="bulletin-list">
          <li v-for="b in bulletins.slice(0,5)" :key="b.id" class="bulletin-item">
            <span class="bulletin-title">{{ b.title }}</span>
            <span class="mono text-muted" style="font-size:11px;">{{ formatDate(b.createdAt) }}</span>
          </li>
        </ul>
      </div>

      <div class="card dash-col">
        <div class="card-label mono">Today's Appointments</div>
        <LoadingState v-if="loadingAppt" label="Loading..." />
        <ErrorState v-else-if="apptError" :message="apptError" />
        <EmptyState v-else-if="appointments.length === 0" title="No appointments" message="None scheduled today." />
        <ul v-else class="appt-list">
          <li v-for="a in appointments.slice(0,6)" :key="a.id" class="appt-item">
            <span class="appt-time mono">{{ a.time || a.scheduledAt || '—' }}</span>
            <span class="appt-name">{{ a.patientName || a.patient?.displayName || a.id }}</span>
            <span class="badge" :class="statusBadge(a.status)">{{ a.status }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import client, { getMeta } from '../../api/client.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'

const today = new Date().toLocaleDateString('en-US', { weekday:'long', year:'numeric', month:'long', day:'numeric' })
const todayQuery = (() => {
  const d = new Date()
  const mm = String(d.getMonth()+1).padStart(2,'0')
  const dd = String(d.getDate()).padStart(2,'0')
  return `${mm}/${dd}/${d.getFullYear()}`
})()

const loadingStats = ref(true)
const loadingBulletins = ref(true)
const loadingAppt = ref(true)
const bulletinError = ref('')
const apptError = ref('')

const visitCount = ref(null)
const incidentCount = ref(null)
const patientCount = ref(null)

const bulletins = ref([])
const appointments = ref([])

const stats = computed(() => [
  { label: 'Open Visits',    value: visitCount.value,   sub: 'active today',  color: 'amber' },
  { label: 'Incidents',      value: incidentCount.value, sub: 'total records', color: '' },
  { label: 'Patients',       value: patientCount.value,  sub: 'registered',    color: '' },
])

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleDateString()
}

function statusBadge(s) {
  if (!s) return 'badge-gray'
  const map = { scheduled: 'badge-blue', completed: 'badge-green', cancelled: 'badge-red', pending: 'badge-amber' }
  return map[s?.toLowerCase()] || 'badge-gray'
}

async function loadStats() {
  loadingStats.value = true
  try {
    const [visits, incidents, patients] = await Promise.allSettled([
      client.get('/visits?size=1'),
      client.get('/incidents?size=1'),
      client.get('/patients?size=1'),
    ])
    visitCount.value    = visits.status === 'fulfilled'    ? (getMeta(visits.value)?.total ?? '?') : '?'
    incidentCount.value = incidents.status === 'fulfilled' ? (getMeta(incidents.value)?.total ?? '?') : '?'
    patientCount.value  = patients.status === 'fulfilled'  ? (getMeta(patients.value)?.total ?? '?') : '?'
  } finally {
    loadingStats.value = false
  }
}

async function loadBulletins() {
  try {
    const res = await client.get('/bulletins')
    bulletins.value = res || []
  } catch (e) {
    bulletinError.value = e.message
  } finally {
    loadingBulletins.value = false
  }
}

async function loadAppointments() {
  try {
    const res = await client.get(`/appointments?date=${todayQuery}`)
    appointments.value = res || []
  } catch (e) {
    apptError.value = e.message
  } finally {
    loadingAppt.value = false
  }
}

onMounted(() => {
  loadStats()
  loadBulletins()
  loadAppointments()
})
</script>

<style scoped>
.dash-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0; left: 0; right: 0;
  height: 1px;
  background: linear-gradient(90deg, var(--amber), transparent);
}

.stat-label {
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 10px;
}

.stat-value {
  font-family: var(--font-display);
  font-size: 36px;
  font-weight: 800;
  line-height: 1;
  margin-bottom: 6px;
}

.stat-sub {
  font-size: 11px;
  color: var(--text-dim);
}

.dash-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-2);
  border-top-color: var(--amber);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.dash-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.dash-col { flex: 1; }

.card-label {
  font-size: 10px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.bulletin-list, .appt-list {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bulletin-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}

.bulletin-item:last-child { border-bottom: none; padding-bottom: 0; }

.appt-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}

.appt-item:last-child { border-bottom: none; padding-bottom: 0; }

.appt-time {
  font-size: 11px;
  color: var(--amber);
  min-width: 60px;
}

.appt-name { flex: 1; }
</style>
