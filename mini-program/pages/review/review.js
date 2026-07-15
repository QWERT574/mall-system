const { get, post, resolveImageUrl } = require('../../utils/request')
const app = getApp()

Page({
  data: {
    orderId: null,
    order: null,
    items: [],
    loading: true,
    submitting: false
  },

  onLoad(options) {
    this.orderId = options.id
    this.loadOrderDetail()
  },

  async loadOrderDetail() {
    try {
      const order = await get(`/api/order/detail/${this.orderId}`)
      const items = (order.items || []).map(item => ({
        ...item,
        productImage: resolveImageUrl(item.productImage),
        rating: 5,
        comment: '',
        images: []
      }))
      this.setData({ order, items, loading: false })
    } catch (e) {
      this.setData({ loading: false })
      wx.showToast({ title: '订单不存在', icon: 'none' })
    }
  },

  selectStar(e) {
    const { index, itemindex } = e.currentTarget.dataset
    const key = `items[${itemindex}].rating`
    this.setData({ [key]: index + 1 })
  },

  onCommentInput(e) {
    const { itemindex } = e.currentTarget.dataset
    const key = `items[${itemindex}].comment`
    this.setData({ [key]: e.detail.value })
  },

  chooseImage(e) {
    const { itemindex } = e.currentTarget.dataset
    wx.chooseImage({
      count: 3 - this.data.items[itemindex].images.length,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        const tempPaths = res.tempFilePaths
        tempPaths.forEach(path => {
          wx.uploadFile({
            url: app.globalData.backendBaseUrl + '/api/upload/image',
            filePath: path,
            name: 'file',
            success: (uploadRes) => {
              try {
                const data = JSON.parse(uploadRes.data)
                if (data.code === 0 && data.data) {
                  const imagesKey = `items[${itemindex}].images`
                  const newImages = [...this.data.items[itemindex].images, data.data]
                  this.setData({ [imagesKey]: newImages })
                }
              } catch (e) {}
            }
          })
        })
      }
    })
  },

  deleteImage(e) {
    const { itemindex, imgindex } = e.currentTarget.dataset
    const imagesKey = `items[${itemindex}].images`
    const imgs = [...this.data.items[itemindex].images]
    imgs.splice(imgindex, 1)
    this.setData({ [imagesKey]: imgs })
  },

  async submitReview() {
    if (this.data.submitting) return

    for (const item of this.data.items) {
      try {
        await post('/api/product/review', {
          productId: item.productId,
          orderId: this.orderId,
          userId: app.globalData.userInfo ? app.globalData.userInfo.id : 0,
          rating: item.rating,
          content: item.comment,
          images: JSON.stringify(item.images)
        })
      } catch (e) {
        wx.showToast({ title: '部分商品评价失败', icon: 'none' })
      }
    }

    wx.showToast({ title: '评价成功', icon: 'success' })
    setTimeout(() => {
      wx.navigateBack()
    }, 1500)
  },

  goBack() {
    wx.navigateBack()
  }
})