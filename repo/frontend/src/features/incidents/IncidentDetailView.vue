<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <button class="btn btn-ghost btn-sm" @click="router.back()" style="margin-bottom:8px;">← Back</button>
        <h1 class="page-title" v-if="incident">Incident <span class="mono text-amber">{{ String(incident.id).substring(0,8) }}</span></h1>
        <p class="page-subtitle" v-if="incident">{{ incident.category }} · {{ formatDate(incident.createdAt) }}</p>
      </div>
      <div v-if="incident" class="flex gap-2">
        <button class="btn btn-secondary" @click="showMedia = true">Upload Media</button>
        <button v-if="canModerate" class="btn btn-secondary" @click="showModerate = true">Moderate</button>
        <button v-if="canReclassify" class="btn btn-secondary" @click="showReclassify = true">Reclassify</button>
        <button class="btn btn-primary" @click="showRouteSheet = true">Route Sheet</button>
      </div>
    </div>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="load" />

    <template v-else-if="incident">
      <div class="detail-grid">
        <div class="card">
          <div class="section-label mono">Incident Details</div>
          <dl class="detail-list">
            <div class="detail-row"><dt>Location</dt><dd>{{ incident.approximateLocationText }}</dd></div>
            <div class="detail-row"><dt>Neighborhood</dt><dd>{{ incident.neighborhood || '—' }}</dd></div>
            <div class="detail-row"><dt>Cross Streets</dt><dd>{{ incident.nearestCrossStreets || '—' }}</dd></div>
            <div class="detail-row"><dt>Category</dt><dd><span class="badge badge-amber">{{ incident.category }}</span></dd></div>
            <div class="detail-row">
              <dt>Flags</dt>
              <dd class="flex gap-2">
                <span v-if="incident.involvesMinor" class="badge badge-red">MINOR</span>
                <span v-if="incident.isProtectedCase || incident.protectedCase" class="badge badge-blue">PROTECTED</span>
                <span v-if="!incident.involvesMinor && !(incident.isProtectedCase || incident.protectedCase)" class="text-dim mono">None</span>
              </dd>
            </div>
            <div class="detail-row"><dt>Status</dt><dd><span class="badge" :class="statusBadge(incident.status)">{{ incident.status }}</span></dd></div>
          </dl>
          <div class="divider" />
          <div class="section-label mono">Description</div>
          <p class="desc-text">{{ incident.description }}</p>
        </div>

        <div class="card" style="grid-column:1/-1;">
          <div class="section-label mono">Local Map</div>
          <LocalMap :incident="incident" :shelters="shelters" :can-reveal-exact="canReclassify" />
        </div>

        <div class="card">
          <div class="section-label mono">Media Files</div>
          <LoadingState v-if="loadingMedia" label="Loading media..." />
          <EmptyState v-else-if="mediaFiles.length === 0" title="No media" message="Upload files using the button above." />
          <ul v-else class="media-list">
            <li v-for="m in mediaFiles" :key="m.id" class="media-item">
              <span class="mono text-amber">{{ m.fileName }}</span>
              <span class="badge badge-gray mono">{{ m.fileType }}</span>
            </li>
          </ul>
        </div>
      </div>
    </template>

    <!-- Upload Media dialog -->
    <Teleport to="body">
      <div v-if="showMedia" class="dialog-backdrop" @click.self="showMedia = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Upload Media</span>
            <button class="btn btn-ghost btn-sm" @click="showMedia = false">✕</button>
          </div>
          <form @submit.prevent="uploadMedia" class="dialog-body">
            <div v-if="mediaError" class="form-error">{{ mediaError }}</div>
            <div class="field">
              <label>File (one file per upload)</label>
              <input type="file" ref="fileInput" style="color:var(--text);" />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showMedia = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="uploading">
                <span v-if="uploading" class="spinner-sm" /> Upload
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Moderate dialog -->
    <Teleport to="body">
      <div v-if="showModerate" class="dialog-backdrop" @click.self="showModerate = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Moderate Incident</span>
            <button class="btn btn-ghost btn-sm" @click="showModerate = false">✕</button>
          </div>
          <form @submit.prevent="moderate" class="dialog-body">
            <div v-if="moderateError" class="form-error">{{ moderateError }}</div>
            <div class="field">
              <label>Action</label>
              <select v-model="moderateForm.action">
                <option value="APPROVE">Approve</option>
                <option value="FLAG">Flag</option>
                <option value="REMOVE">Remove</option>
              </select>
            </div>
            <div class="field" style="margin-top:12px;">
              <label>Note</label>
              <textarea v-model="moderateForm.note" rows="3" placeholder="Moderation note..." />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showModerate = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="moderating">
                <span v-if="moderating" class="spinner-sm" /> Submit
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Reclassify dialog -->
    <Teleport to="body">
      <div v-if="showReclassify" class="dialog-backdrop" @click.self="showReclassify = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Reclassify Incident</span>
            <button class="btn btn-ghost btn-sm" @click="showReclassify = false">✕</button>
          </div>
          <form @submit.prevent="(async () => { await reclassify(); showReclassify = false; })()" class="dialog-body">
            <label class="field"><input type="checkbox" v-model="reclassifyForm.isProtectedCase" /> Protected case</label>
            <label class="field"><input type="checkbox" v-model="reclassifyForm.involvesMinor" /> Involves minor</label>
            <div class="field">
              <label>Exact location (only if becoming protected/minor; stored encrypted)</label>
              <input v-model="reclassifyForm.exactLocation" placeholder="e.g. 123 Main St Apt 4" />
            </div>
            <div class="field">
              <label>Reason</label>
              <textarea v-model="reclassifyForm.reason" rows="2" />
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showReclassify = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="reclassifying">
                <span v-if="reclassifying" class="spinner-sm" /> Submit
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Route Sheet dialog -->
    <Teleport to="body">
      <div v-if="showRouteSheet" class="dialog-backdrop" @click.self="showRouteSheet = false">
        <div class="dialog-box fade-in" style="max-width:520px;">
          <div class="dialog-header">
            <span class="dialog-title">Generate Route Sheet</span>
            <button class="btn btn-ghost btn-sm" @click="showRouteSheet = false">✕</button>
          </div>
          <form @submit.prevent="createRouteSheet" class="dialog-body">
            <div v-if="routeError" class="form-error">{{ routeError }}</div>
            <div class="field">
              <label>Select Shelter</label>
              <select v-model.number="routeForm.resourceId">
                <option :value="0">Auto-pick nearest</option>
                <option v-for="s in shelters" :key="s.id" :value="s.id">{{ s.name || s.id }}</option>
              </select>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showRouteSheet = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="creatingRoute">
                <span v-if="creatingRoute" class="spinner-sm" /> Generate
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Printable route sheet overlay -->
    <Teleport to="body">
      <div v-if="routeSheet" class="dialog-backdrop" @click.self="routeSheet = null">
        <div class="dialog-box fade-in" style="max-width:600px;">
          <div class="dialog-header">
            <span class="dialog-title">Route Sheet</span>
            <div class="flex gap-2">
              <button class="btn btn-primary btn-sm" onclick="window.print()">Print</button>
              <button class="btn btn-ghost btn-sm" @click="routeSheet = null">✕</button>
            </div>
          </div>
          <div class="dialog-body route-print">
            <div class="print-header">
              <strong>RescueHub Route Sheet</strong>
              <span class="mono">{{ new Date().toLocaleString() }}</span>
            </div>
            <dl class="detail-list">
              <div class="detail-row"><dt>Incident</dt><dd class="mono">{{ routeSheet.incidentReportId || routeSheet.incidentId }}</dd></div>
              <div class="detail-row"><dt>Shelter ID</dt><dd class="mono">{{ routeSheet.resourceId }}</dd></div>
              <div class="detail-row"><dt>Location</dt><dd>{{ incident?.approximateLocationText }}</dd></div>
              <div class="detail-row"><dt>Category</dt><dd>{{ incident?.category }}</dd></div>
              <div class="detail-row"><dt>Generated</dt><dd class="mono">{{ formatDate(routeSheet.createdAt) }}</dd></div>
            </dl>

            <div class="section-label mono" style="margin-top:16px;">Turn-by-turn</div>
            <ol v-if="routeSteps.length > 0" class="route-steps">
              <li v-for="(step, i) in routeSteps" :key="i">{{ step }}</li>
            </ol>
            <p v-else class="text-dim mono">No turn-by-turn data available.</p>

            <p v-if="routePrivacyLine" class="route-privacy">{{ routePrivacyLine }}</p>

            <details style="margin-top:12px;">
              <summary class="mono text-dim" style="cursor:pointer; font-size:11px;">Full route summary (raw)</summary>
              <pre class="route-raw">{{ routeSheet.routeSummaryText }}</pre>
            </details>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import { useSessionStore } from '../../stores/session.js'
import LoadingState from '../../components/LoadingState.vue'
import ErrorState from '../../components/ErrorState.vue'
import EmptyState from '../../components/EmptyState.vue'
import LocalMap from '../../components/LocalMap.vue'

const router = useRouter()
const route = useRoute()
const toast = useToastStore()
const session = useSessionStore()

const incident = ref(null)
const mediaFiles = ref([])
const loading = ref(true)
const loadingMedia = ref(false)
const error = ref('')

const showMedia = ref(false)
const uploading = ref(false)
const mediaError = ref('')
const fileInput = ref(null)

const showModerate = ref(false)
const moderating = ref(false)
const moderateError = ref('')
const moderateForm = ref({ action: 'APPROVE', note: '' })
const reclassifying = ref(false)
const reclassifyForm = ref({ isProtectedCase: false, involvesMinor: false, exactLocation: '', reason: '' })

const showRouteSheet = ref(false)
const creatingRoute = ref(false)
const routeError = ref('')
const routeForm = ref({ resourceId: 0 })
const routeSheet = ref(null)
const shelters = ref([])

const canModerate = computed(() => session.hasRole('MODERATOR', 'ADMIN'))
const canReclassify = computed(() => session.hasRole('MODERATOR', 'ADMIN', 'QUALITY'))
const showReclassify = ref(false)

function formatDate(d) { return d ? new Date(d).toLocaleString() : '—' }

function statusBadge(s) {
  const map = { OPEN: 'badge-amber', CLOSED: 'badge-green', MODERATED: 'badge-blue', REMOVED: 'badge-red' }
  return map[s] || 'badge-gray'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get(`/incidents/${route.params.id}`)
    incident.value = res
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function loadMedia() {
  loadingMedia.value = true
  try {
    const res = await client.get(`/incidents/${route.params.id}/media`)
    mediaFiles.value = Array.isArray(res) ? res : []
  } catch (_) {
    mediaFiles.value = []
  } finally {
    loadingMedia.value = false
  }
}

async function loadShelters() {
  try {
    const res = await client.get('/shelters')
    shelters.value = res || []
  } catch (_) {}
}

async function uploadMedia() {
  const files = fileInput.value?.files
  if (!files || files.length === 0) { mediaError.value = 'Select a file.'; return }
  uploading.value = true
  mediaError.value = ''
  try {
    const fd = new FormData()
    fd.append('file', files[0]) // backend accepts one file per request
    await client.post(`/incidents/${incident.value.id}/media`, fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    toast.success('Media uploaded.')
    showMedia.value = false
    loadMedia()
  } catch (e) {
    mediaError.value = e.message
  } finally {
    uploading.value = false
  }
}

async function reclassify() {
  if (reclassifying.value) return
  reclassifying.value = true
  try {
    const res = await client.post(`/incidents/${incident.value.id}/reclassify`, {
      isProtectedCase: reclassifyForm.value.isProtectedCase,
      involvesMinor: reclassifyForm.value.involvesMinor,
      exactLocation: reclassifyForm.value.exactLocation || null,
      reason: reclassifyForm.value.reason || null,
    })
    incident.value = res
    toast.success('Incident reclassified.')
  } catch (e) {
    toast.error(e.message)
  } finally {
    reclassifying.value = false
  }
}

const ACTION_TO_STATUS = { APPROVE: 'APPROVED', FLAG: 'FLAGGED', REMOVE: 'REMOVED' }

async function moderate() {
  moderating.value = true
  moderateError.value = ''
  try {
    const status = ACTION_TO_STATUS[moderateForm.value.action]
    await client.post(`/incidents/${incident.value.id}/moderate`, { status })
    toast.success('Incident moderated.')
    showModerate.value = false
    load()
  } catch (e) {
    moderateError.value = e.message
  } finally {
    moderating.value = false
  }
}

async function createRouteSheet() {
  // resourceId can legitimately be 0 ("auto-pick nearest"). Only reject null/undefined/"".
  const rid = routeForm.value.resourceId
  if (rid === null || rid === undefined || rid === '') {
    routeError.value = 'Select a shelter or choose "Auto-pick nearest".'
    return
  }
  creatingRoute.value = true
  routeError.value = ''
  try {
    const res = await client.post('/routesheets', {
      incidentId: incident.value.id,
      resourceId: Number(rid),
    })
    showRouteSheet.value = false
    routeSheet.value = res
    toast.success('Route sheet generated.')
  } catch (e) {
    routeError.value = e.message
  } finally {
    creatingRoute.value = false
  }
}

// Derive turn-by-turn steps from the backend's routeSummaryText. We parse the
// "Turn-by-turn:" section rather than re-deriving anything client-side.
const routeSteps = computed(() => {
  const text = routeSheet.value?.routeSummaryText
  if (!text) return []
  const idx = text.indexOf('Turn-by-turn:')
  if (idx < 0) return []
  return text.slice(idx + 'Turn-by-turn:'.length)
    .split('\n')
    .map(l => l.trim())
    .filter(l => l.length > 0 && !l.startsWith('PRIVACY:'))
})

const routePrivacyLine = computed(() => {
  const text = routeSheet.value?.routeSummaryText || ''
  const m = text.match(/PRIVACY:[^\n]*/)
  return m ? m[0] : null
})

onMounted(() => {
  load()
  loadMedia()
  loadShelters()
})
</script>

<style scoped>
.detail-grid { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; }
.section-label { font-size:10px; letter-spacing:0.1em; text-transform:uppercase; color:var(--text-muted); margin-bottom:12px; }
.detail-list { display:flex; flex-direction:column; gap:10px; }
.detail-row { display:flex; justify-content:space-between; gap:12px; padding-bottom:10px; border-bottom:1px solid var(--border); font-size:13px; }
.detail-row:last-child { border-bottom:none; padding-bottom:0; }
dt { color:var(--text-muted); font-size:12px; }
.desc-text { font-size:13px; line-height:1.7; color:var(--text); }
.media-list { list-style:none; display:flex; flex-direction:column; gap:8px; }
.media-item { display:flex; gap:10px; align-items:center; font-size:13px; padding-bottom:8px; border-bottom:1px solid var(--border); }
.media-item:last-child { border-bottom:none; padding-bottom:0; }
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
.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-bottom: 16px;
}
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
.route-print .print-header {
  display: flex; justify-content: space-between; align-items: center;
  font-family: var(--font-display); font-size: 16px; font-weight: 700;
  margin-bottom: 20px; padding-bottom: 12px; border-bottom: 2px solid var(--amber);
}
.route-steps {
  margin: 8px 0 0 0;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  line-height: 1.45;
}
.route-steps li { padding: 2px 0; }
.route-privacy {
  margin-top: 14px;
  padding: 8px 10px;
  border-left: 3px solid var(--amber);
  background: rgba(245, 158, 11, 0.08);
  font-size: 12px;
  color: var(--amber);
  font-family: var(--font-mono, monospace);
}
.route-raw {
  margin: 6px 0 0 0;
  padding: 8px;
  background: var(--bg-elev, #0e1116);
  border: 1px solid var(--border, #334155);
  font-size: 11px;
  white-space: pre-wrap;
  color: var(--text-muted, #94a3b8);
  max-height: 180px;
  overflow: auto;
}
@media print {
  .dialog-backdrop { position: static; background: white; }
  .dialog-box { border: none; box-shadow: none; max-width: none; }
  .dialog-header .btn { display: none; }
  .route-raw, details { display: none; }
}
</style>
