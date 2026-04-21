<template>
  <nav class="tab-bar" aria-label="主导航">
    <RouterLink class="tab" to="/dashboard" active-class="is-active">
      <span class="ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M4 10.5V19a1 1 0 001 1h5v-6H9v-3h2V9H4v1.5zm16 0V9h-7v2h2v3h-2v6h5a1 1 0 001-1v-8.5a1 1 0 00-.4-.8L13 5.1a1 1 0 00-1.2 0l-5.6 4.4a1 1 0 00-.2.2z"/></svg>
      </span>
      <span>概览</span>
    </RouterLink>
    <RouterLink class="tab" to="/entries" active-class="is-active">
      <span class="ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M4 4h16v2H4V4zm2 4h12v12H6V8zm2 2v8h8v-8H8zm2 2h4v4h-4v-4z"/></svg>
      </span>
      <span>流水</span>
    </RouterLink>
    <RouterLink class="tab tab-center" to="/entry/new">
      <span class="fab" aria-hidden="true">+</span>
      <span class="lbl">记账</span>
    </RouterLink>
    <RouterLink class="tab" to="/stats" active-class="is-active">
      <span class="ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M4 19h3v-9H4v9zm5 0h3V5H9v14zm5 0h3v-7h-3v7zm5 0h3V9h-3v10z"/></svg>
      </span>
      <span>统计</span>
    </RouterLink>
    <RouterLink class="tab" to="/profile" active-class="is-active">
      <span class="ico" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="22" height="22"><path fill="currentColor" d="M12 12a4 4 0 100-8 4 4 0 000 8zm-8 8a8 8 0 1116 0H4z"/></svg>
      </span>
      <span>我的</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  align-items: end;
  padding: 0.35rem 0.25rem calc(0.35rem + env(safe-area-inset-bottom, 0px));
  background: linear-gradient(180deg, rgb(255 255 255 / 0.92), #fff 30%);
  border-top: 1px solid var(--line);
  box-shadow: 0 -8px 32px rgb(15 118 110 / 0.06);
}
.tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
  padding: 0.25rem 0;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--muted);
  text-decoration: none;
  border-radius: 12px;
  transition: color 0.15s ease, background 0.15s ease;
}
.tab .ico {
  display: flex;
  opacity: 0.85;
}
.tab.is-active {
  color: var(--primary);
  background: rgb(13 148 136 / 0.08);
}
.tab-center {
  position: relative;
  padding-bottom: 0.5rem;
}
.fab {
  width: 3rem;
  height: 3rem;
  margin-top: -1.35rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(145deg, #14b8a6, #0d9488);
  color: #fff;
  font-size: 1.75rem;
  font-weight: 300;
  line-height: 1;
  box-shadow: 0 10px 24px rgb(13 148 136 / 0.45);
}
.tab-center.router-link-active .fab {
  background: linear-gradient(145deg, #0f766e, #0d9488);
}
.lbl {
  margin-top: 0.1rem;
  font-size: 0.65rem;
  font-weight: 600;
  color: var(--muted);
}
.tab-center.router-link-active .lbl {
  color: var(--primary);
}

@media (min-width: 720px) {
  .tab-bar {
    left: 50%;
    right: auto;
    width: min(var(--app-shell-max, 500px), 100%);
    max-width: var(--app-shell-max, 500px);
    transform: translateX(-50%);
    border-left: 1px solid rgb(13 148 136 / 0.08);
    border-right: 1px solid rgb(13 148 136 / 0.08);
  }
}
</style>
