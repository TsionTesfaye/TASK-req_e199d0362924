<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <button class="btn btn-ghost btn-sm" @click="router.back()" style="margin-bottom:8px;">← Back</button>
        <h1 class="page-title" v-if="invoice">Invoice <span class="mono text-amber">{{ String(invoice.id).substring(0, 8) }}</span></h1>
        <p class="page-subtitle mono" v-if="invoice">
          <span class="badge" :class="statusBadge(invoice.status)">{{ invoice.status }}</span>
          &nbsp;·&nbsp;Balance: ${{ (invoice.outstandingAmount ?? 0).toFixed(2) }}
        </p>
      </div>
      <div v-if="invoice && invoice.status !== 'VOIDED'" class="flex gap-2">
        <button class="btn btn-secondary" @click="showPayment = true">Record Payment</button>
        <button class="btn btn-secondary" @click="showRefund = true" :disabled="!canRefund">Refund</button>
        <button class="btn btn-danger" @click="voidConfirm = true">Void</button>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="load" />

    <template v-else-if="invoice">
      <div class="detail-grid">
        <div class="card">
          <div class="section-label mono">Line Items</div>
          <table>
            <thead>
              <tr>
                <th>Description</th>
                <th>Qty</th>
                <th>Unit</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="li in invoice.lineItems || []" :key="li.id">
                <td>{{ li.description }}</td>
                <td class="mono">{{ li.quantity ?? 1 }}</td>
                <td class="mono">${{ (li.unitPrice ?? li.amount ?? 0).toFixed(2) }}</td>
                <td class="mono">${{ (li.total ?? li.amount ?? 0).toFixed(2) }}</td>
              </tr>
              <tr v-if="!(invoice.lineItems?.length)">
                <td colspan="4" class="text-muted mono" style="text-align:center;font-size:12px;">No line items</td>
              </tr>
            </tbody>
          </table>
          <div class="totals">
            <div class="total-row">
              <span class="mono text-muted">Subtotal</span>
              <span class="mono">${{ (invoice.subtotalAmount ?? 0).toFixed(2) }}</span>
            </div>
            <div class="total-row">
              <span class="mono text-muted">Paid</span>
              <span class="mono text-green">${{ ((invoice.totalAmount ?? 0) - (invoice.outstandingAmount ?? 0)).toFixed(2) }}</span>
            </div>
            <div class="total-row total-due">
              <span class="mono">Balance Due</span>
              <span class="mono text-amber">${{ (invoice.outstandingAmount ?? 0).toFixed(2) }}</span>
            </div>
          </div>
        </div>

        <div class="card">
          <div class="section-label mono">Payment Ledger</div>
          <LoadingState v-if="loadingLedger" label="Loading payments..." />
          <EmptyState v-else-if="payments.length === 0" title="No payments" message="No payments recorded." />
          <ul v-else class="ledger-list">
            <li v-for="p in payments" :key="p.id" class="ledger-item">
              <div>
                <span class="mono text-green">+${{ (p.amount ?? 0).toFixed(2) }}</span>
                <span class="badge badge-gray mono" style="margin-left:8px;">{{ p.tenderType }}</span>
              </div>
              <span class="mono text-muted">{{ formatDate(p.createdAt) }}</span>
            </li>
          </ul>
        </div>
      </div>
    </template>

    <!-- Record Payment dialog -->
    <Teleport to="body">
      <div v-if="showPayment" class="dialog-backdrop" @click.self="showPayment = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Record Payment</span>
            <button class="btn btn-ghost btn-sm" @click="showPayment = false">✕</button>
          </div>
          <form @submit.prevent="recordPayment" class="dialog-body">
            <div v-if="paymentError" class="form-error">{{ paymentError }}</div>
            <div class="field">
              <label>Amount *</label>
              <input v-model.number="paymentForm.amount" type="number" step="0.01" min="0.01" required />
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Method *</label>
              <select v-model="paymentForm.tenderType" required>
                <option value="CASH">Cash</option>
                <option value="CHECK">Check</option>
                <option value="EXTERNAL_CARD_ON_FILE">External Card on File</option>
              </select>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showPayment = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="paymentLoading">
                <span v-if="paymentLoading" class="spinner-sm" /> Record
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Refund dialog -->
    <Teleport to="body">
      <div v-if="showRefund" class="dialog-backdrop" @click.self="showRefund = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Issue Refund</span>
            <button class="btn btn-ghost btn-sm" @click="showRefund = false">✕</button>
          </div>
          <form @submit.prevent="issueRefund" class="dialog-body">
            <div v-if="refundError" class="form-error">{{ refundError }}</div>
            <div class="field">
              <label>Amount *</label>
              <input v-model.number="refundForm.amount" type="number" step="0.01" min="0.01" required />
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Reason</label>
              <textarea v-model="refundForm.reason" rows="2" />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showRefund = false">Cancel</button>
              <button type="submit" class="btn btn-danger" :disabled="refundLoading">
                <span v-if="refundLoading" class="spinner-sm" /> Issue Refund
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Void confirm -->
    <ConfirmDialog
      v-model="voidConfirm"
      title="Void Invoice"
      message="This will void the invoice and cannot be undone. Continue?"
      confirm-label="Void Invoice"
      :danger="true"
      :loading="voiding"
      @confirm="doVoid"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import client, { uuid } from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const router = useRouter()
const route = useRoute()
const toast = useToastStore()

const invoice = ref(null)
const payments = ref([])
const loading = ref(true)
const loadingLedger = ref(true)
const error = ref('')

const showPayment = ref(false)
const paymentLoading = ref(false)
const paymentError = ref('')
const paymentForm = ref({ amount: '', tenderType: 'CASH', externalReference: '' })

const showRefund = ref(false)
const refundLoading = ref(false)
const refundError = ref('')
const refundForm = ref({ amount: '', reason: '' })

const voidConfirm = ref(false)
const voiding = ref(false)

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

function statusBadge(s) {
  const map = { OPEN: 'badge-amber', PAID: 'badge-green', VOIDED: 'badge-red', PARTIAL: 'badge-blue' }
  return map[s] || 'badge-gray'
}

const canRefund = computed(() => {
  if (!invoice.value?.createdAt) return false
  const created = new Date(invoice.value.createdAt)
  const diff = (Date.now() - created.getTime()) / (1000 * 60 * 60 * 24)
  return diff <= 30
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/invoices/${route.params.id}`)
    invoice.value = res
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadPayments() {
  try {
    const res = await client.get(`/invoices/${route.params.id}/payments`)
    payments.value = res || []
  } catch (_) {
    payments.value = []
  } finally {
    loadingLedger.value = false
  }
}

async function recordPayment() {
  paymentLoading.value = true
  paymentError.value = ''
  try {
    await client.post(`/invoices/${invoice.value.id}/payments`, {
      tenderType: paymentForm.value.tenderType,
      amount: paymentForm.value.amount,
      externalReference: paymentForm.value.externalReference || null,
    })
    toast.success('Payment recorded.')
    showPayment.value = false
    paymentForm.value = { amount: '', tenderType: 'CASH', externalReference: '' }
    load()
    loadPayments()
  } catch (e) {
    paymentError.value = e.message
  } finally {
    paymentLoading.value = false
  }
}

async function issueRefund() {
  if (refundLoading.value) return // guard against double-click
  if (!canRefund.value) {
    refundError.value = 'Refunds are not available beyond 30 days of invoice creation.'
    return
  }
  refundLoading.value = true
  refundError.value = ''
  const idempotencyKey = uuid()
  try {
    await client.post(`/invoices/${invoice.value.id}/refunds`, refundForm.value, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    toast.success('Refund issued.')
    showRefund.value = false
    refundForm.value = { amount: '', reason: '' }
    load()
    loadPayments()
  } catch (e) {
    refundError.value = e.message
  } finally {
    refundLoading.value = false
  }
}

async function doVoid() {
  if (voiding.value) return // guard against double-click
  voiding.value = true
  const idempotencyKey = uuid()
  try {
    await client.post(`/invoices/${invoice.value.id}/void`, {}, {
      headers: { 'Idempotency-Key': idempotencyKey },
    })
    toast.success('Invoice voided.')
    voidConfirm.value = false
    load()
  } catch (e) {
    toast.error(e.message)
    voidConfirm.value = false
  } finally {
    voiding.value = false
  }
}

onMounted(() => {
  load()
  loadPayments()
})
</script>

<style scoped>
.detail-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.section-label { font-size:10px; letter-spacing:0.1em; text-transform:uppercase; color:var(--text-muted); margin-bottom:12px; }
.totals { margin-top: 16px; padding-top: 12px; border-top: 1px solid var(--border); display: flex; flex-direction: column; gap: 6px; }
.total-row { display: flex; justify-content: space-between; font-size: 13px; }
.total-due { padding-top: 8px; border-top: 1px solid var(--border-2); font-weight: 600; }
.ledger-list { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.ledger-item {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; padding-bottom: 8px; border-bottom: 1px solid var(--border);
}
.ledger-item:last-child { border-bottom: none; padding-bottom: 0; }
.dialog-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 9000; backdrop-filter: blur(2px);
}
.dialog-box {
  background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--radius-lg);
  width: 100%; max-width: 420px; box-shadow: 0 20px 60px rgba(0,0,0,0.6);
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
