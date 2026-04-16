<template>
  <Teleport to="body">
    <div v-if="modelValue" class="dialog-backdrop" @click.self="emit('update:modelValue', false)">
      <div class="dialog-box fade-in">
        <div class="dialog-header">
          <span class="dialog-title">{{ title }}</span>
          <button class="btn btn-ghost btn-sm" @click="emit('update:modelValue', false)">✕</button>
        </div>
        <div class="dialog-body">
          <p v-if="message" class="dialog-message">{{ message }}</p>
          <slot />
        </div>
        <div class="dialog-footer">
          <button class="btn btn-secondary" @click="emit('update:modelValue', false)" :disabled="loading">
            {{ cancelLabel }}
          </button>
          <button
            class="btn"
            :class="danger ? 'btn-danger' : 'btn-primary'"
            @click="confirm"
            :disabled="loading || confirmDisabled"
          >
            <span v-if="loading" class="spinner-sm" />
            {{ confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
const props = defineProps({
  modelValue:     { type: Boolean, required: true },
  title:          { type: String, default: 'Confirm' },
  message:        { type: String, default: '' },
  confirmLabel:   { type: String, default: 'Confirm' },
  cancelLabel:    { type: String, default: 'Cancel' },
  danger:         { type: Boolean, default: false },
  loading:        { type: Boolean, default: false },
  confirmDisabled:{ type: Boolean, default: false },
})

const emit = defineEmits(['update:modelValue', 'confirm'])

function confirm() {
  emit('confirm')
}
</script>

<style scoped>
.dialog-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9000;
  backdrop-filter: blur(2px);
}

.dialog-box {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: var(--radius-lg);
  width: 100%;
  max-width: 460px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.6);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--border);
}

.dialog-title {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 15px;
}

.dialog-body {
  padding: 20px;
}

.dialog-message {
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 20px;
  border-top: 1px solid var(--border);
}

.spinner-sm {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(0,0,0,0.3);
  border-top-color: #000;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
</style>
