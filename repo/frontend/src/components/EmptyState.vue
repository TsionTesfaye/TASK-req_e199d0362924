<template>
  <div class="empty-state">
    <div class="empty-icon">◎</div>
    <div class="empty-title">{{ title }}</div>
    <div class="empty-msg">{{ message }}</div>
    <slot />
  </div>
</template>

<script setup>
defineProps({
  title:   { type: String, default: 'No data' },
  message: { type: String, default: 'Nothing to display here yet.' },
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 32px;
  color: var(--border-2);
}

.empty-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 16px;
  color: var(--text-muted);
}

.empty-msg {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-dim);
}
</style>
