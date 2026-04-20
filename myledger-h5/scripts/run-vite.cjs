/**
 * Windows：若主 node_modules 里 esbuild 平台包损坏或被锁（.esbuild-windows-64.DELETE），
 * 则把 esbuild-windows-64 装到 scripts/.esbuild-cache 并设置 ESBUILD_BINARY_PATH，再启动 Vite。
 */
var path = require('path')
var fs = require('fs')
var spawnSync = require('child_process').spawnSync

var projectRoot = path.join(__dirname, '..')
var viteCli = path.join(projectRoot, 'node_modules', 'vite', 'bin', 'vite.js')
var args = process.argv.slice(2)

if (!fs.existsSync(viteCli)) {
  console.error('[myledger-h5] 未找到 vite，请在 myledger-h5 目录执行 npm install')
  process.exit(1)
}

function runVite() {
  var r = spawnSync(process.execPath, [viteCli].concat(args), {
    cwd: projectRoot,
    stdio: 'inherit',
    env: process.env,
  })
  process.exit(r.status === null ? 1 : r.status)
}

if (process.platform !== 'win32') {
  runVite()
}

var winBin = path.join(projectRoot, 'node_modules', 'esbuild-windows-64', 'esbuild.exe')
var cacheRoot = path.join(__dirname, '.esbuild-cache')
var cacheBin = path.join(cacheRoot, 'node_modules', 'esbuild-windows-64', 'esbuild.exe')

function findEsbuildExe() {
  if (process.env.ESBUILD_BINARY_PATH && fs.existsSync(process.env.ESBUILD_BINARY_PATH)) {
    return process.env.ESBUILD_BINARY_PATH
  }
  /** 优先用侧路缓存，避免主 node_modules 下 .esbuild-windows-64.DELETE 被锁导致装不全 */
  if (fs.existsSync(cacheBin)) return cacheBin
  if (fs.existsSync(winBin)) return winBin
  return null
}

function installEsbuildToCache() {
  if (!fs.existsSync(cacheRoot)) fs.mkdirSync(cacheRoot, { recursive: true })
  var r = spawnSync(
    'npm',
    ['install', 'esbuild-windows-64@0.14.54', '--no-save', '--ignore-scripts', '--prefix', cacheRoot],
    { cwd: projectRoot, stdio: 'inherit', shell: true, env: process.env }
  )
  return r.status === 0 && fs.existsSync(cacheBin)
}

var bin = findEsbuildExe()
if (!bin) {
  console.log('[myledger-h5] 未找到 esbuild-windows-64，正在安装到 scripts/.esbuild-cache …')
  if (!installEsbuildToCache()) {
    console.error('[myledger-h5] 无法安装 esbuild-windows-64。请关闭所有 node/vite 进程后：')
    console.error('  删除 myledger-h5\\node_modules（含 .esbuild-windows-64.DELETE）再执行 npm install')
    console.error('或设置环境变量 ESBUILD_BINARY_PATH 为本机 esbuild.exe 的绝对路径')
    process.exit(1)
  }
  bin = cacheBin
}

process.env.ESBUILD_BINARY_PATH = path.resolve(bin)
runVite()
