<template>
  <div class="main-layout">
    <header v-if="title" class="page-head">
      <button v-if="showBack" type="button" class="back" @click="goBack" aria-label="返回">‹</button>
      <h1>{{ title }}</h1>
    </header>
    <div class="page-body" :class="{ 'has-head': !!title }">
      <RouterView />
    </div>
    <TabBar v-if="!hideTab" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import TabBar from '@/components/TabBar.vue'

const route = useRoute()
const router = useRouter()

const leaf = computed(() => route.matched[route.matched.length - 1] || route)
const title = computed(() => leaf.value?.meta?.title || '')
const showBack = computed(() => leaf.value?.meta?.showBack === true)
const hideTab = computed(() => leaf.value?.meta?.hideTab === true)

function goBack() {
  router.back()
}
</script>

<style scoped>
.main-layout {
  /* 固定为视口高度，避免整页滚动把顶栏带走；中间 .page-body 单独滚动 */
  height: 100dvh;
  max-height: 100dvh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--bg);
  color: var(--text);
}
.page-head {
  flex-shrink: 0;
  position: relative;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.65rem 1rem calc(0.65rem + env(safe-area-inset-top, 0px));
  padding-top: calc(0.65rem + env(safe-area-inset-top, 0px));
  background: linear-gradient(135deg, #0f766e 0%, #0d9488 55%, #14b8a6 100%);
  color: #ecfdf5;
  box-shadow: 0 4px 24px rgb(13 148 136 / 0.25);
}
.page-head h1 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.back {
  border: none;
  background: rgb(255 255 255 / 0.15);
  color: #fff;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: 12px;
  font-size: 1.5rem;
  line-height: 1;
  cursor: pointer;
}
.page-body {
  flex: 1;
  min-height: 0;
  padding: 1rem 1rem 5.5rem;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}
.page-body.has-head {
  padding-top: 0.75rem;
}
</style>
