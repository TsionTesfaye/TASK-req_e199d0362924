<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Search</h1>
        <p class="page-subtitle">Incidents · Bulletins · All records</p>
      </div>
    </div>

    <div class="search-bar">
      <input
        v-model="query"
        type="text"
        placeholder="Search incidents, bulletins, patients..."
        class="search-input"
        @keydown.enter="search"
      />
      <select v-model="typeFilter" style="max-width:160px;">
        <option value="">All Types</option>
        <option value="incident">Incidents</option>
        <option value="bulletin">Bulletins</option>
      </select>
      <select v-model="sortFilter" style="max-width:160px;">
        <option value="recent">Recent</option>
        <option value="popular">Popular</option>
        <option value="favorites">Favorites</option>
        <option value="comments">Comments</option>
      </select>
      <button class="btn btn-primary" @click="search" :disabled="loading || !query.trim()">
        <span v-if="loading" class="spinner-sm" /> Search
      </button>
    </div>

    <div v-if="searched" class="card" style="margin-top:16px;">
      <DataTable
        :columns="columns"
        :rows="results"
        :loading="loading"
        :error="error"
        empty-title="No results"
        :empty-message="`No results for &quot;${query}&quot;`"
      >
        <template #type="{ row }">
          <span class="badge" :class="row.type === 'incident' ? 'badge-amber' : 'badge-blue'">
            {{ row.type || '—' }}
          </span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
      </DataTable>
    </div>

    <div v-else class="empty-prompt">
      <span class="mono text-muted">Enter a query and press Search</span>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import client from '../../api/client.js'
import DataTable from '../../components/DataTable.vue'

const query = ref('')
const typeFilter = ref('')
const sortFilter = ref('recent')
const results = ref([])
const loading = ref(false)
const error = ref('')
const searched = ref(false)

const columns = [
  { key: 'type',      label: 'Type' },
  { key: 'title',     label: 'Title', format: (v, row) => v || row.description?.slice(0,60) || '—' },
  { key: 'createdAt', label: 'Date' },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

async function search() {
  if (!query.value.trim()) return
  loading.value = true
  error.value = ''
  searched.value = true
  try {
    const params = new URLSearchParams({ q: query.value, sort: sortFilter.value })
    if (typeFilter.value) params.set('type', typeFilter.value)
    const res = await client.get(`/search?${params}`)
    results.value = res || []
  } catch (e) {
    error.value = e.message
    results.value = []
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 220px;
}

.empty-prompt {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
