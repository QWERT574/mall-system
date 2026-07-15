const app = getApp()
const { get, post } = require('../../utils/request')

Page({
  data: {
    tab: 'center',
    coupons: [],
    myCoupons: [],
    claimedIds: {},
    page: 1,
    total: 0,
    loading: true
  },
  onShow() {
    this.loadCoupons()
    this.loadMyCoupons()
  },
  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.tab }) },
  async loadCoupons() {
    this.setData({ loading: true })
    try {
      const res = await get('/api/coupon/available', { page: this.data.page, size: 20 })
      const coupons = (res.records || res || []).map(c => ({
        ...c,
        endTimeText: this.formatTime(c.endTime)
      }))
      this.setData({ coupons, total: res.total || 0 })
    } catch (e) {
      console.error('加载优惠券失败:', e)
    } finally { this.setData({ loading: false }) }
  },
  async loadMyCoupons() {
    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) return
    try {
      const res = await get('/api/coupon/user/' + userInfo.id)
      const myCoupons = (Array.isArray(res) ? res : (res.records || [])).map(c => ({
        ...c,
        endTimeText: this.formatTime(c.endTime)
      }))
      const ids = {}
      myCoupons.forEach(uc => { ids[uc.couponId] = true })
      this.setData({ myCoupons, claimedIds: ids })
    } catch (e) {
      console.error('加载我的优惠券失败:', e)
    }
  },
  async claimCoupon(e) {
    const coupon = e.currentTarget.dataset.coupon
    const userInfo = app.globalData.userInfo
    if (!userInfo || !userInfo.id) { wx.showToast({ title: '请先登录', icon: 'none' }); return }
    // 防止重复点击：同一时间只允许一个领取请求
    if (this.claiming) return
    this.claiming = true
    try {
      await post('/api/coupon/claim/' + coupon.id, { userId: userInfo.id })
      wx.showToast({ title: '领取成功！', icon: 'success' })
      const ids = { ...this.data.claimedIds, [coupon.id]: true }
      this.setData({ claimedIds: ids })
      this.loadMyCoupons()
    } catch (e) {
      // 后端 message 形如 "领取优惠券失败: 你已达到该优惠券领取上限"
      // wx.showToast 的 title 有长度限制（中文 7 字左右），原消息会被截断
      // 去掉前缀拿到真正原因后展示，必要时长 toast 停留时间
      const rawMsg = (e && e.message) || ''
      const msg = rawMsg.replace(/^领取优惠券失败[：:]\s*/, '') || '领取失败'
      console.error('领取优惠券失败:', e)
      wx.showToast({ title: msg, icon: 'none', duration: 2500 })
      // 失败后也刷新"我的优惠券"，把已领过的券同步成"已领取"状态
      // 避免因为限额等原因失败时，按钮一直显示"立即领取"误导用户反复点击
      this.loadMyCoupons()
    } finally {
      this.claiming = false
    }
  },
  formatTime(t) {
    if (!t) return ''
    return new Date(t).toLocaleDateString('zh-CN')
  }
})
