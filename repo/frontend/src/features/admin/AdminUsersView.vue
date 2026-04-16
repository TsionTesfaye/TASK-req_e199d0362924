<template>
  <div class="page fade-in">
    <div class="page-header">
      <div>
        <h1 class="page-title">User Management</h1>
        <p class="page-subtitle">Create and manage operator accounts</p>
      </div>
      <button class="btn btn-primary" @click="showCreate = true">+ New User</button>
    </div>

    <div class="card">
      <DataTable
        :columns="columns"
        :rows="users"
        :loading="loading"
        :error="error"
        :on-retry="load"
        empty-title="No users"
        empty-message="No user accounts found."
      >
        <template #role="{ row }">
          <span class="badge badge-amber">{{ row.role }}</span>
        </template>
        <template #createdAt="{ row }">
          <span class="mono">{{ formatDate(row.createdAt) }}</span>
        </template>
        <template #actions="{ row }">
          <button class="btn btn-ghost btn-sm" @click="openEdit(row)">Edit</button>
          <button class="btn btn-danger btn-sm" @click="confirmDelete(row)">Delete</button>
        </template>
      </DataTable>
    </div>

    <!-- Create user dialog -->
    <Teleport to="body">
      <div v-if="showCreate" class="dialog-backdrop" @click.self="showCreate = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">New User</span>
            <button class="btn btn-ghost btn-sm" @click="showCreate = false">✕</button>
          </div>
          <form @submit.prevent="createUser" class="dialog-body">
            <div v-if="createError" class="form-error">{{ createError }}</div>
            <div class="form-grid">
              <div class="field">
                <label>Username *</label>
                <input v-model="createForm.username" required autocomplete="off" />
              </div>
              <div class="field">
                <label>Display Name</label>
                <input v-model="createForm.displayName" />
              </div>
              <div class="field">
                <label>Password *</label>
                <input v-model="createForm.password" type="password" required autocomplete="new-password" />
              </div>
              <div class="field">
                <label>Role *</label>
                <select v-model="createForm.role" required>
                  <option v-for="r in ROLES" :key="r" :value="r">{{ r }}</option>
                </select>
              </div>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showCreate = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="creating">
                <span v-if="creating" class="spinner-sm" /> Create
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <!-- Edit user dialog -->
    <Teleport to="body">
      <div v-if="showEdit && editTarget" class="dialog-backdrop" @click.self="showEdit = false">
        <div class="dialog-box fade-in">
          <div class="dialog-header">
            <span class="dialog-title">Edit User</span>
            <button class="btn btn-ghost btn-sm" @click="showEdit = false">✕</button>
          </div>
          <form @submit.prevent="saveEdit" class="dialog-body">
            <div v-if="editError" class="form-error">{{ editError }}</div>
            <div class="form-grid">
              <div class="field">
                <label>Display Name</label>
                <input v-model="editForm.displayName" />
              </div>
              <div class="field">
                <label>Role</label>
                <select v-model="editForm.role">
                  <option v-for="r in ROLES" :key="r" :value="r">{{ r }}</option>
                </select>
              </div>
            </div>
            <div class="dialog-footer">
              <button type="button" class="btn btn-secondary" @click="showEdit = false">Cancel</button>
              <button type="submit" class="btn btn-primary" :disabled="saving">
                <span v-if="saving" class="spinner-sm" /> Save
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      v-model="deleteDialog.show"
      title="Delete User"
      :message="`Delete user ${deleteDialog.user?.username}? This cannot be undone.`"
      confirm-label="Delete"
      :danger="true"
      :loading="deleteDialog.loading"
      @confirm="doDelete"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import client from '../../api/client.js'
import { useToastStore } from '../../stores/toast.js'
import DataTable from '../../components/DataTable.vue'
import ConfirmDialog from '../../components/ConfirmDialog.vue'

const toast = useToastStore()
const ROLES = ['FRONT_DESK', 'CLINICIAN', 'BILLING', 'QUALITY', 'MODERATOR', 'ADMIN']

const users = ref([])
const loading = ref(false)
const error = ref('')

const showCreate = ref(false)
const creating = ref(false)
const createError = ref('')
const createForm = ref({ username: '', displayName: '', password: '', role: 'FRONT_DESK' })

const showEdit = ref(false)
const editTarget = ref(null)
const saving = ref(false)
const editError = ref('')
const editForm = ref({ displayName: '', role: '', isActive: true, isFrozen: false })

const deleteDialog = ref({ show: false, user: null, loading: false })

const columns = [
  { key: 'username',    label: 'Username' },
  { key: 'displayName', label: 'Display Name', format: v => v || '—' },
  { key: 'role',        label: 'Role' },
  { key: 'createdAt',   label: 'Created' },
  { key: 'actions',     label: '' },
]

function formatDate(d) { return d ? new Date(d).toLocaleDateString() : '—' }

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await client.get('/admin/users')
    users.value = res || []
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function createUser() {
  creating.value = true
  createError.value = ''
  try {
    await client.post('/admin/users', createForm.value)
    toast.success('User created.')
    showCreate.value = false
    createForm.value = { username: '', displayName: '', password: '', role: 'FRONT_DESK' }
    load()
  } catch (e) {
    createError.value = e.message
  } finally {
    creating.value = false
  }
}

function openEdit(user) {
  editTarget.value = user
  editForm.value = {
    displayName: user.displayName || '',
    role: user.role,
    isActive: user.isActive ?? true,
    isFrozen: user.isFrozen ?? false,
  }
  showEdit.value = true
}

async function saveEdit() {
  saving.value = true
  editError.value = ''
  try {
    await client.put(`/admin/users/${editTarget.value.id}`, editForm.value)
    toast.success('User updated.')
    showEdit.value = false
    load()
  } catch (e) {
    editError.value = e.message
  } finally {
    saving.value = false
  }
}

function confirmDelete(user) {
  deleteDialog.value = { show: true, user, loading: false }
}

async function doDelete() {
  deleteDialog.value.loading = true
  try {
    await client.delete(`/admin/users/${deleteDialog.value.user.id}`)
    toast.success('User deleted.')
    deleteDialog.value.show = false
    load()
  } catch (e) {
    toast.error(e.message)
    deleteDialog.value.show = false
  }
}

onMounted(() => load())
</script>

<style scoped>
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.form-error {
  background: var(--red-dim); border: 1px solid var(--red); border-radius: var(--radius);
  padding: 10px 14px; font-family: var(--font-mono); font-size: 12px; color: var(--red); margin-bottom: 16px;
}
.dialog-backdrop {
  position: fixed; inset: 0; background: rgba(0,0,0,0.7);
  display: flex; align-items: center; justify-content: center; z-index: 9000; backdrop-filter: blur(2px);
}
.dialog-box {
  background: var(--surface); border: 1px solid var(--border-2); border-radius: var(--radius-lg);
  width: 100%; max-width: 500px; box-shadow: 0 20px 60px rgba(0,0,0,0.6);
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
.spinner-sm {
  display: inline-block; width: 12px; height: 12px;
  border: 2px solid rgba(0,0,0,0.3); border-top-color: #000;
  border-radius: 50%; animation: spin 0.6s linear infinite;
}
</style>
