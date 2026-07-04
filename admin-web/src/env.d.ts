/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module '@element-plus/icons-vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
  export const User: DefineComponent<{}, {}, any>
  export const Star: DefineComponent<{}, {}, any>
  export const Promotion: DefineComponent<{}, {}, any>
  export const Search: DefineComponent<{}, {}, any>
  export const DocumentAdd: DefineComponent<{}, {}, any>
  export const Warning: DefineComponent<{}, {}, any>
  export const Bell: DefineComponent<{}, {}, any>
  export const UserFilled: DefineComponent<{}, {}, any>
  export const ChatDotRound: DefineComponent<{}, {}, any>
  export const Smiley: DefineComponent<{}, {}, any>
  export const Picture: DefineComponent<{}, {}, any>
  export const CircleCheck: DefineComponent<{}, {}, any>
  export const CircleClose: DefineComponent<{}, {}, any>
  export const Loading: DefineComponent<{}, {}, any>
  export const Shop: DefineComponent<{}, {}, any>
  export const Avatar: DefineComponent<{}, {}, any>
  export const InfoFilled: DefineComponent<{}, {}, any>
  export const Edit: DefineComponent<{}, {}, any>
  export const Delete: DefineComponent<{}, {}, any>
  export const View: DefineComponent<{}, {}, any>
  export const Plus: DefineComponent<{}, {}, any>
  export const Check: DefineComponent<{}, {}, any>
  export const Close: DefineComponent<{}, {}, any>
  export const Setting: DefineComponent<{}, {}, any>
  export const Refresh: DefineComponent<{}, {}, any>
  export const Download: DefineComponent<{}, {}, any>
  export const Upload: DefineComponent<{}, {}, any>
  export const ArrowLeft: DefineComponent<{}, {}, any>
  export const ArrowRight: DefineComponent<{}, {}, any>
  export const Document: DefineComponent<{}, {}, any>
  export const ChatLineSquare: DefineComponent<{}, {}, any>
  export const Phone: DefineComponent<{}, {}, any>
  export const Message: DefineComponent<{}, {}, any>
  export const Location: DefineComponent<{}, {}, any>
  export const Timer: DefineComponent<{}, {}, any>
  export const Money: DefineComponent<{}, {}, any>
  export const Goods: DefineComponent<{}, {}, any>
  export const ShoppingCart: DefineComponent<{}, {}, any>
  export const Ticket: DefineComponent<{}, {}, any>
  export const DataAnalysis: DefineComponent<{}, {}, any>
}

declare module 'sockjs-client' {
  const SockJS: any
  export default SockJS
}

declare module 'stompjs' {
  const Stomp: any
  export default Stomp
}
