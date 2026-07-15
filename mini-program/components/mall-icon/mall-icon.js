const iconMap = {
  home: '🏠',
  shop: '🏪',
  cart: '🛒',
  user: '👤',
  order: '📋',
  category: '📂',
  search: '🔍',
  plus: '➕',
  edit: '✏️',
  trash: '🗑️',
  check: '✅',
  close: '❌',
  arrowLeft: '◀',
  arrowRight: '▶',
  arrowDown: '▼',
  arrowUp: '▲',
  heart: '❤️',
  star: '⭐',
  clock: '⏰',
  bell: '🔔',
  setting: '⚙️',
  info: 'ℹ️',
  filter: '🔽',
  location: '📍',
  tag: '🏷️',
  gift: '🎁',
  leaf: '🌿',
  money: '💰',
  chat: '💬',
  thumbsUp: '👍',
  upload: '⬆️',
  download: '⬇️',
  refresh: '🔄',
  share: '↗️',
  exit: '🚪',
  phone: '📞',
  coupon: '🎫',
  robot: '🤖',
  settings: '⚙️',
  home2: '🏡',
  lock: '🔒'
}

const sizeMap = {
  sm: { fontSize: '32rpx', width: '40rpx', height: '40rpx' },
  md: { fontSize: '44rpx', width: '52rpx', height: '52rpx' },
  lg: { fontSize: '56rpx', width: '68rpx', height: '68rpx' }
}

Component({
  properties: {
    name: { type: String, value: '' },
    size: { type: String, value: 'md' },
    color: { type: String, value: '#333333' }
  },

  data: {
    iconText: '',
    iconSize: sizeMap.md
  },

  observers: {
    'name, size': function(name, size) {
      this.setData({
        iconText: iconMap[name] || '?',
        iconSize: sizeMap[size] || sizeMap.md
      })
    }
  },

  attached() {
    this.setData({
      iconText: iconMap[this.data.name] || '?',
      iconSize: sizeMap[this.data.size] || sizeMap.md
    })
  }
})
