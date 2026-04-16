<template>
  <div>
    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" :on-retry="onRetry" />
    <template v-else>
      <div v-if="rows.length === 0">
        <EmptyState :title="emptyTitle" :message="emptyMessage" />
      </div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.key">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in rows" :key="row.id ?? i" @click="emit('row-click', row)" :class="{ clickable: !!onRowClick }">
              <td v-for="col in columns" :key="col.key">
                <slot :name="col.key" :row="row">
                  {{ col.format ? col.format(row[col.key], row) : (row[col.key] ?? '—') }}
                </slot>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="meta && meta.total > meta.size" class="pagination">
        <button class="btn btn-ghost btn-sm" :disabled="page <= 1" @click="emit('page', page - 1)">← Prev</button>
        <span class="page-info mono">
          {{ page }} / {{ totalPages }}
          <span class="text-muted"> — {{ meta.total }} total</span>
        </span>
        <button class="btn btn-ghost btn-sm" :disabled="page >= totalPages" @click="emit('page', page + 1)">Next →</button>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import LoadingState from './LoadingState.vue'
import ErrorState from './ErrorState.vue'
import EmptyState from './EmptyState.vue'

const props = defineProps({
  columns:      { type: Array, required: true },
  rows:         { type: Array, default: () => [] },
  loading:      { type: Boolean, default: false },
  error:        { type: String, default: null },
  meta:         { type: Object, default: null },
  page:         { type: Number, default: 1 },
  emptyTitle:   { type: String, default: 'No records' },
  emptyMessage: { type: String, default: 'No data to display.' },
  onRetry:      { type: Function, default: null },
  onRowClick:   { type: Function, default: null },
})

const emit = defineEmits(['page', 'row-click'])

const totalPages = computed(() => {
  if (!props.meta) return 1
  return Math.ceil(props.meta.total / props.meta.size) || 1
})
</script>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}

.page-info {
  font-size: 11px;
  color: var(--text-muted);
}

tr.clickable { cursor: pointer; }
</style>
