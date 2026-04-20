var m = process.version.match(/^v(\d+)\.(\d+)/)
var major = m ? parseInt(m[1], 10) : 0
var minor = m ? parseInt(m[2], 10) : 0

var ok = major > 12 || (major === 12 && minor >= 9)

if (!ok) {
  console.error('[myledger-h5] 需要 Node.js >= 12.9.0，当前为 ' + process.version)
  console.error('请从 https://nodejs.org/ 安装或升级后重开终端，再执行 npm run dev。')
  process.exit(1)
}
