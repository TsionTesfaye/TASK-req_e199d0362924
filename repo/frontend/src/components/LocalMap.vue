<script setup>
/**
 * Local-only SVG map (no external tile providers).
 * Plots shelters and an incident pin in normalized lat/lon space.
 * Falls back to neighborhood-only display when the incident has no coords
 * AND the viewer is not allowed to see the exact location.
 */
import { computed } from 'vue'

const props = defineProps({
  incident: { type: Object, required: true },
  shelters: { type: Array, default: () => [] },
  canRevealExact: { type: Boolean, default: false },
})

function parseLatLon(s) {
  if (!s) return null
  const m = String(s).trim().match(/^(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)$/)
  return m ? [parseFloat(m[1]), parseFloat(m[2])] : null
}

const incidentCoords = computed(() => {
  if ((props.incident.protectedCase || props.incident.involvesMinor) && !props.canRevealExact) return null
  return parseLatLon(props.incident.nearestCrossStreets)
})

const points = computed(() => {
  const pts = []
  for (const s of props.shelters) {
    if (s.latitude != null && s.longitude != null) {
      pts.push({ kind: 'shelter', id: s.id, name: s.name, lat: +s.latitude, lon: +s.longitude })
    }
  }
  if (incidentCoords.value) {
    pts.push({ kind: 'incident', id: 'incident', name: 'Incident', lat: incidentCoords.value[0], lon: incidentCoords.value[1] })
  }
  return pts
})

const bounds = computed(() => {
  if (points.value.length === 0) return null
  let minLat = Infinity, maxLat = -Infinity, minLon = Infinity, maxLon = -Infinity
  for (const p of points.value) {
    if (p.lat < minLat) minLat = p.lat
    if (p.lat > maxLat) maxLat = p.lat
    if (p.lon < minLon) minLon = p.lon
    if (p.lon > maxLon) maxLon = p.lon
  }
  // pad
  const padLat = Math.max((maxLat - minLat) * 0.15, 0.005)
  const padLon = Math.max((maxLon - minLon) * 0.15, 0.005)
  return { minLat: minLat - padLat, maxLat: maxLat + padLat, minLon: minLon - padLon, maxLon: maxLon + padLon }
})

function project(p) {
  const W = 600, H = 360
  const b = bounds.value
  const x = ((p.lon - b.minLon) / (b.maxLon - b.minLon)) * W
  const y = H - ((p.lat - b.minLat) / (b.maxLat - b.minLat)) * H
  return { x, y }
}
</script>

<template>
  <div class="local-map">
    <div v-if="!bounds" class="map-empty">
      <div class="mono section-label">Local map</div>
      <p>No coordinates available for this incident or any shelter.</p>
      <p v-if="incident.neighborhood">Neighborhood: <strong>{{ incident.neighborhood }}</strong></p>
      <p v-if="(incident.protectedCase || incident.involvesMinor) && !canRevealExact" class="muted">
        Exact location hidden (protected case).
      </p>
    </div>
    <svg v-else :viewBox="`0 0 600 360`" xmlns="http://www.w3.org/2000/svg" class="map-svg" aria-label="Local map">
      <rect x="0" y="0" width="600" height="360" fill="#0e1116" />
      <!-- gridlines for visual scale -->
      <g stroke="#222a33" stroke-width="0.5">
        <line x1="0" y1="90" x2="600" y2="90" />
        <line x1="0" y1="180" x2="600" y2="180" />
        <line x1="0" y1="270" x2="600" y2="270" />
        <line x1="150" y1="0" x2="150" y2="360" />
        <line x1="300" y1="0" x2="300" y2="360" />
        <line x1="450" y1="0" x2="450" y2="360" />
      </g>
      <g v-for="p in points" :key="p.kind + ':' + p.id">
        <template v-if="p.kind === 'shelter'">
          <circle :cx="project(p).x" :cy="project(p).y" r="6" fill="#4ec9b0" stroke="#fff" stroke-width="1" />
          <text :x="project(p).x + 8" :y="project(p).y + 4" fill="#cbd5e1" font-size="11" font-family="monospace">{{ p.name }}</text>
        </template>
        <template v-else>
          <circle :cx="project(p).x" :cy="project(p).y" r="8" fill="#f59e0b" stroke="#fff" stroke-width="2" />
          <text :x="project(p).x + 10" :y="project(p).y + 4" fill="#fbbf24" font-size="12" font-family="monospace" font-weight="bold">INCIDENT</text>
        </template>
      </g>
      <g font-family="monospace" font-size="10" fill="#94a3b8">
        <rect x="8" y="8" width="170" height="42" fill="#0e1116" stroke="#334155" />
        <circle cx="20" cy="20" r="5" fill="#4ec9b0" /><text x="32" y="24">shelter / resource</text>
        <circle cx="20" cy="38" r="6" fill="#f59e0b" /><text x="32" y="42">incident</text>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.local-map { width: 100%; }
.map-svg { width: 100%; max-width: 720px; border: 1px solid #334155; border-radius: 6px; }
.map-empty { padding: 16px; border: 1px dashed #334155; border-radius: 6px; color: #94a3b8; }
.muted { color: #94a3b8; font-size: 12px; }
</style>
