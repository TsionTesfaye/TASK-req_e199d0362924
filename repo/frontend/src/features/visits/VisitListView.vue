<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Visits</h1>
        <p class="page-subtitle">Patient encounter records</p>
      </div>
      <button class="btn btn-primary" @click="showOpenVisit = true">+ Open Visit</button>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="visits"
        :loading="loading"
        :error="error"
        :meta="meta"
        :page="page"
        empty-title="No visits"
        empty-message="Open a new visit using the button above."
        :on-retry="() => load()"
        @page="load"
      >
        <template #status="{ row }">
          <span class="badge" :class="statusBadge(row.status)">{{ row.status }}</span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
        <template #actions="{ row }">
          <button
            v-if="row.status === 'OPEN'"
            class="btn btn-secondary btn-sm"
            @click="initiateClose(row)"
            :disabled="closingId === row.id"
          >
            <span v-if="closingId === row.id" class="spinner-sm" />
            Close Visit
          </button>
          <span v-else class="text-muted mono" style="font-size:11px;">closed</span>
        </template>
      </DataTable>
    </div>

    <!-- Open Visit dialog -->
    <Teleport to="body">
      <div v-if="showOpenVisit" class="dialog-backdrop" @click.self="showOpenVisit = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Open Visit</span>
            <button class="btn btn-ghost btn-sm" @click="showOpenVisit = false">✕</button>
          </div>
          <form @submit.prevent="openVisit" class="dialog-body">
            <div v-if="openError" class="form-error">{{ openError }}</div>
            <div class="field">
              <label>Patient ID *</label>
              <input v-model="openForm.patientId" required placeholder="patient-uuid" />
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Notes</label>
              <textarea v-model="openForm.chiefComplaint" placeholder="Optional notes..." rows="3" />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showOpenVisit = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="opening">
                <span v-if="opening" class="spinner-sm" /> Open Visit
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- QC block dialog -->
    <Teleport to="body">
      <div v-if="qcBlock.show" class="dialog-backdrop" @click.self="qcBlock.show = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title text-amber">Visit Close Blocked</span>
            <button class="btn btn-ghost btn-sm" @click="qcBlock.show = false">✕</button>
          </div>
          <div class="dialog-body">
            <p class="text-muted" style="font-size:13px;margin-bottom:12px;">
              This visit cannot be closed — quality rules are blocking it:
            </p>
            <ul class="qc-list">
              <li v-for="r in qcBlock.results" :key="r.id" class="qc-item">
                <span class="badge badge-red">BLOCKED</span>
                <span>{{ r.ruleName || r.message || r.id }}</span>
              </li>
            </ul>
            <p class="mono" style="font-size:11px;color:var(--text-dim);margin-top:12px;">
              Go to <router-link to="/quality">Quality → Results</router-link> to request an override.
            </p>
          </div>
          <div class="dialog-footer">
            <button class="btn btn-secondary" @click="qcBlock.show = false">Close</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- Close Visit confirm -->
    <ConfirmDialog
      v-model="closeConfirm.show"
      title="Close Visit"
      :message="`Close visit for patient ${closeConfirm.visit?.patientId || closeConfirm.visit?.id}? This will generate an invoice.`"
      confirm-label="Close & Invoice"
      :loading="closeConfirm.loading"
      @confirm="doClose"
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

const visits = ref([])
const loading = ref(false)
const error = ref('')
const meta = ref(null)
const page = ref(1)
const closingId = ref(null)

const showOpenVisit = ref(false)
const opening = ref(false)
const openError = ref('')
const openForm = ref({ patientId: '', chiefComplaint: '' })

const closeConfirm = ref({ show: false, visit: null, loading: false })
const qcBlock = ref({ show: false, results: [] })

const columns = [
  { key: 'id',        label: 'Visit ID',   format: v => v },
  { key: 'patientId', label: 'Patient ID'  },
  { key: 'status',    label: 'Status'      },
  { key: 'createdAt', label: 'Opened'      },
  { key: 'actions',   label: ''            },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

function statusBadge(s) {
  const map = { OPEN: 'badge-green', CLOSED: 'badge-gray', VOIDED: 'badge-red' }
  return map[s] || 'badge-gray'
}

async function load(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/visits?page=${p}&size=20`)
    visits.value = res || []
    meta.value = getMeta(res) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function openVisit() {
  opening.value = true
  openError.value = ''
  try {
    await client.post('/visits', openForm.value, { headers: { 'Idempotency-Key': uuid() } })
    toast.success('Visit opened.')
    showOpenVisit.value = false
    openForm.value = { patientId: '', chiefComplaint: '' }
    load(1)
  } catch (e) {
    openError.value = e.message
  } finally {
    opening.value = false
  }
}

function initiateClose(visit) {
  closeConfirm.value = { show: true, visit, loading: false }
}

async function doClose() {
  if (closeConfirm.value.loading) return // guard against double-click
  closeConfirm.value.loading = true
  const idempotencyKey = uuid()
  try {
    const res = await client.post(`/visits/${closeConfirm.value.visit.id}/close`, {}, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    toast.success('Visit closed. Invoice generated.')
    closeConfirm.value.show = false
    load(page.value)
  } catch (e) {
    closeConfirm.value.show = false
    if (e.status === 422) {
      // QC blocked
      qcBlock.value = { show: true, results: e.details || [] }
    } else {
      toast.error(e.message)
    }
  } finally {
    closeConfirm.value.loading = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.dialog-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 9000; backdrop-filter: blur(2px);
}
.dialog-box {
  background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--radius-lg);
  width: 100%; max-width: 460px; box-shadow: 0 20px 60px rgba(0,0,0,0.6);
}
.dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 14px; border-bottom: 1px solid var(--border);
}
.dialog-title { font-family: var(--font-display); font-weight: 700; font-size: 15px; }
.dialog-body { padding: 20px; }
.dialog-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px; border-top: 1px solid var(--border);
}
.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-bottom: 16px;
}
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
.qc-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.qc-item { display: flex; align-items: center; gap: 10px; font-size: 13px; }
</style>
