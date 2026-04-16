<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Appointments</h1>
        <p class="page-subtitle">Schedule by date</p>
      </div>
      <div class="flex gap-2 items-center">
        <input type="date" v-model="selectedDate" @change="load" style="max-width:180px;" />
      </div>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="appointments"
        :loading="loading"
        :error="error"
        :on-retry="load"
        empty-title="No appointments"
        :empty-message="`No appointments on ${displayDate}.`"
      >
        <template #status="{ row }">
          <span class="badge" :class="statusBadge(row.status)">{{ row.status || '—' }}</span>
        </template>
        <template #time="{ row }">
          <span class="mono">{{ row.time || row.scheduledAt || '—' }}</span>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import client from '../../api/client.js'
import DataTable from '../../components/DataTable.vue'

const loading = ref(false)
const error = ref('')
const appointments = ref([])

const today = new Date()
const pad = (n) => String(n).padStart(2,'0')
const toDateInput = (d) => `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`
const toApiDate = (d) => {
  const [y,m,day] = d.split('-')
  return `${m}/${day}/${y}`
}

const selectedDate = ref(toDateInput(today))
const displayDate = computed(() => new Date(selectedDate.value + 'T00:00:00').toLocaleDateString())

const columns = [
  { key: 'time',        label: 'Time' },
  { key: 'patientName', label: 'Patient', format: (v, row) => v || row.patient?.displayName || row.patientId || '—' },
  { key: 'provider',    label: 'Provider', format: (v) => v || '—' },
  { key: 'type',        label: 'Type', format: (v) => v || '—' },
  { key: 'status',      label: 'Status' },
]

function statusBadge(s) {
  const map = { SCHEDULED: 'badge-blue', COMPLETED: 'badge-green', CANCELLED: 'badge-red', PENDING: 'badge-amber' }
  return map[s?.toUpperCase()] || 'badge-gray'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/appointments?date=${toApiDate(selectedDate.value)}`)
    appointments.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>
