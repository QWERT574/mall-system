const { get, post, del, resolveImageUrl } = require('../../utils/request')

Page({
  data: {
    activity: {},
    loading: true,
    joining: false
  },

  onLoad(options) {
    if (options.id) {
      this.loadDetail(options.id)
    }
  },

  async loadDetail(id) {
    this.setData({ loading: true })
    try {
      const activity = await get('/api/activity/' + id)
      if (activity) {
        activity.coverImage = resolveImageUrl(activity.coverImage)
        activity.statusText = this.getStatusText(activity.status)
        activity.statusClass = this.getStatusClass(activity.status)
        activity.startTimeText = this.formatDate(activity.startTime)
        activity.endTimeText = this.formatDate(activity.endTime)
      }
      this.setData({ activity, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  async joinActivity() {
    if (this.data.joining) return
    const { activity } = this.data
    if (activity.status !== 1) {
      wx.showToast({ title: '活动未在进行中', icon: 'none' })
      return
    }
    if (activity.maxCount > 0 && activity.currentCount >= activity.maxCount) {
      wx.showToast({ title: '名额已满', icon: 'none' })
      return
    }

    this.setData({ joining: true })
    try {
      await post('/api/activity/participant', { activityId: activity.id })
      wx.showToast({ title: '报名成功', icon: 'success' })
      this.setData({
        'activity.joined': true,
        'activity.currentCount': (activity.currentCount || 0) + 1
      })
    } catch (e) {
      wx.showToast({ title: '报名失败', icon: 'none' })
    } finally {
      this.setData({ joining: false })
    }
  },

  cancelJoin() {
    if (this.data.joining) return
    const { activity } = this.data

    wx.showModal({
      title: '提示',
      content: '确定取消报名吗？',
      success: async (res) => {
        if (res.confirm) {
          this.setData({ joining: true })
          try {
            await del('/api/activity/participant', { activityId: activity.id })
            wx.showToast({ title: '已取消报名', icon: 'success' })
            this.setData({
              'activity.joined': false,
              'activity.currentCount': Math.max(0, (activity.currentCount || 1) - 1)
            })
          } catch (e) {
            wx.showToast({ title: '操作失败', icon: 'none' })
          } finally {
            this.setData({ joining: false })
          }
        }
      }
    })
  },

  formatDate(dt) {
    if (!dt) return ''
    const d = new Date(dt)
    const pad = n => String(n).padStart(2, '0')
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
  },

  getStatusText(status) {
    const map = { 0: '未开始', 1: '进行中', 2: '已结束' }
    return map[status] || '未知'
  },

  getStatusClass(status) {
    const map = { 0: 'status-upcoming', 1: 'status-ongoing', 2: 'status-ended' }
    return map[status] || ''
  }
})
