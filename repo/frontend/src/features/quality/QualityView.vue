<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Quality Assurance</h1>
        <p class="page-subtitle">QA results &amp; corrective actions</p>
      </div>
    </div>

    <!-- QA Results section -->
    <div class="section-header">
      <span class="section-title">Blocking QA Results</span>
    </div>

    <div class="card">
      <DataTable
        :columns="resultCols"
        :rows="results"
        :loading="loadingResults"
        :error="resultsError"
        :on-retry="loadResults"
        empty-title="No blocking results"
        empty-message="All quality checks are passing."
      >
        <template #status="{ row }">
          <span class="badge" :class="row.status === 'OPEN' ? 'badge-amber' : 'badge-gray'">
            {{ row.status }}
          </span>
        </template>
        <template #actions="{ row }">
          <button v-if="row.severity === 'BLOCKING' && row.status !== 'OVERRIDDEN'" class="btn btn-secondary btn-sm" @click="openOverride(row)">
            Override
          </button>
          <span v-else-if="row.status === 'OVERRIDDEN'" class="badge badge-gray">Overridden</span>
        </template>
      </DataTable>
    </div>

    <!-- Corrective Actions section -->
    <div class="section-header" style="margin-top:28px;">
      <span class="section-title">Corrective Actions</span>
      <button class="btn btn-primary btn-sm" @click="showCreateCA = true">+ New</button>
    </div>

    <div class="card">
      <DataTable
        :columns="caCols"
        :rows="correctiveActions"
        :loading="loadingCA"
        :error="caError"
        :on-retry="loadCA"
        empty-title="No corrective actions"
        empty-message="No corrective actions recorded."
      >
        <template #status="{ row }">
          <select
            :value="row.status"
            @change="updateCAState(row, $event.target.value)"
            class="state-select"
          >
            <option value="OPEN">Open</option>
            <option value="ASSIGNED">Assigned</option>
            <option value="IN_PROGRESS">In Progress</option>
            <option value="RESOLVED">Resolved</option>
            <option value="VERIFIED_CLOSED">Verified Closed</option>
          </select>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
      </DataTable>
    </div>

    <!-- Override dialog -->
    <Teleport to="body">
      <div v-if="overrideDialog.show" class="dialog-backdrop" @click.self="overrideDialog.show = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Request Override</span>
            <button class="btn btn-ghost btn-sm" @click="overrideDialog.show = false">✕</button>
          </div>
          <form @submit.prevent="submitOverride" class="dialog-body">
            <div v-if="overrideError" class="form-error">{{ overrideError }}</div>
            <p class="text-muted" style="font-size:13px;margin-bottom:14px;">
              Override: <strong>{{ overrideDialog.result?.ruleCode }}</strong>
            </p>
            <div class="field">
              <label>Reason *</label>
              <input v-model="overrideForm.reasonCode" required placeholder="Reason for override" />
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Clinical Note *</label>
              <textarea v-model="overrideForm.note" required rows="3" placeholder="Supporting clinical note..." />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="overrideDialog.show = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="overriding">
                <span v-if="overriding" class="spinner-sm" /> Submit Override
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Create CA dialog -->
    <Teleport to="body">
      <div v-if="showCreateCA" class="dialog-backdrop" @click.self="showCreateCA = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">New Corrective Action</span>
            <button class="btn btn-ghost btn-sm" @click="showCreateCA = false">✕</button>
          </div>
          <form @submit.prevent="createCA" class="dialog-body">
            <div v-if="caCreateError" class="form-error">{{ caCreateError }}</div>
            <div class="field">
              <label>Description *</label>
              <textarea v-model="caForm.description" required rows="3" placeholder="Describe the corrective action..." />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showCreateCA = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="creatingCA">
                <span v-if="creatingCA" class="spinner-sm" /> Create
              </button>
            </div>
          </form>
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

const results = ref([])
const loadingResults = ref(false)
const resultsError = ref('')

const correctiveActions = ref([])
const loadingCA = ref(false)
const caError = ref('')

const overrideDialog = ref({ show: false, result: null })
const overrideForm = ref({ reasonCode: '', note: '' })
const overriding = ref(false)
const overrideError = ref('')

const showCreateCA = ref(false)
const creatingCA = ref(false)
const caCreateError = ref('')
const caForm = ref({ description: '' })

const resultCols = [
  { key: 'ruleCode',  label: 'Rule',    format: v => v || '—' },
  { key: 'visitId',   label: 'Visit ID', format: v => v != null ? String(v).slice(0,8) + '…' : '—' },
  { key: 'status',    label: 'Status'   },
  { key: 'actions',   label: ''         },
]

const caCols = [
  { key: 'description', label: 'Description' },
  { key: 'status',      label: 'Status'      },
  { key: 'createdAt',   label: 'Created'     },
]

function formatDate(d) { return d ? new Date(d).toLocaleDateString() : '—' }

async function loadResults() {
  loadingResults.value = true
  resultsError.value = ''
  try {
    const res = await client.get('/quality/results')
    results.value = res || []
  } catch (e) {
    resultsError.value = e.message
  } finally {
    loadingResults.value = false
  }
}

async function loadCA() {
  loadingCA.value = true
  caError.value = ''
  try {
    const res = await client.get('/quality/corrective-actions')
    correctiveActions.value = res || []
  } catch (e) {
    caError.value = e.message
  } finally {
    loadingCA.value = false
  }
}

function openOverride(result) {
  overrideDialog.value = { show: true, result }
  overrideForm.value = { reasonCode: '', note: '' }
}

async function submitOverride() {
  overriding.value = true
  overrideError.value = ''
  try {
    await client.post(`/quality/results/${overrideDialog.value.result.id}/override`, overrideForm.value)
    toast.success('Override submitted.')
    overrideDialog.value.show = false
    loadResults()
  } catch (e) {
    overrideError.value = e.message
  } finally {
    overriding.value = false
  }
}

async function updateCAState(ca, newState) {
  try {
    // Backend TransitionCARequest: { status (required), resolutionNote (optional), assignedTo (optional) }
    await client.put(`/quality/corrective-actions/${ca.id}`, { status: newState })
    ca.status = newState
    toast.success('Status updated.')
  } catch (e) {
    toast.error(e.message)
  }
}

async function createCA() {
  creatingCA.value = true
  caCreateError.value = ''
  try {
    // Backend CreateCARequest: { description (required), relatedVisitId (optional), relatedRuleResultId (optional) }
    await client.post('/quality/corrective-actions', { description: caForm.value.description })
    toast.success('Corrective action created.')
    showCreateCA.value = false
    caForm.value = { description: '' }
    loadCA()
  } catch (e) {
    caCreateError.value = e.message
  } finally {
    creatingCA.value = false
  }
}

onMounted(() => {
  loadResults()
  loadCA()
})
</script>

<style scoped>
.section-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 12px; gap: 12px;
}
.section-title {
  font-family: var(--font-display); font-weight: 700; font-size: 15px;
}
.state-select {
  background: var(--surface-2); border: 1px solid var(--border-2); border-radius: var(--radius);
  color: var(--text); font-family: var(--font-mono); font-size: 11px; padding: 3px 6px; width: auto;
}
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
</style>
