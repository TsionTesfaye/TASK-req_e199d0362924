<template>
  <div class="loading-state">
    <div class="spinner" />
    <span class="loading-text">{{ label }}</span>
  </div>
</template>

<script setup>
defineProps({
  label: { type: String, default: 'Loading...' },
})
</script>

<style scoped>
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 60px 20px;
  color: var(--text-muted);
}

.spinner {
  width: 28px;
  height: 28px;
  border: 2px solid var(--border-2);
  border-top-color: var(--amber);
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.loading-text {
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
</style>
