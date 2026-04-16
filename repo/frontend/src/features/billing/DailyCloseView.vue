<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Daily Close</h1>
        <p class="page-subtitle">End-of-day financial reconciliation</p>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="load" />

    <div v-else class="content">
      <div class="card status-card">
        <div class="section-label mono">Today's Status</div>
        <div v-if="status">
          <dl class="detail-list">
            <div class="detail-row">
              <dt>Date</dt>
              <dd class="mono">{{ status.date || today }}</dd>
            </div>
            <div class="detail-row">
              <dt>Status</dt>
              <dd>
                <span class="badge" :class="status.closed ? 'badge-green' : 'badge-amber'">
                  {{ status.closed ? 'Closed' : 'Open' }}
                </span>
              </dd>
            </div>
            <div class="detail-row" v-if="status.closedAt">
              <dt>Closed At</dt>
              <dd class="mono">{{ formatDate(status.closedAt) }}</dd>
            </div>
            <div class="detail-row" v-if="status.totalRevenue !== undefined">
              <dt>Total Revenue</dt>
              <dd class="mono text-green">${{ (status.totalRevenue ?? 0).toFixed(2) }}</dd>
            </div>
            <div class="detail-row" v-if="status.totalInvoices !== undefined">
              <dt>Invoices</dt>
              <dd class="mono">{{ status.totalInvoices }}</dd>
            </div>
          </dl>

          <div style="margin-top:20px;">
            <button
              v-if="!status.closed"
              class="btn btn-primary"
              @click="closeConfirm = true"
            >
              Close Today
            </button>
            <p v-else class="mono text-muted" style="font-size:12px;">Today's books are closed.</p>
          </div>
        </div>
        <EmptyState v-else title="No data" message="No daily close record found." />
      </div>
    </div>

    <ConfirmDialog
      v-model="closeConfirm"
      title="Close Today's Books"
      message="This will finalize today's financial records. This action cannot be undone. Continue?"
      confirm-label="Close Today"
      :loading="closing"
      @confirm="doClose"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const toast = useToastStore()
const status = ref(null)
const loading = ref(true)
const error = ref('')
const closeConfirm = ref(false)
const closing = ref(false)
const today = new Date().toLocaleDateString()

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/daily-close')
    status.value = res
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function doClose() {
  closing.value = true
  try {
    const businessDate = new Date().toISOString().slice(0, 10) // YYYY-MM-DD required by backend
    await client.post('/daily-close', { businessDate })
    toast.success("Today's books have been closed.")
    closeConfirm.value = false
    load()
  } catch (e) {
    toast.error(e.message)
    closeConfirm.value = false
  } finally {
    closing.value = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.status-card { max-width: 520px; }
.section-label { font-size:10px; letter-spacing:0.1em; text-transform:uppercase; color:var(--text-muted); margin-bottom:16px; }
.detail-list { display: flex; flex-direction: column; gap: 10px; }
.detail-row { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 10px; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-row:last-child { border-bottom: none; padding-bottom: 0; }
dt { color: var(--text-muted); font-size: 12px; }
</style>
