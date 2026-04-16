<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Exports</h1>
        <p class="page-subtitle">Data exports &amp; downloads</p>
      </div>
    </div>

    <div class="layout">
      <!-- Create export form -->
      <div class="card create-card">
        <div class="section-label mono">New Export</div>

        <div v-if="createError" class="form-error">{{ createError }}</div>

        <div class="field">
          <label>Export Type</label>
          <select v-model="form.type" @change="estimateCount">
            <option value="">Select type...</option>
            <option value="ledger">Ledger</option>
            <option value="audit">Audit Log</option>
            <option value="patients">Patients</option>
          </select>
        </div>

        <div v-if="estimated !== null" class="estimate-banner" :class="estimated > 500 ? 'warn' : 'ok'">
          <span class="mono">~{{ estimated }} rows estimated</span>
          <span v-if="estimated > 500" class="warn-text">⚠ Large export requires elevated access</span>
        </div>

        <template v-if="estimated !== null && estimated > 500">
          <div class="field checkbox-field" style="margin-top:14px;">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.elevated" />
              I have elevated authorization for this export
            </label>
          </div>
          <div class="field checkbox-field" style="margin-top:8px;">
            <label class="checkbox-label">
              <input type="checkbox" v-model="form.secondConfirmation" />
              I confirm this export is required and compliant
            </label>
          </div>
        </template>

        <button
          class="btn btn-primary"
          style="margin-top:18px;"
          @click="confirmExport"
          :disabled="!form.type || creating || (estimated !== null && estimated > 500 && (!form.elevated || !form.secondConfirmation))"
        >
          <span v-if="creating" class="spinner-sm" /> Create Export
        </button>
      </div>

      <!-- Exports list -->
      <div class="list-area">
        <div class="card">
          <div class="section-label mono">Export History</div>
          <DataTable
            :columns="columns"
            :rows="exports"
            :loading="loading"
            :error="error"
            :on-retry="load"
            empty-title="No exports"
            empty-message="Create your first export using the form."
          >
            <template #status="{ row }">
              <span class="badge" :class="row.status === 'READY' ? 'badge-green' : row.status === 'FAILED' ? 'badge-red' : 'badge-amber'">{{ row.status }}</span>
            </template>
            <template #elevated="{ row }">
              <span v-if="row.elevated" class="badge badge-red">ELEVATED</span>
              <span v-else class="text-dim mono">—</span>
            </template>
            <template #createdAt="{ row }">
              <span class="mono">{{ formatDate(row.createdAt) }}</span>
            </template>
          </DataTable>
        </div>
      </div>
    </div>

    <ConfirmDialog
      v-model="showConfirm"
      title="Create Export"
      :message="`Create a ${form.type} export${estimated > 500 ? ' (elevated)' : ''}? This may take a moment.`"
      confirm-label="Create Export"
      :loading="creating"
      @confirm="doCreate"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client, { uuid, getMeta } from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import DataTable from '../../components/DataTable.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const toast = useToastStore()
const exports_ = ref([])
const loading = ref(false)
const error = ref('')
const creating = ref(false)
const createError = ref('')
const showConfirm = ref(false)
const estimated = ref(null)

const form = ref({ type: '', elevated: false, secondConfirmation: false })

const columns = [
  { key: 'id',        label: 'ID',     format: v => String(v).slice(0,8) + '…' },
  { key: 'type',      label: 'Type'    },
  { key: 'status',    label: 'Status'  },
  { key: 'elevated',  label: 'Elevated'},
  { key: 'createdAt', label: 'Created' },
]

// expose as "exports" since exports is a reserved word in modules
const exports = exports_

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

async function estimateCount() {
  if (!form.value.type) { estimated.value = null; return }
  // Only the three backend-supported types are reachable here.
  // We estimate row counts from the closest list endpoint that ADMIN/BILLING can see.
  const endpoints = {
    ledger: '/invoices',   // ledger entries scale with invoice activity
    audit: '/audit',
    patients: '/patients',
  }
  const ep = endpoints[form.value.type]
  if (!ep) { estimated.value = null; return }
  try {
    const res = await client.get(`${ep}?page=0&size=1`)
    estimated.value = getMeta(res)?.total ?? 0
  } catch (_) {
    estimated.value = null
  }
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/exports/history?page=0&size=20')
    exports_.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function confirmExport() {
  showConfirm.value = true
}

async function doCreate() {
  creating.value = true
  createError.value = ''
  const idempotencyKey = uuid()
  try {
    const result = await client.post('/exports', {
      exportType: form.value.type,
      idempotencyKey,
      elevated: form.value.elevated,
      secondConfirmation: form.value.secondConfirmation,
    }, { headers: { 'Idempotency-Key': idempotencyKey } })
    // Backend returns a snapshot string describing the export. Track it locally.
    exports_.value = [{
      id: idempotencyKey,
      type: form.value.type,
      status: 'CREATED',
      elevated: form.value.elevated,
      createdAt: new Date().toISOString(),
      result: typeof result?.result === 'string' ? result.result : JSON.stringify(result),
    }, ...exports_.value]
    toast.success('Export created.')
    showConfirm.value = false
    form.value = { type: '', elevated: false, secondConfirmation: false }
    estimated.value = null
  } catch (e) {
    createError.value = e.message
    showConfirm.value = false
  } finally {
    creating.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.layout { display: grid; grid-template-columns: 320px 1fr; gap: 16px; align-items: start; }
.create-card { display: flex; flex-direction: column; gap: 12px; }
.section-label { font-size:10px; letter-spacing:0.1em; text-transform:uppercase; color:var(--text-muted); }
.session-tag { font-size:10px; letter-spacing:0; text-transform:none; color:var(--text-dim); font-style:italic; }
.estimate-banner {
  border-radius: var(--radius); padding: 10px 14px;
  font-family: var(--font-mono); font-size: 12px;
  display: flex; flex-direction: column; gap: 4px;
}
.estimate-banner.ok { background: var(--green-dim); border: 1px solid var(--green); color: var(--green); }
.estimate-banner.warn { background: var(--red-dim); border: 1px solid var(--amber); color: var(--amber); }
.warn-text { font-size: 11px; color: var(--red); }
.checkbox-field { display: flex; align-items: center; }
.checkbox-label {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--font-mono); font-size: 12px; color: var(--text);
  cursor: pointer; text-transform: none; letter-spacing: normal;
}
.checkbox-label input[type=checkbox] { width: auto; }
.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red);
}
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
