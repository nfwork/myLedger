<template>
  <div class="app-root">
    <div class="app-shell">
      <RouterView />
      <UiToast />
    </div>
  </div>
</template>

<script setup>
import UiToast from '@/components/UiToast.vue'
</script>

<style scoped>
.app-root {
  min-height: 100dvh;
  background: radial-gradient(120% 80% at 50% -10%, #ccfbf1 0%, var(--bg) 45%, var(--bg) 100%);
}
.app-shell {
  min-height: 100dvh;
  width: 100%;
}
@media (min-width: 720px) {
  .app-root {
    display: flex;
    flex-direction: column;
    align-items: center;
  }
  .app-shell {
    max-width: var(--app-shell-max, 600px);
    min-height: 100dvh;
    background: var(--bg);
    box-shadow: var(--shadow-lg);
    border-left: 1px solid rgb(13 148 136 / 0.08);
    border-right: 1px solid rgb(13 148 136 / 0.08);
  }
}
</style>
