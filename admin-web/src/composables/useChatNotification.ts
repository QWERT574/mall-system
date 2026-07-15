import { ref } from 'vue'

export function useChatNotification() {
  const notificationEnabled = ref(false)
  const soundEnabled = ref(true)
  let audioCtx: AudioContext | null = null

  function requestPermission() {
    if (!('Notification' in window)) return

    Notification.requestPermission().then(permission => {
      notificationEnabled.value = permission === 'granted'
    })
  }

  function showNotification(title: string, body: string, icon?: string) {
    if (!notificationEnabled.value) return

    try {
      const notification = new Notification(title, {
        body,
        icon: icon || '/favicon.ico',
        tag: 'chat-message'
      })

      notification.onclick = () => {
        window.focus()
        notification.close()
      }

      setTimeout(() => notification.close(), 5000)
    } catch (e) {
      console.warn('[Notification] Failed to show:', e)
    }
  }

  function playSound() {
    if (!soundEnabled.value) return

    try {
      if (!audioCtx) {
        audioCtx = new AudioContext()
      }

      const oscillator = audioCtx.createOscillator()
      const gainNode = audioCtx.createGain()

      oscillator.connect(gainNode)
      gainNode.connect(audioCtx.destination)

      oscillator.frequency.value = 880
      oscillator.type = 'sine'

      gainNode.gain.setValueAtTime(0.1, audioCtx.currentTime)
      gainNode.gain.exponentialRampToValueAtTime(0.001, audioCtx.currentTime + 0.15)

      oscillator.start(audioCtx.currentTime)
      oscillator.stop(audioCtx.currentTime + 0.15)
    } catch (e) {
      console.warn('[Sound] Failed to play:', e)
    }
  }

  function flashTitle(message: string) {
    const originalTitle = document.title
    let count = 0
    const maxFlashes = 6

    const interval = setInterval(() => {
      document.title = count % 2 === 0 ? message : originalTitle
      count++
      if (count >= maxFlashes) {
        clearInterval(interval)
        document.title = originalTitle
      }
    }, 800)
  }

  function notify(title: string, body: string) {
    playSound()
    showNotification(title, body)
  }

  return {
    notificationEnabled,
    soundEnabled,
    requestPermission,
    notify,
    flashTitle
  }
}
