<template>
  <div class="error-state">
    <div class="error-icon">⚠</div>
    <div class="error-title">{{ title }}</div>
    <div class="error-msg">{{ message }}</div>
    <button v-if="onRetry" class="btn btn-secondary btn-sm" @click="onRetry">Retry</button>
  </div>
</template>

<script setup>
defineProps({
  title:   { type: String, default: 'Error' },
  message: { type: String, default: 'Something went wrong.' },
  onRetry: { type: Function, default: null },
})
</script>

<style scoped>
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 60px 20px;
  text-align: center;
}

.error-icon {
  font-size: 28px;
  color: var(--red);
}

.error-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 16px;
  color: var(--text);
}

.error-msg {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-muted);
  max-width: 360px;
}
</style>
