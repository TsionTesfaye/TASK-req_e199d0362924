<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Patients</h1>
        <p class="page-subtitle">Registered individuals</p>
      </div>
      <button class="btn btn-primary" @click="showCreate = true">+ Register Patient</button>
    </div>

    <!-- Filters -->
    <div class="filter-bar">
      <input v-model="search" type="text" placeholder="Search by name or ID..." style="max-width:260px;" @input="debounceLoad" />
      <select v-model="archived" @change="loadPatients(1)" style="max-width:160px;">
        <option value="">Active</option>
        <option value="true">Archived</option>
        <option value="all">All</option>
      </select>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="patients"
        :loading="loading"
        :error="error"
        :meta="meta"
        :page="page"
        empty-title="No patients"
        empty-message="Register the first patient using the button above."
        :on-retry="() => loadPatients()"
        @page="loadPatients"
        @row-click="goToDetail"
      >
        <template #mrn="{ row }">
          <span class="mono">{{ row.medicalRecordNumber || '—' }}</span>
          <ArchivedBadge v-if="row.archivedAt" />
        </template>
        <template #dateOfBirth="{ row }">
          <span class="mono">{{ row.dateOfBirth || '—' }}</span>
        </template>
        <template #phone="{ row }">
          <span class="mono">{{ row.phoneLast4 ? '****' + row.phoneLast4 : '—' }}</span>
        </template>
        <template #actions="{ row }">
          <button class="btn btn-ghost btn-sm" @click.stop="goToDetail(row)">View →</button>
          <button class="btn btn-danger btn-sm" @click.stop="confirmDelete(row)">Archive</button>
        </template>
      </DataTable>
    </div>

    <!-- Create patient dialog -->
    <Teleport to="body">
      <div v-if="showCreate" class="dialog-backdrop" @click.self="showCreate = false">
        <div class="dialog-box fade-in" style="max-width:560px;">
          <div class="dialog-header">
            <span class="dialog-title">Register Patient</span>
            <button class="btn btn-ghost btn-sm" @click="showCreate = false">✕</button>
          </div>
          <form @submit.prevent="createPatient" class="dialog-body">
            <div v-if="createError" class="form-error">{{ createError }}</div>
            <div class="form-grid">
              <div class="field">
                <label>First Name *</label>
                <input v-model="form.firstName" required />
              </div>
              <div class="field">
                <label>Last Name *</label>
                <input v-model="form.lastName" required />
              </div>
              <div class="field">
                <label>Date of Birth *</label>
                <input v-model="form.dateOfBirth" type="date" required />
              </div>
              <div class="field">
                <label>Phone</label>
                <input v-model="form.phone" type="tel" />
              </div>
              <div class="field" style="grid-column:1/-1;">
                <label>Address</label>
                <input v-model="form.address" />
              </div>
              <div class="field checkbox-field">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.isMinor" /> Minor
                </label>
              </div>
              <div class="field checkbox-field">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.isProtectedCase" /> Protected case
                </label>
              </div>
              <div class="field" style="grid-column:1/-1;">
                <p class="text-dim mono" style="font-size:11px;">
                  ID verification is captured separately after registration on the patient detail page.
                </p>
              </div>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showCreate = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="creating">
                <span v-if="creating" class="spinner-sm" /> Register
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      v-model="deleteDialog.show"
      title="Archive Patient"
      :message="`Archive patient ${deleteDialog.patient?.medicalRecordNumber}? This will soft-delete their record.`"
      confirm-label="Archive"
      :danger="true"
      :loading="deleteDialog.loading"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client, { getMeta } from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import DataTable from '../../components/DataTable.vue'
import ArchivedBadge from '../../components/ArchivedBadge.vue'
import SensitiveField from '../../components/SensitiveField.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const router = useRouter()
const toast = useToastStore()

const patients = ref([])
const loading = ref(false)
const error = ref('')
const meta = ref(null)
const page = ref(1)
const search = ref('')
const archived = ref('')

const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const form = ref({ firstName:'', lastName:'', dateOfBirth:'', phone:'', address:'', isMinor:false, isProtectedCase:false })

const deleteDialog = ref({ show: false, patient: null, loading: false })

const columns = [
  { key: 'mrn',         label: 'MRN'   },
  { key: 'dateOfBirth', label: 'DOB'   },
  { key: 'phone',       label: 'Phone' },
  { key: 'actions',     label: ''      },
]

let debounceTimer = null
function debounceLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => loadPatients(1), 350)
}

async function loadPatients(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: p, size: 20 })
    if (search.value) params.set('q', search.value)
    if (archived.value === 'true') params.set('archived', 'true')
    else if (archived.value === 'all') params.set('archived', 'all')
    const res = await client.get(`/patients?${params}`)
    patients.value = res || []
    meta.value = getMeta(res) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function createPatient() {
  creating.value = true
  createError.value = ''
  try {
    await client.post('/patients', form.value)
    toast.success('Patient registered.')
    showCreate.value = false
    form.value = { firstName:'', lastName:'', dateOfBirth:'', phone:'', address:'', isMinor:false, isProtectedCase:false }
    loadPatients(1)
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

function goToDetail(row) {
  router.push(`/patients/${row.id}`)
}

function confirmDelete(row) {
  deleteDialog.value = { show: true, patient: row, loading: false }
}

async function doDelete() {
  deleteDialog.value.loading = true
  try {
    await client.delete(`/patients/${deleteDialog.value.patient.id}/archive`)
    toast.success('Patient archived.')
    deleteDialog.value.show = false
    loadPatients(page.value)
  } catch (e) {
    toast.error(e.message)
    deleteDialog.value.show = false
  }
}

onMounted(() => loadPatients())
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.patient-name {
  font-weight: 500;
  margin-right: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 4px;
}

.form-error {
  background: var(--red-dim);
  border: 1px solid var(--red);
  border-radius: var(--radius);
  padding: 10px 14px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--red);
  margin-bottom: 16px;
}

.dialog-backdrop {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center;
  z-index: 9000;
  backdrop-filter: blur(2px);
}

.dialog-box {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius-lg);
  width: 100%;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6);
}

.dialog-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--border);
}

.dialog-title { font-family: var(--font-display); font-weight: 700; font-size: 15px; }

.dialog-body { padding: 20px; }

.dialog-footer {
  display: flex; justify-content: flex-end; gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
}

.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
