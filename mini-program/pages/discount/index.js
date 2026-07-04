const app = getApp()
const { get, resolveImageUrl } = require('../../utils/request')

Page({
  data: {
    activities: [],
    loading: true
  },
  onShow() { this.loadActivities() },
  async loadActivities() {
    try {
      const res = await get('/api/discount/active-with-products')
      const list = Array.isArray(res) ? res : (res.records || res.list || [])
      // 第一步：把基础结构和倒计时渲染出来
      const activities = list.map(act => {
        const diff = new Date(act.endTime) - new Date()
        let countdownText = ''
        if (diff <= 0) countdownText = '已结束'
        else {
          const d = Math.floor(diff / 86400000)
          const h = Math.floor((diff % 86400000) / 3600000)
          countdownText = d > 0 ? `${d}天${h}时` : `${h}时${Math.floor((diff%3600000)/60000)}分`
        }
        if (act.products && Array.isArray(act.products)) {
          act.products = act.products.map(p => ({
            ...p,
            cover: resolveImageUrl(p.cover)
          }))
        }
        return { ...act, countdownText }
      })
      this.setData({ activities, loading: false })
      // 第二步：N+1 补全 productName / originalPrice（后端非 DB 字段）
      // 并行拉取所有相关商品详情，失败的单个不影响整体
      const productIdSet = new Set()
      activities.forEach(act => {
        (act.products || []).forEach(dp => {
          if (dp.productId) productIdSet.add(dp.productId)
        })
      })
      const productInfoMap = {}
      await Promise.all(
        Array.from(productIdSet).map(pid =>
          get('/api/product/' + pid)
            .then(p => {
              if (p) productInfoMap[pid] = p
            })
            .catch(() => { /* 单个商品拉取失败忽略 */ })
        )
      )
      // 补全并刷新视图
      const filled = activities.map(act => ({
        ...act,
        products: (act.products || []).map(dp => {
          const detail = productInfoMap[dp.productId]
          if (!detail) return dp
          return {
            ...dp,
            productName: dp.productName || detail.name,
            originalPrice: dp.originalPrice == null ? detail.price : dp.originalPrice
          }
        })
      }))
      this.setData({ activities: filled })
    } catch (e) {
      console.error('加载优惠活动失败:', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally { this.setData({ loading: false }) }
  },

  // 跳转到活动详情
  goToActivity(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/activity/detail?id=${id}` });
  },

  // 跳转到商品详情
  goToProduct(e) {
    const id = e.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/product/product?id=${id}` });
  }
})
