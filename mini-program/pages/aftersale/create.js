const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    orderId: '',
    orderItem: null,
    serviceType: 0,
    reason: '',
    contactPhone: '',
    submitting: false
  },
  onLoad(options) {
    if (options.orderId) {
      this.setData({ orderId: options.orderId })
      this.loadOrderDetail(options.orderId)
    }
  },
  async loadOrderDetail(orderId) {
    try {
      const order = await get(`/api/order/detail/${orderId}`)
      this.setData({ orderItem: order })
    } catch (e) {
      console.error('加载订单详情失败:', e)
      wx.showToast({ title: '加载订单信息失败', icon: 'none' })
    }
  },
  selectType(e) {
    this.setData({ serviceType: Number(e.currentTarget.dataset.type) })
  },
  onContactPhoneInput(e) {
    this.setData({ contactPhone: e.detail.value })
  },
  onReasonInput(e) {
    this.setData({ reason: e.detail.value })
  },
  async submit() {
    if (!this.data.serviceType) { wx.showToast({ title: '请选择售后类型', icon: 'none' }); return }
    if (!this.data.reason.trim()) { wx.showToast({ title: '请填写售后原因', icon: 'none' }); return }

    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) { wx.showToast({ title: '请先登录', icon: 'none' }); return }

    this.setData({ submitting: true })
    try {
      const firstItem = this.data.orderItem && this.data.orderItem.items && this.data.orderItem.items[0]
      const data = {
        orderId: parseInt(this.data.orderId),
        userId: userInfo.id,
        productId: firstItem ? firstItem.productId : 0,
        serviceType: this.data.serviceType,
        reason: this.data.reason.trim(),
        contactPhone: this.data.contactPhone || userInfo.phone || '',
        images: ''
      }
      const result = await post('/api/aftersale', data)
      wx.showToast({ title: '申请已提交', icon: 'success' })
      setTimeout(() => wx.navigateTo({ url: `/pages/aftersale/detail?id=${result.id || result}` }), 1000)
    } catch (e) {
      wx.showToast({ title: '提交失败，请重试', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  }
})
