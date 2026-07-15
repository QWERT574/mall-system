const app = getApp();
// 导入网络请求封装
const { get, post, resolveImageUrl } = require('../../utils/request');
// 导入支付工具类
const { payOrder, getOrderPayStatus } = require('../../utils/pay');

Page({
  data:{
    order:null,
    paymentMethod: 'wechat',
    paymentMethods: [
      { value: 'wechat', label: '微信支付', icon: '💚' },
      { value: 'alipay', label: '支付宝', icon: '💙' },
      { value: 'bank', label: '银行卡', icon: '💳' }
    ]
  },
  onLoad(query){
    console.log('订单详情页面onLoad，传入的参数：', query);
    const id = query.id;
    if (!id) {
      console.error('订单ID为空');
      wx.showToast({title: '订单ID为空', icon: 'error'});
      return;
    }
    this.orderId = id;
    this.loadOrderDetail(id);
  },
  
  onShow() {
    // 页面显示时，重新加载订单详情，确保显示最新状态
    if (this.orderId) {
      this.loadOrderDetail(this.orderId);
    }
  },
  
  onUnload() {
    // 页面卸载时，清除订单状态轮询
    if (this.statusPollingTimer) {
      clearInterval(this.statusPollingTimer);
      this.statusPollingTimer = null;
    }
  },
  
  // 加载订单详情
  async loadOrderDetail(id){ 
    console.log('开始加载订单详情，订单ID：', id);
    try {
      wx.showLoading({title: '加载中...'});
      const order = await get(`/api/order/detail/${id}`);
      console.log('订单详情数据：', order);
      if (order && order.items && Array.isArray(order.items)) {
        order.items.forEach(item => {
          item.productImage = resolveImageUrl(item.productImage);
        });
      }
      this.setData({order});
      if (order.status === 0) {
        this.startStatusPolling(id);
      } else {
        this.stopStatusPolling();
      }
    } catch (error) {
      console.error('加载订单详情异常：', error);
      if (error && error.statusCode === 404) {
        wx.showToast({title: '订单不存在', icon: 'none', duration: 2000});
      } else {
        wx.showToast({title: '获取订单详情失败，请稍后重试', icon: 'error'});
      }
    } finally {
      wx.hideLoading();
    }
  },
  
  selectPaymentMethod(e) {
    const method = e.currentTarget.dataset.method;
    this.setData({ paymentMethod: method });
  },

  getPaymentMethodLabel(method) {
    const map = { 'wechat': '微信支付', 'alipay': '支付宝', 'bank': '银行卡' };
    return map[method] || '未知';
  },

  async payNow(){
    const id = Number(this.data.order.id);
    const paymentMethod = this.data.paymentMethod;
    
    await payOrder(id, 
      () => {
        console.log('支付成功，重新加载订单详情');
        this.loadOrderDetail(id);
      }, 
      (error) => {
        console.error('支付失败:', error);
      }, 
      () => {
        console.log('支付流程完成');
      },
      paymentMethod
    );
  },
  
  // 启动订单状态轮询
  startStatusPolling(orderId) {
    this.stopStatusPolling();
    this.statusPollingTimer = setInterval(async () => {
      try {
        const status = await getOrderPayStatus(orderId);
        if (status.status !== 0) {
          this.stopStatusPolling();
          this.loadOrderDetail(orderId);
        }
      } catch (error) {
        console.error('轮询订单状态失败:', error);
      }
    }, 5000);
  },
  
  // 停止订单状态轮询
  stopStatusPolling() {
    if (this.statusPollingTimer) {
      clearInterval(this.statusPollingTimer);
      this.statusPollingTimer = null;
      console.log('订单状态轮询已停止');
    }
  },
  
  // 确认收货
  async confirmReceipt(){ 
    const id = this.data.order.id;
    wx.showModal({
      title: '确认收货',
      content: '确定要确认收货吗？',
      success: async (res) => {
        if(res.confirm){
          try {
            // 使用封装的post函数调用确认收货API，确保带上正确的User-Type头部
            await post(`/api/order/confirm/${id}`);
            wx.showToast({title: '确认收货成功'});
            // 重新加载订单详情
            this.loadOrderDetail(id);
          } catch (error) {
            console.error('确认收货失败:', error);
            wx.showToast({title: '确认收货失败: ' + (error.response?.data?.message || '网络错误'), icon: 'error'});
          }
        }
      }
    })
  },
  
  // 取消订单
  async cancelOrder(){ 
    const id = this.data.order.id;
    wx.showModal({
      title: '取消订单',
      content: '确定要取消订单吗？',
      success: async (res) => {
        if(res.confirm){
          try {
            // 使用封装的post函数调用取消订单API，确保带上正确的User-Type头部
            await post(`/api/order/cancel/${id}`);
            wx.showToast({title: '取消订单成功'});
            // 重新加载订单详情
            this.loadOrderDetail(id);
          } catch (error) {
            console.error('取消订单失败:', error);
            wx.showToast({title: '取消订单失败: ' + (error.response?.data?.message || '网络错误'), icon: 'error'});
          }
        }
      }
    })
  },

  // 去首页
  goToHome(){
    wx.switchTab({ url: '/pages/home/home' });
  },

  // 去评价
  goToReview(){
    wx.navigateTo({ url: '/pages/review/review?id=' + this.data.order.id });
  },

  // 申请售后
  goToAftersale(){
    wx.navigateTo({ url: '/pages/aftersale/create?orderId=' + this.data.order.id });
  },

  // 联系客服
  goToService(){
    const order = this.data.order;
    let url = '/pages/chat/chat';
    const params = [];
    if (order && order.id) params.push('orderId=' + order.id);
    if (params.length > 0) url += '?' + params.join('&');
    wx.navigateTo({ url });
  },

  // 查看售后详情
  goToAftersaleDetail(){
    wx.navigateTo({ url: '/pages/aftersale/list' });
  },

  // 再次购买 - 将订单商品加入购物车
  async buyAgain(){
    const order = this.data.order;
    if (!order || !order.items || order.items.length === 0) return;
    try {
      wx.showLoading({ title: '加入购物车中...' });
      const userInfo = app.globalData.userInfo;
      for (const item of order.items) {
        await post('/api/cart/add', {
          userId: userInfo.id,
          productId: item.productId,
          quantity: item.quantity || 1
        });
      }
      wx.hideLoading();
      wx.showToast({ title: '已加入购物车', icon: 'success' });
      setTimeout(() => {
        wx.switchTab({ url: '/pages/cart/cart' });
      }, 1500);
    } catch (e) {
      wx.hideLoading();
      wx.showToast({ title: '加入购物车失败', icon: 'none' });
    }
  }
})
