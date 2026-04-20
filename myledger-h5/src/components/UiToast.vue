<template>
  <Teleport to="body">
    <Transition name="toast">
      <div v-if="message" class="ui-toast" :class="'is-' + kind" role="status">
        {{ message }}
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { useToast } from '@/composables/useToast'

const { message, kind } = useToast()
</script>

<style scoped>
.ui-toast {
  position: fixed;
  left: 50%;
  bottom: calc(5.5rem + env(safe-area-inset-bottom, 0px));
  transform: translateX(-50%);
  z-index: 9999;
  max-width: min(90vw, 22rem);
  padding: 0.65rem 1rem;
  border-radius: 999px;
  font-size: 0.875rem;
  font-weight: 500;
  box-shadow: 0 10px 40px rgb(15 23 42 / 0.18);
  backdrop-filter: blur(8px);
}
.ui-toast.is-info {
  background: rgb(15 23 42 / 0.88);
  color: #f8fafc;
}
.ui-toast.is-error {
  background: rgb(190 18 60 / 0.95);
  color: #fff;
}
.ui-toast.is-success {
  background: rgb(4 120 87 / 0.95);
  color: #ecfdf5;
}
.toast-enter-active,
.toast-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translate(-50%, 8px);
}
</style>
