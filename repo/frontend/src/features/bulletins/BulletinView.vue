<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Bulletins</h1>
        <p class="page-subtitle">Organization-wide announcements</p>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="load" />
    <EmptyState v-else-if="bulletins.length === 0" title="No bulletins" message="No bulletins posted." />

    <div v-else class="bulletin-grid">
      <div v-for="b in bulletins" :key="b.id" class="bulletin-card card">
        <div class="bull-header">
          <span class="bull-title">{{ b.title }}</span>
          <span class="badge badge-gray mono">{{ formatDate(b.createdAt) }}</span>
        </div>
        <p class="bull-body">{{ b.body || b.content }}</p>
        <div class="bull-footer">
          <span class="mono text-muted">{{ b.author?.displayName || b.authorId || '' }}</span>
          <div class="flex gap-2">
            <button class="btn btn-ghost btn-sm" @click="toggleFav(b)">
              {{ b.favorited ? '★ Unfav' : '☆ Fav' }}
            </button>
            <span class="mono text-muted" style="font-size:11px;">{{ b.commentCount ?? 0 }} comments</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'

const toast = useToastStore()
const bulletins = ref([])
const loading = ref(true)
const error = ref('')

function formatDate(d) { return d ? new Date(d).toLocaleDateString() : '' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/bulletins')
    bulletins.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function toggleFav(b) {
  try {
    if (b.favorited) {
      await client.delete(`/favorites?contentType=bulletin&contentId=${b.id}`)
      b.favorited = false
    } else {
      await client.post('/favorites', { contentType: 'bulletin', contentId: b.id })
      b.favorited = true
    }
  } catch (e) {
    toast.error(e.message)
  }
}

onMounted(() => load())
</script>

<style scoped>
.bulletin-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bulletin-card {
  transition: border-color 0.15s;
}

.bulletin-card:hover {
  border-color: var(--border-2);
}

.bull-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.bull-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 15px;
}

.bull-body {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-muted);
  margin-bottom: 14px;
}

.bull-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 12px;
}
</style>
