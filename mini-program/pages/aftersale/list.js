const app = getApp()
const { get } = require('../../utils/request')

Page({
  data: {
    list: [],
    loading: false,
    activeTab: 'all'
  },
  onShow() {
    this.loadList()
  },
  switchTab(e) {
    this.setData({ activeTab: e.currentTarget.dataset.tab })
    this.loadList()
  },
  async loadList() {
    this.setData({ loading: true })
    try {
      const userInfo = app.globalData.userInfo
      if (!userInfo || !userInfo.id) {
        wx.showToast({ title: '请先登录', icon: 'none' })
        this.setData({ loading: false })
        return
      }
      const params = { page: 1, size: 50 }
      if (this.data.activeTab !== 'all') {
        params.status = this.data.activeTab
      }
      const res = await get(`/api/aftersale/user/${userInfo.id}/page`, params)
      const records = (res.records || res.list || []).map(item => ({
        ...item,
        statusText: this.statusText(item.status),
        serviceTypeText: this.serviceTypeText(item.serviceType),
        createdAtText: this.formatTime(item.createdAt)
      }))
      this.setData({ list: records })
    } catch (e) {
      console.error('加载售后列表失败:', e)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },
  serviceTypeText(t) {
    return { 1: '退货退款', 2: '换货', 3: '维修' }[t] || '其他'
  },
  statusText(s) {
    return { 0: '待处理', 1: '处理中', 2: '已解决', 3: '已关闭' }[s] || '未知'
  },
  formatTime(t) {
    if (!t) return ''
    const d = new Date(t)
    return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/aftersale/detail?id=${e.currentTarget.dataset.id}` })
  }
})
