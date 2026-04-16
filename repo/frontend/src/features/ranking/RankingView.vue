<script setup>
import { ref, onMounted } from 'vue'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import LoadingState from '../../components/LoadingState.vue'
import EmptyState from '../../components/EmptyState.vue'

const toast = useToastStore()
const weights = ref({ recency: 1, favorites: 2, comments: 1.5, moderatorBoost: 5, coldStartBase: 0.5 })
const ranked = ref([])
const loading = ref(true)
const saving = ref(false)
const promoting = ref(false)
const promoteForm = ref({ contentType: 'incident', contentId: '', favoriteCount: 0, commentCount: 0, ageHours: 0 })

async function loadAll() {
  loading.value = true
  try {
    const w = await client.get('/ranking/weights')
    weights.value = w
    const list = await client.get('/ranking?page=0&size=50')
    ranked.value = list || []
  } catch (e) {
    toast.error(e.message)
  } finally {
    loading.value = false
  }
}

async function saveWeights() {
  if (saving.value) return
  saving.value = true
  try {
    weights.value = await client.put('/ranking/weights', weights.value)
    toast.success('Weights updated.')
  } catch (e) {
    toast.error(e.message)
  } finally {
    saving.value = false
  }
}

async function promote() {
  if (promoting.value) return
  promoting.value = true
  try {
    await client.post('/ranking/promote', {
      contentType: promoteForm.value.contentType,
      contentId: Number(promoteForm.value.contentId),
      favoriteCount: Number(promoteForm.value.favoriteCount) || 0,
      commentCount: Number(promoteForm.value.commentCount) || 0,
      ageHours: Number(promoteForm.value.ageHours) || 0,
    })
    toast.success('Content promoted.')
    promoteForm.value = { contentType: 'incident', contentId: '', favoriteCount: 0, commentCount: 0, ageHours: 0 }
    await loadAll()
  } catch (e) {
    toast.error(e.message)
  } finally {
    promoting.value = false
  }
}

onMounted(loadAll)
</script>

<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">Ranking & Promotion</h1>
        <p class="page-subtitle">Adjust ranking weights, promote items, and review the ranked list.</p>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <template v-else>
      <div class="detail-grid">
        <div class="card">
          <div class="section-label mono">Weights</div>
          <form @submit.prevent="saveWeights" class="form-grid">
            <div class="field"><label>Recency</label><input v-model.number="weights.recency" type="number" step="0.1" min="0" required /></div>
            <div class="field"><label>Favorites</label><input v-model.number="weights.favorites" type="number" step="0.1" min="0" required /></div>
            <div class="field"><label>Comments</label><input v-model.number="weights.comments" type="number" step="0.1" min="0" required /></div>
            <div class="field"><label>Moderator boost</label><input v-model.number="weights.moderatorBoost" type="number" step="0.1" min="0" required /></div>
            <div class="field"><label>Cold-start base</label><input v-model.number="weights.coldStartBase" type="number" step="0.1" min="0" required /></div>
            <div class="field" style="grid-column:1/-1;">
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="spinner-sm" /> Save weights
              </button>
            </div>
          </form>
        </div>

        <div class="card">
          <div class="section-label mono">Promote</div>
          <form @submit.prevent="promote" class="form-grid">
            <div class="field">
              <label>Content type</label>
              <select v-model="promoteForm.contentType">
                <option value="incident">incident</option>
                <option value="bulletin">bulletin</option>
              </select>
            </div>
            <div class="field"><label>Content id</label><input v-model="promoteForm.contentId" type="number" required /></div>
            <div class="field"><label>Favorites</label><input v-model.number="promoteForm.favoriteCount" type="number" min="0" /></div>
            <div class="field"><label>Comments</label><input v-model.number="promoteForm.commentCount" type="number" min="0" /></div>
            <div class="field"><label>Age (hours)</label><input v-model.number="promoteForm.ageHours" type="number" min="0" /></div>
            <div class="field" style="grid-column:1/-1;">
              <button type="submit" class="btn btn-primary" :disabled="promoting">
                <span v-if="promoting" class="spinner-sm" /> Promote
              </button>
            </div>
          </form>
        </div>
      </div>

      <div class="card" style="margin-top:16px;">
        <div class="section-label mono">Ranked List</div>
        <EmptyState v-if="ranked.length === 0" title="Empty" message="No items ranked yet." />
        <table v-else class="data-table">
          <thead><tr><th>Type</th><th>Content ID</th><th>Score</th><th>Promoted by</th><th>Updated</th></tr></thead>
          <tbody>
            <tr v-for="r in ranked" :key="r.id">
              <td class="mono">{{ r.contentType }}</td>
              <td class="mono">{{ r.contentId }}</td>
              <td class="mono text-amber">{{ r.score }}</td>
              <td class="mono">{{ r.promotedByUserId }}</td>
              <td class="mono">{{ r.updatedAt ? new Date(r.updatedAt).toLocaleString() : '' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.field label { display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 4px; }
.field input, .field select { width: 100%; padding: 6px; }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 6px; border-bottom: 1px solid var(--border); text-align: left; font-size: 13px; }
</style>
