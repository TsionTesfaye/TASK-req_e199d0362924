<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Audit Log</h1>
        <p class="page-subtitle">System-wide activity trail (Admin only)</p>
      </div>
    </div>

    <div class="filter-bar">
      <input v-model="search" type="text" placeholder="Filter by actor or action..." style="max-width:280px;" @input="debounceLoad" />
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="entries"
        :loading="loading"
        :error="error"
        :meta="meta"
        :page="page"
        empty-title="No audit entries"
        empty-message="No audit records found."
        :on-retry="() => load()"
        @page="load"
      >
        <template #timestamp="{ row }">
          <span class="mono">{{ formatDate(row.timestamp || row.createdAt) }}</span>
        </template>
        <template #actor="{ row }">
          <span class="mono text-amber">{{ row.actorUsername || row.actor || '—' }}</span>
        </template>
        <template #action="{ row }">
          <span class="badge badge-blue">{{ row.action }}</span>
        </template>
        <template #entityId="{ row }">
          <span class="mono text-muted">{{ row.entityId?.slice(0,12) || '—' }}</span>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client, { getMeta } from '../../api/client.js'
import DataTable from '../../components/DataTable.vue'

const entries = ref([])
const loading = ref(false)
const error = ref('')
const meta = ref(null)
const page = ref(1)
const search = ref('')

const columns = [
  { key: 'timestamp', label: 'Timestamp'   },
  { key: 'actor',     label: 'Actor'       },
  { key: 'action',    label: 'Action'      },
  { key: 'entity',    label: 'Entity', format: (v) => v || '—' },
  { key: 'entityId',  label: 'Entity ID'   },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

let debounceTimer = null
function debounceLoad() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => load(1), 400)
}

async function load(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const params = new URLSearchParams({ page: p, size: 30 })
    if (search.value) params.set('q', search.value)
    const res = await client.get(`/audit?${params}`)
    entries.value = res || []
    meta.value = getMeta(res) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
</style>
