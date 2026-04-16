import { http, HttpResponse } from 'msw'

/**
 * Default MSW handlers for all API routes used in Vitest component tests.
 * Tests that need specific responses override these per-test with server.use().
 * All responses use the { data: ... } envelope that client.js's response
 * interceptor unwraps before returning to the component.
 *
 * MSW 2.x + setupServer (Node) requires absolute URLs — path-only patterns
 * like '/api/visits' are NOT resolved against localhost in Node and will not
 * match any request. All handlers use 'http://localhost/api/...' to match
 * the baseURL set in setup.js: client.defaults.baseURL = 'http://localhost/api'.
 */

const BASE = 'http://localhost/api'

export const handlers = [
  // ── Auth ──────────────────────────────────────────────────────────────────
  http.post(`${BASE}/auth/login`, () =>
    HttpResponse.json({
      data: {
        sessionToken: 'tok123',
        csrfToken: 'csrf123',
        user: { id: 1, username: 'admin', role: 'ADMIN' },
      },
    })
  ),
  http.post(`${BASE}/auth/logout`, () => HttpResponse.json({ data: {} })),
  http.post(`${BASE}/bootstrap`, () =>
    HttpResponse.json({
      data: {
        sessionToken: 'boot-tok',
        csrfToken: 'boot-csrf',
        user: { id: 1, username: 'admin', role: 'ADMIN' },
      },
    })
  ),
  http.get(`${BASE}/bootstrap/status`, () =>
    HttpResponse.json({ data: { initialized: false } })
  ),

  // ── Visits ────────────────────────────────────────────────────────────────
  http.get(`${BASE}/visits`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/visits`, () => HttpResponse.json({ data: { id: 'visit-new' } })),
  http.post(`${BASE}/visits/:id/close`, () => HttpResponse.json({ data: {} })),

  // ── Patients ──────────────────────────────────────────────────────────────
  http.get(`${BASE}/patients`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/patients`, () =>
    HttpResponse.json({ data: { id: 'p-new', medicalRecordNumber: 'MRN-001' } })
  ),
  // More-specific paths must appear before /:id to avoid shadowing
  http.get(`${BASE}/patients/:id/reveal`, () =>
    HttpResponse.json({ data: { phone: '555-0100' } })
  ),
  http.get(`${BASE}/patients/:id/verifications`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/patients/:id/verify-identity`, () => HttpResponse.json({ data: {} })),
  http.get(`${BASE}/patients/:id`, () =>
    HttpResponse.json({
      data: {
        id: '123',
        medicalRecordNumber: 'MRN-123',
        dateOfBirth: '1990-01-01',
        sex: 'F',
      },
    })
  ),

  // ── Incidents ─────────────────────────────────────────────────────────────
  http.get(`${BASE}/incidents`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/incidents`, () => HttpResponse.json({ data: { id: 'inc-new' } })),
  http.post(`${BASE}/incidents/:id/moderate`, () =>
    HttpResponse.json({
      data: {
        id: 42,
        category: 'welfare',
        status: 'APPROVED',
        description: 'test desc',
        approximateLocationText: '5th & Main',
        neighborhood: 'DT',
        nearestCrossStreets: '5th',
      },
    })
  ),
  http.get(`${BASE}/incidents/:id`, () =>
    HttpResponse.json({
      data: {
        id: 42,
        category: 'welfare',
        status: 'OPEN',
        description: 'test desc',
        approximateLocationText: '5th & Main',
        neighborhood: 'DT',
        nearestCrossStreets: '5th',
      },
    })
  ),

  // ── Bulletins ─────────────────────────────────────────────────────────────
  http.get(`${BASE}/bulletins`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/bulletins`, () => HttpResponse.json({ data: {} })),
  http.delete(`${BASE}/bulletins/:id`, () => HttpResponse.json({ data: {} })),

  // ── Appointments ──────────────────────────────────────────────────────────
  http.get(`${BASE}/appointments`, () => HttpResponse.json({ data: [] })),

  // ── Invoices / Billing ────────────────────────────────────────────────────
  http.get(`${BASE}/invoices/:id/payments`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/invoices/:id/payments`, () =>
    HttpResponse.json({ data: { id: 'pay-1', amount: 100 } })
  ),
  http.get(`${BASE}/invoices/:id`, () =>
    HttpResponse.json({
      data: {
        id: 'inv-999',
        visitId: 'v-1',
        status: 'OPEN',
        totalAmount: 250.0,
        outstandingAmount: 250.0,
        createdAt: new Date().toISOString(),
      },
    })
  ),
  http.get(`${BASE}/invoices`, () => HttpResponse.json({ data: [] })),
  http.get(`${BASE}/daily-close`, () =>
    HttpResponse.json({ data: { status: 'OPEN', businessDate: '2026-04-15' } })
  ),
  http.post(`${BASE}/daily-close`, () =>
    HttpResponse.json({ data: { status: 'CLOSED' } })
  ),
  http.post(`${BASE}/exports`, () =>
    HttpResponse.json({ data: { result: '{"type":"ledger","rows":5}' } })
  ),
  http.get(`${BASE}/exports/history`, () => HttpResponse.json({ data: [] })),

  // ── Quality ───────────────────────────────────────────────────────────────
  http.get(`${BASE}/quality/results`, () => HttpResponse.json({ data: [] })),
  http.get(`${BASE}/quality/corrective-actions`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/quality/results/:id/override`, () => HttpResponse.json({ data: {} })),
  http.post(`${BASE}/quality/corrective-actions`, () =>
    HttpResponse.json({ data: { id: 'ca-new', status: 'OPEN' } })
  ),
  http.put(`${BASE}/quality/corrective-actions/:id`, () =>
    HttpResponse.json({ data: { status: 'IN_PROGRESS' } })
  ),

  // ── Sampling ──────────────────────────────────────────────────────────────
  http.get(`${BASE}/sampling/runs`, () => HttpResponse.json({ data: [] })),
  http.get(`${BASE}/sampling/runs/:id/visits`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/sampling/runs`, () =>
    HttpResponse.json({ data: { id: 5, status: 'COMPLETE', sampleSize: 3 } })
  ),

  // ── Search ────────────────────────────────────────────────────────────────
  http.get(`${BASE}/search`, () =>
    HttpResponse.json({ data: [{ id: 1, title: 'Incident match', type: 'INCIDENT' }] })
  ),

  // ── Ranking ───────────────────────────────────────────────────────────────
  http.get(`${BASE}/ranking/weights`, () =>
    HttpResponse.json({
      data: { recency: 1, favorites: 2, comments: 1.5, moderatorBoost: 5, coldStartBase: 0.5 },
    })
  ),
  http.put(`${BASE}/ranking/weights`, () =>
    HttpResponse.json({
      data: { recency: 1, favorites: 2, comments: 1.5, moderatorBoost: 5, coldStartBase: 0.5 },
    })
  ),
  http.post(`${BASE}/ranking/promote`, () => HttpResponse.json({ data: {} })),
  http.get(`${BASE}/ranking`, () => HttpResponse.json({ data: [] })),

  // ── Audit ─────────────────────────────────────────────────────────────────
  http.get(`${BASE}/audit`, () => HttpResponse.json({ data: [] })),

  // ── Backups ───────────────────────────────────────────────────────────────
  http.get(`${BASE}/backups`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/backups/run`, () =>
    HttpResponse.json({ data: { id: 'b-1', status: 'COMPLETED' } })
  ),

  // ── Admin ─────────────────────────────────────────────────────────────────
  http.get(`${BASE}/admin/users`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/admin/users`, () =>
    HttpResponse.json({ data: { id: 'u-new', username: 'new_user' } })
  ),
  http.put(`${BASE}/admin/users/:id`, () =>
    HttpResponse.json({ data: { id: 'u-1', username: 'existing' } })
  ),
  http.delete(`${BASE}/admin/users/:id`, () => HttpResponse.json({ data: {} })),

  // ── Auth (current user) ───────────────────────────────────────────────────
  http.get(`${BASE}/auth/me`, () =>
    HttpResponse.json({ data: { id: 1, username: 'admin', role: 'ADMIN' } })
  ),

  // ── Health ────────────────────────────────────────────────────────────────
  http.get(`${BASE}/health`, () => HttpResponse.json({ data: { status: 'ok' } })),

  // ── Invoice refunds / void ────────────────────────────────────────────────
  http.post(`${BASE}/invoices/:id/refunds`, () => HttpResponse.json({ data: {} })),
  http.post(`${BASE}/invoices/:id/void`, () => HttpResponse.json({ data: {} })),

  // ── Incident media / shelters / route sheets / reclassify ─────────────────
  http.get(`${BASE}/incidents/:id/media`, () => HttpResponse.json({ data: [] })),
  http.get(`${BASE}/shelters`, () => HttpResponse.json({ data: [] })),
  http.post(`${BASE}/incidents/:id/reclassify`, () =>
    HttpResponse.json({ data: { id: 42, category: 'safety' } })
  ),
  http.post(`${BASE}/routesheets`, () =>
    HttpResponse.json({ data: { id: 'rs-1', steps: 'Step 1: ...', privacyNote: '' } })
  ),

  // ── Patient archive / visits (per-patient) ────────────────────────────────
  http.delete(`${BASE}/patients/:id/archive`, () => HttpResponse.json({ data: {} })),

  // ── Backups restore-test ──────────────────────────────────────────────────
  http.post(`${BASE}/backups/:id/restore-test`, () => HttpResponse.json({ data: {} })),

  // ── Favorites (used by BulletinView.toggleFav) ───────────────────────────
  http.post(`${BASE}/favorites`, () => HttpResponse.json({ data: {} })),
  http.delete(`${BASE}/favorites`, () => HttpResponse.json({ data: {} })),
]
