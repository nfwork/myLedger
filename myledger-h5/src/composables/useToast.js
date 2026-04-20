import { ref } from 'vue'

const message = ref('')
const kind = ref('info')
let timer

export function useToast() {
  function show(text, type = 'info', ms = 2600) {
    message.value = text
    kind.value = type
    clearTimeout(timer)
    timer = setTimeout(() => {
      message.value = ''
    }, ms)
  }

  return { message, kind, show }
}
