const path = require('path')
const { defineConfig } = require('vite')
const vue = require('@vitejs/plugin-vue')

/**
 * 使用 `vite.config.js` 而非 `.cjs`：Vite 2 在 Node 较新版本上加载打包后的配置时，
 * `require.extensions['.cjs']` 可能为 undefined，会报 defaultLoader is not a function。
 * 解析顺序为 .js → .mjs → .ts → .cjs，故 .js 优先且与 package.json 未声明 type:module 时 CJS 语义一致。
 */
module.exports = defineConfig({
  /** 显式传入与 `vue` 同版本的 `@vue/compiler-sfc`（当前锁 3.2.x，避免 3.5+ 产物含 `?.` / 私有字段导致 Node 12 解析失败）；勿在文件顶层 require（会与 Vite 加载配置的钩子冲突） */
  plugins: [vue({ compiler: require('@vue/compiler-sfc') })],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  /**
   * Node 12.9 的 fs.rmdir 不支持 (path, { recursive: true }, cb)；Vite 2 预构建会用到该写法并报 ERR_INVALID_CALLBACK。
   * 关闭依赖预构建以兼容 12.9；若使用 Node >=12.10 可改为 false 以加快冷启动。
   */
  optimizeDeps: {
    disabled: true,
  },
  server: {
    port: 5173,
    /** 监听 0.0.0.0，便于局域网用本机 IP（如 192.168.0.156）访问 */
    host: true,
  },
})
