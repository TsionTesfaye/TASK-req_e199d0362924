<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Billing</h1>
        <p class="page-subtitle">Invoices &amp; payment ledger</p>
      </div>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="invoices"
        :loading="loading"
        :error="error"
        :meta="meta"
        :page="page"
        empty-title="No invoices"
        empty-message="Invoices are generated when visits are closed."
        :on-retry="() => load()"
        @page="load"
        @row-click="goToDetail"
      >
        <template #status="{ row }">
          <span class="badge" :class="statusBadge(row.status)">{{ row.status }}</span>
        </template>
        <template #totalAmount="{ row }">
          <span class="mono">${{ (row.totalAmount ?? 0).toFixed(2) }}</span>
        </template>
        <template #outstandingAmount="{ row }">
          <span class="mono" :class="row.outstandingAmount > 0 ? 'text-amber' : 'text-green'">
            ${{ (row.outstandingAmount ?? 0).toFixed(2) }}
          </span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import client, { getMeta } from '../../api/client.js'
import DataTable from '../../components/DataTable.vue'

const router = useRouter()
const invoices = ref([])
const loading = ref(false)
const error = ref('')
const meta = ref(null)
const page = ref(1)

const columns = [
  { key: 'id',          label: 'Invoice ID', format: v => String(v).substring(0, 8) + '…' },
  { key: 'visitId',     label: 'Visit'       },
  { key: 'status',      label: 'Status'      },
  { key: 'totalAmount', label: 'Total'       },
  { key: 'outstandingAmount', label: 'Balance Due' },
  { key: 'createdAt',   label: 'Date'        },
]

function formatDate(d) { return d ? new Date(d).toLocaleDateString() : '—' }

function statusBadge(s) {
  const map = { OPEN: 'badge-amber', PAID: 'badge-green', VOIDED: 'badge-red', PARTIAL: 'badge-blue' }
  return map[s] || 'badge-gray'
}

function goToDetail(row) { router.push(`/billing/${row.id}`) }

async function load(p = 1) {
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/invoices?page=${p}&size=20`)
    invoices.value = res || []
    meta.value = getMeta(res) || null
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(() => load())
</script>
