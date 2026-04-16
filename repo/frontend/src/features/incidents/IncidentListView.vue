<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Incidents</h1>
        <p class="page-subtitle">Field incident reports</p>
      </div>
      <button class="btn btn-primary" @click="showCreate = true">+ New Incident</button>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="incidents"
        :loading="loading"
        :error="error"
        :meta="meta"
        :page="page"
        empty-title="No incidents"
        empty-message="No incident reports filed yet."
        :on-retry="() => load()"
        @page="load"
        @row-click="goToDetail"
      >
        <template #category="{ row }">
          <span class="badge badge-amber">{{ row.category || '—' }}</span>
        </template>
        <template #involvesMinor="{ row }">
          <span v-if="row.involvesMinor" class="badge badge-red">MINOR</span>
          <span v-else class="text-dim mono">—</span>
        </template>
        <template #isProtectedCase="{ row }">
          <span v-if="row.isProtectedCase || row.protectedCase" class="badge badge-blue">PROTECTED</span>
          <span v-else class="text-dim mono">—</span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
      </DataTable>
    </div>

    <!-- Create incident dialog -->
    <Teleport to="body">
      <div v-if="showCreate" class="dialog-backdrop" @click.self="showCreate = false">
        <div class="dialog-box fade-in" style="max-width:600px;">
          <div class="dialog-header">
            <span class="dialog-title">Submit Incident Report</span>
            <button class="btn btn-ghost btn-sm" @click="showCreate = false">✕</button>
          </div>
          <form @submit.prevent="submitIncident" class="dialog-body">
            <div v-if="createError" class="form-error">{{ createError }}</div>
            <div class="form-grid">
              <div class="field" style="grid-column:1/-1;">
                <label>Approximate Location *</label>
                <input v-model="form.approximateLocationText" required placeholder="e.g. 5th Ave & Main St" />
              </div>
              <div class="field">
                <label>Neighborhood</label>
                <input v-model="form.neighborhood" placeholder="Neighborhood name" />
              </div>
              <div class="field">
                <label>Cross Streets</label>
                <input v-model="form.nearestCrossStreets" placeholder="Nearest cross streets (or lat,lon)" />
              </div>
              <div class="field">
                <label>Category *</label>
                <select v-model="form.category" required>
                  <option value="">Select...</option>
                  <option value="MEDICAL">Medical</option>
                  <option value="BEHAVIORAL">Behavioral</option>
                  <option value="ENVIRONMENTAL">Environmental</option>
                  <option value="WELFARE_CHECK">Welfare Check</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
              <div class="field" style="grid-column:1/-1;">
                <label>Description *</label>
                <textarea v-model="form.description" required rows="4" placeholder="Describe the incident..." />
              </div>
              <div class="field checkbox-field">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.isProtectedCase" />
                  Protected Case
                </label>
              </div>
              <div class="field checkbox-field">
                <label class="checkbox-label">
                  <input type="checkbox" v-model="form.involvesMinor" />
                  Involves Minor
                </label>
              </div>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showCreate = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="creating">
                <span v-if="creating" class="spinner-sm" /> Submit
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
import { useRouter } from 'vue-router'
import client, { uuid, getMeta } from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import DataTable from '../../components/DataTable.vue'

const router = useRouter()
const toast = useToastStore()

const incidents = ref([])
const loading = ref(false)
const error = ref('')
const meta = ref(null)
const page = ref(1)
const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const form = ref({
  approximateLocationText: '', neighborhood: '', nearestCrossStreets: '',
  category: '', description: '', isProtectedCase: false, involvesMinor: false,
})

const columns = [
  { key: 'id',                label: 'ID',       format: v => String(v).slice(0,8) + '…' },
  { key: 'category',          label: 'Category'  },
  { key: 'approximateLocationText', label: 'Location' },
  { key: 'involvesMinor',    label: 'Minor'     },
  { key: 'isProtectedCase',  label: 'Protected' },
  { key: 'createdAt',         label: 'Filed'     },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

function goToDetail(row) { router.push(`/incidents/${row.id}`) }

async function load(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/incidents?page=${p}&size=20`)
    incidents.value = res || []
    meta.value = getMeta(res) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function submitIncident() {
  creating.value = true
  createError.value = ''
  const idempotencyKey = uuid()
  try {
    const payload = {
      idempotencyKey,
      category: form.value.category,
      description: form.value.description,
      approximateLocationText: form.value.approximateLocationText,
      neighborhood: form.value.neighborhood || null,
      nearestCrossStreets: form.value.nearestCrossStreets || null,
      exactLocation: null,
      isAnonymous: false,
      involvesMinor: !!form.value.involvesMinor,
      isProtectedCase: !!form.value.isProtectedCase,
      subjectAgeGroup: null,
    }
    const res = await client.post('/incidents', payload, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    toast.success('Incident submitted.')
    showCreate.value = false
    form.value = { approximateLocationText:'', neighborhood:'', nearestCrossStreets:'', category:'', description:'', isProtectedCase:false, involvesMinor:false }
    router.push(`/incidents/${res?.id || res}`)
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
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
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-bottom: 16px;
}
.checkbox-field { display: flex; align-items: center; }
.checkbox-label {
  display: flex; align-items: center; gap: 8px;
  font-family: var(--font-mono); font-size: 12px; color: var(--text);
  cursor: pointer; text-transform: none; letter-spacing: normal;
}
.checkbox-label input[type=checkbox] { width: auto; }
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
