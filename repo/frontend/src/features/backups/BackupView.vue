<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Backups</h1>
        <p class="page-subtitle">System backup management</p>
      </div>
      <button class="btn btn-primary" @click="runConfirm = true" :disabled="running">
        <span v-if="running" class="spinner-sm" /> Run Backup
      </button>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="backups"
        :loading="loading"
        :error="error"
        :on-retry="load"
        empty-title="No backups"
        empty-message="No backups have been created yet."
      >
        <template #status="{ row }">
          <span class="badge" :class="row.status === 'SUCCESS' ? 'badge-green' : row.status === 'FAILED' ? 'badge-red' : 'badge-amber'">
            {{ row.status }}
          </span>
        </template>
        <template #size="{ row }">
          <span class="mono">{{ formatSize(row.sizeBytes || row.size) }}</span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
        <template #actions="{ row }">
          <button class="btn btn-secondary btn-sm" @click="logRestoreTest(row)">Log Restore Test</button>
        </template>
      </DataTable>
    </div>

    <ConfirmDialog
      v-model="runConfirm"
      title="Run Backup"
      message="Start a new system backup? This may take several minutes."
      confirm-label="Start Backup"
      :loading="running"
      @confirm="doRun"
    />

    <!-- Restore-test form dialog: captures result (PASSED/FAILED) and optional note -->
    <Teleport to="body">
      <div v-if="restoreConfirm.show" class="dialog-backdrop" @click.self="restoreConfirm.show = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Log Restore Test</span>
            <button class="btn btn-ghost btn-sm" @click="restoreConfirm.show = false">✕</button>
          </div>
          <form @submit.prevent="doRestoreTest" class="dialog-body">
            <p class="text-muted" style="font-size:13px;margin-bottom:14px;">
              Backup: <span class="mono">{{ restoreConfirm.backup?.id?.toString().slice(0,8) }}…</span>
            </p>
            <div class="field">
              <label>Result *</label>
              <select v-model="restoreForm.result" required>
                <option value="PASSED">PASSED</option>
                <option value="FAILED">FAILED</option>
              </select>
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Note</label>
              <textarea v-model="restoreForm.note" rows="3" placeholder="Optional notes..." />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="restoreConfirm.show = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="restoreConfirm.loading">
                <span v-if="restoreConfirm.loading" class="spinner-sm" /> Log Test
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
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const toast = useToastStore()
const backups = ref([])
const loading = ref(false)
const error = ref('')
const running = ref(false)
const runConfirm = ref(false)
const restoreConfirm = ref({ show: false, backup: null, loading: false })
const restoreForm = ref({ result: 'PASSED', note: '' })

const columns = [
  { key: 'id',        label: 'Backup ID', format: v => String(v).slice(0,8) + '…' },
  { key: 'status',    label: 'Status' },
  { key: 'size',      label: 'Size' },
  { key: 'createdAt', label: 'Created' },
  { key: 'actions',   label: '' },
]

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

function formatSize(bytes) {
  if (!bytes) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/backups')
    backups.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function doRun() {
  running.value = true
  try {
    await client.post('/backups/run')
    toast.success('Backup started.')
    runConfirm.value = false
    load()
  } catch (e) {
    toast.error(e.message)
    runConfirm.value = false
  } finally {
    running.value = false
  }
}

function logRestoreTest(backup) {
  restoreConfirm.value = { show: true, backup, loading: false }
  restoreForm.value = { result: 'PASSED', note: '' }
}

async function doRestoreTest() {
  restoreConfirm.value.loading = true
  try {
    const backup = restoreConfirm.value.backup
    // Backend requires: backupRunId (Long), result (PASSED|FAILED), note (optional)
    await client.post(`/backups/${backup.id}/restore-test`, {
      result: restoreForm.value.result,
      note: restoreForm.value.note || null,
    })
    toast.success('Restore test logged.')
    restoreConfirm.value.show = false
    load()
  } catch (e) {
    toast.error(e.message)
    restoreConfirm.value.show = false
  } finally {
    restoreConfirm.value.loading = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
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
</style>
