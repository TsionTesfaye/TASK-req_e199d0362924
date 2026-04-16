<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <button class="btn btn-ghost btn-sm" @click="router.back()" style="margin-bottom:8px;">← Back</button>
        <h1 class="page-title" v-if="patient">
          Patient {{ patient.medicalRecordNumber }}
          <ArchivedBadge v-if="patient.archivedAt" />
        </h1>
        <p class="page-subtitle mono" v-if="patient">ID: {{ patient.id }}</p>
      </div>
      <div v-if="patient" class="flex gap-2">
        <!-- Edit is intentionally hidden — backend does not support PUT /patients/:id -->
        <!-- <button class="btn btn-secondary" @click="showEdit = true">Edit</button> -->
        <button class="btn btn-danger" @click="deleteDialog = true">Archive</button>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="load" />

    <template v-else-if="patient">
      <div class="detail-grid">
        <div class="card">
          <div class="section-label mono">Personal Info</div>
          <dl class="detail-list">
            <div class="detail-row">
              <dt>Date of Birth</dt>
              <dd class="mono">{{ patient.dateOfBirth || '—' }}</dd>
            </div>
            <div class="detail-row">
              <dt>Phone</dt>
              <!-- SensitiveField: masked by default; Reveal button calls /patients/{id}/reveal -->
              <dd><SensitiveField :patientId="patient.id" field="phone" /></dd>
            </div>
            <div class="detail-row">
              <dt>MRN</dt>
              <dd class="mono">{{ patient.medicalRecordNumber || '—' }}</dd>
            </div>
            <div class="detail-row">
              <dt>Protected case</dt>
              <dd class="mono">{{ patient.isProtectedCase || patient.protectedCase ? 'yes' : 'no' }}</dd>
            </div>
            <div class="detail-row">
              <dt>Registered</dt>
              <dd class="mono">{{ formatDate(patient.createdAt) }}</dd>
            </div>
          </dl>
        </div>

        <div class="card">
          <div class="section-label mono">ID Verification</div>
          <form @submit.prevent="verifyId" class="dialog-body" style="padding:0;">
            <div v-if="verifyError" class="form-error">{{ verifyError }}</div>
            <div class="form-grid">
              <div class="field">
                <label>Document type</label>
                <select v-model="verifyForm.documentType" required>
                  <option value="">Select…</option>
                  <option value="DRIVERS_LICENSE">Driver's License</option>
                  <option value="STATE_ID">State ID</option>
                  <option value="PASSPORT">Passport</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="field">
                <label>Last 4 digits</label>
                <input v-model="verifyForm.documentLast4" maxlength="4" pattern="\d{4}" required />
              </div>
              <div class="field" style="grid-column:1/-1;">
                <label>Note (optional)</label>
                <input v-model="verifyForm.note" />
              </div>
            </div>
            <div style="margin-top:8px;">
              <button type="submit" class="btn btn-primary" :disabled="verifying">
                <span v-if="verifying" class="spinner-sm" /> Verify ID
              </button>
            </div>
          </form>
          <div class="section-label mono" style="margin-top:14px;">History</div>
          <EmptyState v-if="verifications.length === 0" title="Not verified" message="No ID verification on record." />
          <ul v-else class="visit-list">
            <li v-for="v in verifications" :key="v.id" class="visit-item">
              <span class="mono text-amber">{{ v.documentType }}</span>
              <span class="mono">**** {{ v.documentLast4 }}</span>
              <span class="mono text-muted">{{ formatDate(v.verifiedAt) }}</span>
            </li>
          </ul>
        </div>

        <div class="card">
          <div class="section-label mono">Visits</div>
          <LoadingState v-if="loadingVisits" label="Loading visits..." />
          <EmptyState v-else-if="visits.length === 0" title="No visits" message="No visits for this patient." />
          <ul v-else class="visit-list">
            <li v-for="v in visits" :key="v.id" class="visit-item">
              <span class="mono text-amber">{{ v.id }}</span>
              <span>{{ v.status }}</span>
              <span class="mono text-muted">{{ formatDate(v.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </template>

    <!-- Edit dialog -->
    <Teleport to="body">
      <div v-if="showEdit && patient" class="dialog-backdrop" @click.self="showEdit = false">
        <div class="dialog-box fade-in" style="max-width:560px;">
          <div class="dialog-header">
            <span class="dialog-title">Edit Patient</span>
            <button class="btn btn-ghost btn-sm" @click="showEdit = false">✕</button>
          </div>
          <form @submit.prevent="saveEdit" class="dialog-body">
            <div v-if="editError" class="form-error">{{ editError }}</div>
            <div class="form-grid">
              <div class="field">
                <label>First Name</label>
                <input v-model="editForm.firstName" required />
              </div>
              <div class="field">
                <label>Last Name</label>
                <input v-model="editForm.lastName" required />
              </div>
              <div class="field">
                <label>Phone</label>
                <input v-model="editForm.phone" type="tel" />
              </div>
              <div class="field">
                <label>Date of Birth</label>
                <input v-model="editForm.dob" type="date" />
              </div>
              <div class="field" style="grid-column:1/-1;">
                <label>Address</label>
                <input v-model="editForm.address" />
              </div>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showEdit = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="spinner-sm" /> Save
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      v-model="deleteDialog"
      title="Archive Patient"
      message="This patient will be soft-deleted. Continue?"
      confirm-label="Archive"
      :danger="true"
      :loading="deleting"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'
import ArchivedBadge from '../../components/ArchivedBadge.vue'
import SensitiveField from '../../components/SensitiveField.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const router = useRouter()
const route = useRoute()
const toast = useToastStore()

const patient = ref(null)
const visits = ref([])
const loading = ref(true)
const loadingVisits = ref(true)
const error = ref('')
const showEdit = ref(false)
const saving = ref(false)
const editError = ref('')
const editForm = ref({})
const deleteDialog = ref(false)
const deleting = ref(false)
const verifications = ref([])
const verifying = ref(false)
const verifyError = ref('')
const verifyForm = ref({ documentType: '', documentLast4: '', note: '' })

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleDateString()
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/patients/${route.params.id}`)
    patient.value = res
    editForm.value = { ...res }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadVisits() {
  try {
    const res = await client.get(`/visits?patientId=${route.params.id}`)
    visits.value = res || []
  } catch (_) {
    visits.value = []
  } finally {
    loadingVisits.value = false
  }
}

async function loadVerifications() {
  try {
    const res = await client.get(`/patients/${route.params.id}/verifications`)
    verifications.value = res || []
  } catch (_) {
    verifications.value = []
  }
}

async function verifyId() {
  if (verifying.value) return
  verifyError.value = ''
  if (!/^\d{4}$/.test(verifyForm.value.documentLast4)) {
    verifyError.value = 'Last 4 digits must be exactly 4 digits.'
    return
  }
  verifying.value = true
  try {
    await client.post(`/patients/${route.params.id}/verify-identity`, {
      documentType: verifyForm.value.documentType,
      documentLast4: verifyForm.value.documentLast4,
      note: verifyForm.value.note || null,
    })
    toast.success('Identity verified.')
    verifyForm.value = { documentType: '', documentLast4: '', note: '' }
    await loadVerifications()
  } catch (e) {
    verifyError.value = e.message
  } finally {
    verifying.value = false
  }
}

async function saveEdit() {
  // Patient record editing is not supported by the backend. Only registration
  // + archive are permitted. Surface the closest supported action instead.
  editError.value = 'Patient record editing is not supported. Re-register or archive instead.'
}

async function doDelete() {
  deleting.value = true
  try {
    await client.delete(`/patients/${patient.value.id}/archive`)
    toast.success('Patient archived.')
    router.push('/patients')
  } catch (e) {
    toast.error(e.message)
    deleteDialog.value = false
  } finally {
    deleting.value = false
  }
}

onMounted(() => {
  load()
  loadVisits()
  loadVerifications()
})
</script>

<style scoped>
.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.section-label {
  font-size: 10px; letter-spacing: 0.1em; text-transform: uppercase;
  color: var(--text-muted); margin-bottom: 16px;
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}

.detail-row:last-child { border-bottom: none; padding-bottom: 0; }

dt { color: var(--text-muted); font-size: 12px; }
dd { text-align: right; }

.visit-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }

.visit-item {
  display: flex; gap: 12px; align-items: center; justify-content: space-between;
  font-size: 13px; padding-bottom: 8px; border-bottom: 1px solid var(--border);
}

.visit-item:last-child { border-bottom: none; padding-bottom: 0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }

.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-bottom: 16px;
}

.dialog-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 9000; backdrop-filter: blur(2px);
}

.dialog-box {
  background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--radius-lg);
  width: 100%; box-shadow: 0 20px 60px rgba(0,0,0,0.6);
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

.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
