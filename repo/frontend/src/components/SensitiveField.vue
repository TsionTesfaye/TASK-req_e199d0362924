<template>
  <span class="sensitive-wrap">
    <span v-if="!revealed" class="masked">****</span>
    <span v-else class="revealed-container">
      <span class="revealed-value">{{ revealedValue }}</span>
      <div class="watermark" aria-hidden="true">
        {{ watermarkText }}
      </div>
    </span>
    <button
      v-if="!revealed"
      class="btn btn-ghost btn-sm reveal-btn"
      :disabled="loading"
      @click="reveal"
    >
      <span v-if="loading" class="spinner-sm" />
      Reveal
    </button>
    <button
      v-else
      class="btn btn-ghost btn-sm reveal-btn"
      @click="hide"
    >
      Hide
    </button>
  </span>
</template>

<script setup>
import { ref, computed } from 'vue'
import client from '../api/client.js'
import { useSessionStore } from '../stores/session.js'

const props = defineProps({
  patientId: { type: [String, Number], required: true },
  field:     { type: String, default: '' },
})

const session = useSessionStore()
const loading = ref(false)
const revealed = ref(false)
const revealedValue = ref('')
const revealedAt = ref(null)

const watermarkText = computed(() => {
  const name = session.user?.displayName || session.user?.username || 'User'
  const ts = revealedAt.value ? new Date(revealedAt.value).toISOString() : ''
  return `${name} — ${ts}`
})

async function reveal() {
  loading.value = true
  try {
    const res = await client.get(`/patients/${props.patientId}/reveal`)
    revealedValue.value = props.field ? (res?.[props.field] ?? JSON.stringify(res)) : JSON.stringify(res)
    revealedAt.value = Date.now()
    revealed.value = true
  } catch (e) {
    revealedValue.value = '[Reveal failed]'
    revealed.value = true
  } finally {
    loading.value = false
  }
}

function hide() {
  revealed.value = false
  revealedValue.value = ''
}
</script>

<style scoped>
.sensitive-wrap {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.masked {
  font-family: var(--font-mono);
  letter-spacing: 0.2em;
  color: var(--text-dim);
}

.revealed-container {
  position: relative;
  display: inline-block;
}

.revealed-value {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text);
  background: var(--surface-2);
  padding: 2px 8px;
  border-radius: var(--radius);
  border: 1px solid var(--amber);
}

.watermark {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%) rotate(-25deg);
  font-family: var(--font-mono);
  font-size: 11px;
  color: rgba(245, 158, 11, 0.12);
  white-space: nowrap;
  pointer-events: none;
  z-index: 100;
  letter-spacing: 0.04em;
  user-select: none;
}

.reveal-btn { padding: 2px 8px; font-size: 10px; }

.spinner-sm {
  display: inline-block;
  width: 10px;
  height: 10px;
  border: 1.5px solid var(--border-2);
  border-top-color: var(--amber);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
</style>
