<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Sampling</h1>
        <p class="page-subtitle">Statistical visit sampling runs</p>
      </div>
      <button class="btn btn-primary" @click="startRun" :disabled="starting">
        <span v-if="starting" class="spinner-sm" /> Start Run
      </button>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="runs"
        :loading="loading"
        :error="error"
        :on-retry="load"
        empty-title="No runs"
        empty-message="Start a sampling run using the button above."
        @row-click="selectRun"
      >
        <template #status="{ row }">
          <span class="badge" :class="row.status === 'COMPLETE' ? 'badge-green' : 'badge-amber'">{{ row.status || '—' }}</span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
        <template #sampleSize="{ row }">
          <span class="mono">{{ row.sampleSize ?? row.visits?.length ?? '—' }}</span>
        </template>
      </DataTable>
    </div>

    <!-- Run detail drawer -->
    <Teleport to="body">
      <div v-if="selectedRun" class="dialog-backdrop" @click.self="selectedRun = null">
        <div class="dialog-box fade-in" style="max-width:600px;">
          <div class="dialog-header">
            <span class="dialog-title">Sampling Run <span class="mono text-amber">{{ String(selectedRun.id).slice(0,8) }}</span></span>
            <button class="btn btn-ghost btn-sm" @click="selectedRun = null">✕</button>
          </div>
          <div class="dialog-body">
            <div class="section-label mono" style="margin-bottom:12px;">Sampled Visits</div>
            <div v-if="loadingVisits" class="text-muted mono" style="font-size:12px;">Loading visits…</div>
            <div v-else-if="visitsError" class="text-muted mono" style="font-size:12px;color:var(--red);">{{ visitsError }}</div>
            <div v-else-if="!selectedVisits.length" class="text-muted mono" style="font-size:12px;">No visits in this sample.</div>
            <ul v-else class="sample-list">
              <li v-for="v in selectedVisits" :key="v.id" class="sample-item">
                <span class="mono text-amber">{{ String(v.id).slice(0,8) }}</span>
                <span>{{ v.patientId ?? '—' }}</span>
                <span class="badge" :class="v.status === 'CLOSED' ? 'badge-gray' : 'badge-green'">{{ v.status }}</span>
              </li>
            </ul>
          </div>
          <div class="dialog-footer">
            <button class="btn btn-secondary" @click="selectedRun = null">Close</button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import DataTable from '../../components/DataTable.vue'

const toast = useToastStore()
const runs = ref([])
const loading = ref(false)
const error = ref('')
const starting = ref(false)
const selectedRun = ref(null)
const selectedVisits = ref([])
const loadingVisits = ref(false)
const visitsError = ref('')

const columns = [
  { key: 'id',         label: 'Run ID',      format: v => String(v).slice(0,8) + '…' },
  { key: 'status',     label: 'Status'       },
  { key: 'sampleSize', label: 'Sample Size'  },
  { key: 'createdAt',  label: 'Started'      },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/sampling/runs')
    runs.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function currentPeriod() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}

async function startRun() {
  starting.value = true
  try {
    await client.post('/sampling/runs', { period: currentPeriod(), percentage: 5 })
    toast.success('Sampling run started.')
    load()
  } catch (e) {
    toast.error(e.message)
  } finally {
    starting.value = false
  }
}

async function selectRun(row) {
  selectedRun.value = row
  selectedVisits.value = []
  visitsError.value = ''
  loadingVisits.value = true
  try {
    const res = await client.get(`/sampling/runs/${row.id}/visits`)
    selectedVisits.value = res || []
  } catch (e) {
    visitsError.value = e.message
  } finally {
    loadingVisits.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.section-label { font-size:10px; letter-spacing:0.1em; text-transform:uppercase; color:var(--text-muted); }
.sample-list { list-style:none; display:flex; flex-direction:column; gap:8px; }
.sample-item { display:flex; gap:12px; align-items:center; font-size:13px; padding-bottom:8px; border-bottom:1px solid var(--border); }
.sample-item:last-child { border-bottom:none; padding-bottom:0; }
.dialog-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 9000; backdrop-filter: blur(2px);
}
.dialog-box {
  background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--radius-lg);
  width: 100%; max-width: 480px; box-shadow: 0 20px 60px rgba(0,0,0,0.6);
}
.dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 14px; border-bottom: 1px solid var(--border);
}
.dialog-title { font-family: var(--font-display); font-weight: 700; font-size: 15px; }
.dialog-body { padding: 20px; }
.dialog-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px; border-top: 1px solid var(--border); }
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
