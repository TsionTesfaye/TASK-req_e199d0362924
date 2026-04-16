/**
 * Shared component tests — Vitest + @vue/test-utils
 * Covers: LoadingState, ErrorState, EmptyState, ArchivedBadge,
 *         SensitiveField, DataTable, ConfirmDialog, LocalMap, App
 *
 * No module mocking. MSW intercepts HTTP at the network level (see setup.js).
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createMemoryHistory } from 'vue-router'
import { http, HttpResponse } from 'msw'
import { server } from './msw/server.js'

/** Access <script setup> reactive state */
const ss = (wrapper) => wrapper.vm.$.setupState

// ── LoadingState ───────────────────────────────────────────────────────────

describe('LoadingState', () => {
  it('renders default label', async () => {
    const { default: C } = await import('../components/LoadingState.vue')
    const w = mount(C)
    expect(w.text()).toContain('Loading...')
  })

  it('renders custom label prop', async () => {
    const { default: C } = await import('../components/LoadingState.vue')
    const w = mount(C, { props: { label: 'Fetching records...' } })
    expect(w.text()).toContain('Fetching records...')
  })

  it('renders spinner element', async () => {
    const { default: C } = await import('../components/LoadingState.vue')
    const w = mount(C)
    expect(w.find('.spinner').exists()).toBe(true)
    expect(w.find('.loading-state').exists()).toBe(true)
  })
})

// ── ErrorState ─────────────────────────────────────────────────────────────

describe('ErrorState', () => {
  it('renders default title and message', async () => {
    const { default: C } = await import('../components/ErrorState.vue')
    const w = mount(C)
    expect(w.text()).toContain('Error')
    expect(w.text()).toContain('Something went wrong.')
  })

  it('renders custom title and message', async () => {
    const { default: C } = await import('../components/ErrorState.vue')
    const w = mount(C, { props: { title: 'Network Error', message: 'Cannot reach server.' } })
    expect(w.text()).toContain('Network Error')
    expect(w.text()).toContain('Cannot reach server.')
  })

  it('shows Retry button when onRetry is provided', async () => {
    const { default: C } = await import('../components/ErrorState.vue')
    const retry = vi.fn()
    const w = mount(C, { props: { onRetry: retry } })
    expect(w.find('button').exists()).toBe(true)
    await w.find('button').trigger('click')
    expect(retry).toHaveBeenCalledOnce()
  })

  it('does not show Retry button when onRetry is null', async () => {
    const { default: C } = await import('../components/ErrorState.vue')
    const w = mount(C)
    expect(w.find('button').exists()).toBe(false)
  })

  it('renders the error icon', async () => {
    const { default: C } = await import('../components/ErrorState.vue')
    const w = mount(C)
    expect(w.find('.error-icon').exists()).toBe(true)
  })
})

// ── EmptyState ─────────────────────────────────────────────────────────────

describe('EmptyState', () => {
  it('renders default title and message', async () => {
    const { default: C } = await import('../components/EmptyState.vue')
    const w = mount(C)
    expect(w.text()).toContain('No data')
    expect(w.text()).toContain('Nothing to display here yet.')
  })

  it('renders custom title and message', async () => {
    const { default: C } = await import('../components/EmptyState.vue')
    const w = mount(C, { props: { title: 'No visits', message: 'No visits found.' } })
    expect(w.text()).toContain('No visits')
    expect(w.text()).toContain('No visits found.')
  })

  it('renders slot content', async () => {
    const { default: C } = await import('../components/EmptyState.vue')
    const w = mount(C, { slots: { default: '<button class="cta-btn">Create First</button>' } })
    expect(w.find('.cta-btn').exists()).toBe(true)
    expect(w.text()).toContain('Create First')
  })

  it('renders empty icon element', async () => {
    const { default: C } = await import('../components/EmptyState.vue')
    const w = mount(C)
    expect(w.find('.empty-icon').exists()).toBe(true)
    expect(w.find('.empty-state').exists()).toBe(true)
  })
})

// ── ArchivedBadge ──────────────────────────────────────────────────────────

describe('ArchivedBadge', () => {
  it('renders Archived text', async () => {
    const { default: C } = await import('../components/ArchivedBadge.vue')
    const w = mount(C)
    expect(w.text()).toContain('Archived')
  })

  it('has archived-badge class', async () => {
    const { default: C } = await import('../components/ArchivedBadge.vue')
    const w = mount(C)
    expect(w.find('.archived-badge').exists()).toBe(true)
  })
})

// ── SensitiveField ─────────────────────────────────────────────────────────

describe('SensitiveField', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    // Default: reveal endpoint returns { phone: '555-0100' }
    server.use(
      http.get('http://localhost/api/patients/:id/reveal', () =>
        HttpResponse.json({ data: { phone: '555-0100' } })
      )
    )
  })

  it('shows masked stars initially', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    expect(w.find('.masked').exists()).toBe(true)
    expect(w.text()).toContain('****')
  })

  it('shows Reveal button initially', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    expect(w.find('.reveal-btn').text()).toContain('Reveal')
  })

  it('calls /patients/:id/reveal when Reveal is clicked', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    let revealCalled = false
    server.use(
      http.get('http://localhost/api/patients/:id/reveal', ({ params }) => {
        if (params.id === '42') revealCalled = true
        return HttpResponse.json({ data: { phone: '555-0100' } })
      })
    )
    const w = mount(C, {
      props: { patientId: 42, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(revealCalled).toBe(true)
  })

  it('shows revealed value after successful API call', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(w.find('.revealed-value').exists()).toBe(true)
    expect(w.text()).toContain('555-0100')
  })

  it('shows full JSON when field prop is not set', async () => {
    server.use(
      http.get('http://localhost/api/patients/:id/reveal', () =>
        HttpResponse.json({ data: { name: 'Alice' } })
      )
    )
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: '' },
      global: { plugins: [createPinia()] },
    })
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(w.find('.revealed-value').exists()).toBe(true)
  })

  it('shows [Reveal failed] on API error', async () => {
    server.use(
      http.get('http://localhost/api/patients/:id/reveal', () =>
        HttpResponse.json({ message: 'Forbidden' }, { status: 403 })
      )
    )
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(w.text()).toContain('[Reveal failed]')
  })

  it('hides revealed value when Hide is clicked', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    // Reveal
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(w.find('.revealed-value').exists()).toBe(true)
    // Hide
    await w.find('.reveal-btn').trigger('click')
    expect(w.find('.masked').exists()).toBe(true)
    expect(w.find('.revealed-value').exists()).toBe(false)
  })

  it('shows watermark text after reveal', async () => {
    const { default: C } = await import('../components/SensitiveField.vue')
    const w = mount(C, {
      props: { patientId: 1, field: 'phone' },
      global: { plugins: [createPinia()] },
    })
    await w.find('.reveal-btn').trigger('click')
    await flushPromises()
    expect(w.find('.watermark').exists()).toBe(true)
  })
})

// ── DataTable ──────────────────────────────────────────────────────────────

describe('DataTable', () => {
  const columns = [
    { key: 'id', label: 'ID' },
    { key: 'name', label: 'Name' },
  ]

  const TABLE_STUBS = {
    LoadingState: { template: '<div class="loading-state-stub" />' },
    ErrorState: { template: '<div class="error-state-stub" />' },
    EmptyState: { template: '<div class="empty-state-stub" />' },
  }

  it('shows LoadingState when loading is true', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const w = mount(C, {
      props: { columns, loading: true },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.find('.loading-state-stub').exists()).toBe(true)
    expect(w.find('.error-state-stub').exists()).toBe(false)
  })

  it('shows ErrorState when error prop is set', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const w = mount(C, {
      props: { columns, error: 'Load failed' },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.find('.error-state-stub').exists()).toBe(true)
    expect(w.find('.loading-state-stub').exists()).toBe(false)
  })

  it('shows EmptyState when rows is empty', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const w = mount(C, {
      props: { columns, rows: [] },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.find('.empty-state-stub').exists()).toBe(true)
  })

  it('renders column headers', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.text()).toContain('ID')
    expect(w.text()).toContain('Name')
  })

  it('renders table rows when rows are provided', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }]
    const w = mount(C, {
      props: { columns, rows },
      global: { stubs: TABLE_STUBS },
    })
    const trs = w.findAll('tbody tr')
    expect(trs).toHaveLength(2)
    expect(w.text()).toContain('Alice')
    expect(w.text()).toContain('Bob')
  })

  it('renders em-dash for null/undefined cell values', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: null }]
    const w = mount(C, {
      props: { columns, rows },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.text()).toContain('—')
  })

  it('applies col.format when provided', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const colsWithFormat = [{ key: 'id', label: 'ID', format: (v) => `#${v}` }]
    const rows = [{ id: 7 }]
    const w = mount(C, {
      props: { columns: colsWithFormat, rows },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.text()).toContain('#7')
  })

  it('emits row-click when a row is clicked', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, onRowClick: vi.fn() },
      global: { stubs: TABLE_STUBS },
    })
    await w.find('tbody tr').trigger('click')
    expect(w.emitted('row-click')).toBeTruthy()
    expect(w.emitted('row-click')[0][0]).toEqual({ id: 1, name: 'Alice' })
  })

  it('shows pagination when meta.total > meta.size', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 1 },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.find('.pagination').exists()).toBe(true)
  })

  it('hides pagination when meta.total <= meta.size', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 5, size: 20 }, page: 1 },
      global: { stubs: TABLE_STUBS },
    })
    expect(w.find('.pagination').exists()).toBe(false)
  })

  it('emits page event with next page number when Next is clicked', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 1 },
      global: { stubs: TABLE_STUBS },
    })
    const buttons = w.findAll('.pagination button')
    await buttons[1].trigger('click') // Next button
    expect(w.emitted('page')[0][0]).toBe(2)
  })

  it('emits page event with prev page number when Prev is clicked', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 2 },
      global: { stubs: TABLE_STUBS },
    })
    const buttons = w.findAll('.pagination button')
    await buttons[0].trigger('click') // Prev button
    expect(w.emitted('page')[0][0]).toBe(1)
  })

  it('computes totalPages correctly', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 1 },
      global: { stubs: TABLE_STUBS },
    })
    expect(ss(w).totalPages).toBe(3) // Math.ceil(50/20) = 3
  })

  it('totalPages is 1 when meta is null', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: null },
      global: { stubs: TABLE_STUBS },
    })
    expect(ss(w).totalPages).toBe(1)
  })

  it('disables Prev button on first page', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 1 },
      global: { stubs: TABLE_STUBS },
    })
    const buttons = w.findAll('.pagination button')
    expect(buttons[0].element.disabled).toBe(true)
    expect(buttons[1].element.disabled).toBe(false)
  })

  it('disables Next button on last page', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const rows = [{ id: 1, name: 'Alice' }]
    const w = mount(C, {
      props: { columns, rows, meta: { total: 50, size: 20 }, page: 3 },
      global: { stubs: TABLE_STUBS },
    })
    const buttons = w.findAll('.pagination button')
    expect(buttons[1].element.disabled).toBe(true)
  })

  it('uses custom emptyTitle and emptyMessage', async () => {
    const { default: C } = await import('../components/DataTable.vue')
    const EmptyCapture = {
      template: '<div class="empty-capture">{{ $props.title }} {{ $props.message }}</div>',
      props: ['title', 'message'],
    }
    const w = mount(C, {
      props: {
        columns,
        rows: [],
        emptyTitle: 'No patients',
        emptyMessage: 'Register a patient first.',
      },
      global: {
        stubs: {
          LoadingState: { template: '<div />' },
          ErrorState: { template: '<div />' },
          EmptyState: EmptyCapture,
        },
      },
    })
    expect(w.find('.empty-capture').text()).toContain('No patients')
    expect(w.find('.empty-capture').text()).toContain('Register a patient first.')
  })
})

// ── ConfirmDialog ──────────────────────────────────────────────────────────

describe('ConfirmDialog', () => {
  it('does not render dialog box when modelValue is false', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: false, title: 'Delete?', message: 'Sure?' },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.dialog-box').exists()).toBe(false)
  })

  it('renders dialog box and content when modelValue is true', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, title: 'Delete?', message: 'Are you sure?' },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.dialog-box').exists()).toBe(true)
    expect(w.text()).toContain('Delete?')
    expect(w.text()).toContain('Are you sure?')
  })

  it('emits update:modelValue false when close (✕) button is clicked', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true },
      global: { stubs: { Teleport: true } },
    })
    await w.find('.dialog-header .btn-ghost').trigger('click')
    expect(w.emitted('update:modelValue')).toBeTruthy()
    expect(w.emitted('update:modelValue')[0][0]).toBe(false)
  })

  it('emits update:modelValue false when Cancel button is clicked', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, cancelLabel: 'Nope' },
      global: { stubs: { Teleport: true } },
    })
    await w.find('.btn-secondary').trigger('click')
    expect(w.emitted('update:modelValue')[0][0]).toBe(false)
  })

  it('emits confirm when confirm button is clicked', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true },
      global: { stubs: { Teleport: true } },
    })
    await w.find('.btn-primary').trigger('click')
    expect(w.emitted('confirm')).toBeTruthy()
    expect(w.emitted('confirm')).toHaveLength(1)
  })

  it('applies btn-danger class when danger prop is true', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, danger: true },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.btn-danger').exists()).toBe(true)
    expect(w.find('.btn-primary').exists()).toBe(false)
  })

  it('applies btn-primary class when danger is false', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, danger: false },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.btn-primary').exists()).toBe(true)
    expect(w.find('.btn-danger').exists()).toBe(false)
  })

  it('disables confirm and cancel when loading is true', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, loading: true },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.btn-secondary').element.disabled).toBe(true)
    expect(w.find('.btn-primary').element.disabled).toBe(true)
  })

  it('disables confirm when confirmDisabled is true', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, confirmDisabled: true },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.btn-primary').element.disabled).toBe(true)
  })

  it('uses custom confirmLabel and cancelLabel', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true, confirmLabel: 'Wipe', cancelLabel: 'Abort' },
      global: { stubs: { Teleport: true } },
    })
    expect(w.text()).toContain('Wipe')
    expect(w.text()).toContain('Abort')
  })

  it('renders slot content inside dialog body', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true },
      slots: { default: '<input class="slot-input" placeholder="Reason" />' },
      global: { stubs: { Teleport: true } },
    })
    expect(w.find('.slot-input').exists()).toBe(true)
  })

  it('emits update:modelValue false when backdrop is clicked', async () => {
    const { default: C } = await import('../components/ConfirmDialog.vue')
    const w = mount(C, {
      props: { modelValue: true },
      global: { stubs: { Teleport: true } },
    })
    await w.find('.dialog-backdrop').trigger('click')
    expect(w.emitted('update:modelValue')[0][0]).toBe(false)
  })
})

// ── LocalMap ───────────────────────────────────────────────────────────────

describe('LocalMap', () => {
  const plainIncident = {
    id: 1,
    neighborhood: 'Downtown',
    protectedCase: false,
    involvesMinor: false,
    nearestCrossStreets: null,
  }

  it('shows empty state when no points exist', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const w = mount(C, { props: { incident: plainIncident, shelters: [] } })
    expect(w.find('.map-empty').exists()).toBe(true)
    expect(w.find('svg').exists()).toBe(false)
  })

  it('displays neighborhood in empty state', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const w = mount(C, { props: { incident: { ...plainIncident, neighborhood: 'Riverside' }, shelters: [] } })
    expect(w.text()).toContain('Riverside')
  })

  it('shows protected-case message when applicable and canRevealExact is false', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, protectedCase: true, nearestCrossStreets: '1st & Main' }
    const w = mount(C, { props: { incident, shelters: [], canRevealExact: false } })
    expect(w.text()).toContain('Exact location hidden')
  })

  it('shows protected-case message for involvesMinor incidents', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, involvesMinor: true, nearestCrossStreets: '5th & Oak' }
    const w = mount(C, { props: { incident, shelters: [], canRevealExact: false } })
    expect(w.text()).toContain('Exact location hidden')
  })

  it('renders SVG map when incident has valid coords and canRevealExact is true', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, protectedCase: true, nearestCrossStreets: '37.7749, -122.4194' }
    const w = mount(C, { props: { incident, shelters: [], canRevealExact: true } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.find('.map-empty').exists()).toBe(false)
  })

  it('renders SVG map when incident has coords and is not protected', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, nearestCrossStreets: '37.77, -122.41' }
    const w = mount(C, { props: { incident, shelters: [] } })
    expect(w.find('svg').exists()).toBe(true)
  })

  it('renders shelter points in the SVG', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, nearestCrossStreets: '37.77, -122.41' }
    const shelters = [{ id: 1, name: 'Safe House A', latitude: 37.78, longitude: -122.42 }]
    const w = mount(C, { props: { incident, shelters } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.text()).toContain('Safe House A')
  })

  it('renders SVG with only shelters when incident has no valid coords', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const shelters = [
      { id: 1, name: 'Shelter One', latitude: 37.78, longitude: -122.42 },
      { id: 2, name: 'Shelter Two', latitude: 37.79, longitude: -122.43 },
    ]
    const w = mount(C, { props: { incident: plainIncident, shelters } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.text()).toContain('Shelter One')
    expect(w.text()).toContain('Shelter Two')
  })

  it('skips shelters without lat/lon', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const shelters = [
      { id: 1, name: 'No Coords' },
      { id: 2, name: 'Has Coords', latitude: 37.78, longitude: -122.42 },
    ]
    const w = mount(C, { props: { incident: plainIncident, shelters } })
    expect(w.find('svg').exists()).toBe(true)
    expect(w.text()).toContain('Has Coords')
    expect(w.text()).not.toContain('No Coords')
  })

  it('ignores invalid coord strings', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, nearestCrossStreets: 'not-valid-coords' }
    const w = mount(C, { props: { incident, shelters: [] } })
    expect(w.find('.map-empty').exists()).toBe(true)
  })

  it('renders INCIDENT label in SVG for incident point', async () => {
    const { default: C } = await import('../components/LocalMap.vue')
    const incident = { ...plainIncident, nearestCrossStreets: '37.77, -122.41' }
    const w = mount(C, { props: { incident, shelters: [] } })
    expect(w.text()).toContain('INCIDENT')
  })
})

// ── App.vue ────────────────────────────────────────────────────────────────

describe('App.vue', () => {
  function makeAppRouter() {
    return createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/login', component: { template: '<div>Login</div>' }, meta: { public: true } },
        { path: '/', component: { template: '<div>Home</div>' } },
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
      ],
    })
  }

  const APP_STUBS = {
    RouterLink: { template: '<a><slot /></a>', props: ['to'] },
    RouterView: { template: '<div class="router-view-stub" />' },
  }

  it('can() returns false when session.user is null', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/login')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    const state = ss(w)
    expect(state.can('ADMIN')).toBe(false)
    expect(state.can('BILLING', 'ADMIN')).toBe(false)
  })

  it('can() returns true when user role matches', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'BILLING', username: 'biller', displayName: 'Billing User' }

    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    const state = ss(w)
    expect(state.can('BILLING')).toBe(true)
    expect(state.can('BILLING', 'ADMIN')).toBe(true)
  })

  it('can() returns false when user role does not match', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'CLINICIAN', username: 'doc', displayName: 'Dr. Smith' }

    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    const state = ss(w)
    expect(state.can('ADMIN')).toBe(false)
    expect(state.can('BILLING')).toBe(false)
  })

  it('isPublicRoute is true for /login', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/login')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(ss(w).isPublicRoute).toBe(true)
  })

  it('isPublicRoute is false for authenticated routes', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(ss(w).isPublicRoute).toBe(false)
  })

  it('renders toast container', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/login')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    expect(w.find('.toast-container').exists()).toBe(true)
  })

  it('renders sidebar and topbar for non-public route', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(w.find('.sidebar').exists()).toBe(true)
    expect(w.find('.topbar').exists()).toBe(true)
  })

  it('does not render sidebar for public route', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = makeAppRouter()
    await router.push('/login')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(w.find('.sidebar').exists()).toBe(false)
  })

  it('displays user role badge and name when logged in', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'ADMIN', username: 'administrator', displayName: 'Admin User' }

    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(w.find('.user-role').text()).toBe('ADMIN')
    expect(w.find('.user-name').text()).toContain('Admin User')
  })

  it('calls session.logout and redirects on Logout click', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const { useSessionStore } = await import('../stores/session.js')
    const session = useSessionStore()
    session.user = { role: 'ADMIN', username: 'adm', displayName: 'Admin' }

    const router = makeAppRouter()
    await router.push('/')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()

    await w.find('button.btn-ghost').trigger('click')
    await flushPromises()

    expect(session.user).toBeNull()
    expect(router.currentRoute.value.path).toBe('/login')
  })

  it('currentRouteName reflects the route name', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/patients', name: 'Patients', component: { template: '<div />' } },
        { path: '/:pathMatch(.*)*', component: { template: '<div />' } },
      ],
    })
    await router.push('/patients')
    const { default: C } = await import('../App.vue')
    const w = mount(C, { global: { plugins: [pinia, router], stubs: APP_STUBS } })
    await flushPromises()
    expect(ss(w).currentRouteName).toBe('Patients')
  })
})
