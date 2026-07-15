import { ref, onUnmounted } from 'vue'
import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

interface StompConfig {
  brokerURL?: string
  token: string
  userType: string
  onMessage?: (msg: any) => void
  onConnected?: () => void
  onDisconnected?: () => void
}

export function useStompClient() {
  const connected = ref(false)
  const client = ref<Client | null>(null)
  const subscriptions = new Map<string, StompSubscription>()

  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let heartbeatTimer: ReturnType<typeof setInterval> | null = null
  let retryCount = 0
  const maxRetries = 10

  function connect(config: StompConfig) {
    if (client.value?.active) {
      return
    }

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(config.brokerURL || '/ws-chat'),
      connectHeaders: {
        Authorization: `Bearer ${config.token}`,
        'User-Type': config.userType
      },
      debug: (str: string) => {
        if (import.meta.env.DEV) console.debug('[STOMP]', str)
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 15000,
      heartbeatOutgoing: 15000,
      onConnect: () => {
        connected.value = true
        retryCount = 0
        config.onConnected?.()
        startHeartbeat()
      },
      onDisconnect: () => {
        connected.value = false
        config.onDisconnected?.()
        stopHeartbeat()
      },
      onStompError: (frame: any) => {
        console.error('[STOMP] Error:', frame.headers['message'])
        connected.value = false
      },
      onWebSocketClose: () => {
        connected.value = false
        retryCount++
        if (retryCount <= maxRetries) {
          const delay = Math.min(30000, 1000 * Math.pow(2, retryCount))
          reconnectTimer = setTimeout(() => {
            if (!client.value?.active) {
              connect(config)
            }
          }, delay)
        }
      }
    })

    client.value = stompClient
    stompClient.activate()
  }

  function subscribe(destination: string, callback: (msg: any) => void): StompSubscription | null {
    if (!client.value?.active) return null

    const sub = client.value.subscribe(destination, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body)
        callback(body)
      } catch (e) {
        console.error('[STOMP] Failed to parse message:', e)
      }
    })

    subscriptions.set(destination, sub)
    return sub
  }

  function unsubscribe(destination: string) {
    const sub = subscriptions.get(destination)
    if (sub) {
      sub.unsubscribe()
      subscriptions.delete(destination)
    }
  }

  function send(destination: string, body: any) {
    if (!client.value?.active) {
      console.warn('[STOMP] Cannot send, not connected')
      return false
    }

    client.value.publish({
      destination,
      body: JSON.stringify(body)
    })
    return true
  }

  function disconnect() {
    stopHeartbeat()
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    subscriptions.forEach(sub => sub.unsubscribe())
    subscriptions.clear()
    client.value?.deactivate()
    connected.value = false
  }

  function startHeartbeat() {
    stopHeartbeat()
    heartbeatTimer = setInterval(() => {
      if (client.value?.active) {
        send('/tcp/chat/heartbeat', { type: 'heartbeat', timestamp: Date.now() })
      }
    }, 15000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    send
  }
}
