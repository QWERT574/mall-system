const { get, post, del, resolveImageUrl } = require('../../utils/request')

Page({
  data: {
    activities: [],
    loading: true,
    searchQuery: '',
    filterStatus: -1,
    page: 1,
    hasMore: true,
    filters: [
      { label: '全部', value: -1 },
      { label: '未开始', value: 0 },
      { label: '进行中', value: 1 },
      { label: '已结束', value: 2 }
    ]
  },

  onLoad() {
    this.loadActivities()
  },

  onPullDownRefresh() {
    this.setData({ page: 1, hasMore: true, activities: [] })
    this.loadActivities().then(() => {
      wx.stopPullDownRefresh()
    })
  },

  async loadActivities() {
    this.setData({ loading: true })
    const { page, filterStatus, searchQuery } = this.data
    const params = { page, size: 10 }
    if (filterStatus >= 0) params.status = filterStatus
    if (searchQuery) params.keyword = searchQuery

    try {
      const res = await get('/api/activity/list', params)
      const list = (res.records || res.list || res.content || []).map(item => ({
        ...item,
        coverImage: resolveImageUrl(item.coverImage),
        statusText: this.getStatusText(item.status),
        statusClass: this.getStatusClass(item.status)
      }))
      const total = res.total || 0
      const newList = page === 1 ? list : [...this.data.activities, ...list]
      this.setData({
        activities: newList,
        loading: false,
        hasMore: newList.length < total
      })
    } catch (err) {
      this.setData({ loading: false })
      wx.showToast({ title: '加载失败', icon: 'none' })
    }
  },

  onSearchInput(e) {
    this.setData({ searchQuery: e.detail.value })
  },

  onSearch() {
    this.setData({ page: 1, activities: [], hasMore: true })
    this.loadActivities()
  },

  filterByStatus(e) {
    const value = e.currentTarget.dataset.value
    this.setData({ filterStatus: value, page: 1, activities: [], hasMore: true })
    this.loadActivities()
  },

  goToDetail(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: '/pages/activity/detail?id=' + id })
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ page: this.data.page + 1 })
    this.loadActivities()
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
