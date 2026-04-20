const path = require('path')
const { defineConfig, loadEnv } = require('vite')
const vue = require('@vitejs/plugin-vue')

module.exports = defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_DEV_PROXY_TARGET || 'http://127.0.0.1:8080'

  return {
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
      proxy: {
        '/api': { target, changeOrigin: true },
        '/user': { target, changeOrigin: true },
        '/ledger_settings': { target, changeOrigin: true },
        '/bookkeeping': { target, changeOrigin: true },
        '/report': { target, changeOrigin: true },
      },
    },
  }
})
