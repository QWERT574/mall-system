const app = getApp();
// 导入网络请求封装
const { get, post, resolveImageUrl } = require('../../utils/request');
// 导入支付工具类
const { payOrder } = require('../../utils/pay');

Page({
  data:{
    orders:[],
    filteredOrders:[],
    loading: false,
    refreshing: false,
    activeFilter: 'all', // all, pending, paid, shipped, completed
    filterOptions: [
      { label: '全部', value: 'all', icon: '📦' },
      { label: '待支付', value: 'pending', icon: '💳' },
      { label: '已支付', value: 'paid', icon: '✅' },
      { label: '已发货', value: 'shipped', icon: '🚚' },
      { label: '已完成', value: 'completed', icon: '🎉' }
    ]
  },

  onLoad(options) {
    if (options && options.status !== undefined) {
      const statusMap = { '0': 'pending', '1': 'paid', '2': 'shipped', '3': 'completed' };
      const filter = statusMap[options.status] || 'all';
      this.setData({ activeFilter: filter });
    }
  },
  
  onShow(){ 
    this.loadOrders(); 
  },
  
  // 加载订单列表
  async loadOrders(){ 
    this.setData({ loading: true });
    try { 
      const userInfo = app.globalData.userInfo;
      
      if (userInfo) {
        try {
          const res = await get('/api/order/list', { openid: userInfo.openid });
          console.log('获取订单列表成功:', res);
          // 兼容多种返回格式：分页对象(records/list/content)或直接数组
          const orderList = Array.isArray(res) ? res : (res.records || res.list || res.content || []);
          const processedOrders = orderList.map(order => ({
            ...order,
            statusText: this.getStatusText(order.status),
            createTimeText: this.formatTime(order.createdAt)
          }));
          this.setData({ 
            orders: processedOrders,
            filteredOrders: processedOrders
          });
          // 如果有初始筛选，应用筛选
          if (this.data.activeFilter !== 'all') {
            this.applyFilter();
          }
        } catch (apiError) {
          console.error('获取订单列表失败:', apiError);
          this.setData({ orders: [], filteredOrders: [] });
          wx.showToast({title: '获取订单列表失败', icon: 'none', duration: 2000});
        }
      } else {
        this.setData({ orders: [], filteredOrders: [] });
        wx.showToast({title: '请先登录查看订单', icon: 'none', duration: 2000});
      }
    } catch (error) { 
      console.error('获取订单列表失败:', error);
      this.setData({ orders: [], filteredOrders: [] });
      wx.showToast({title: '获取订单列表失败', icon: 'error', duration: 2000});
    } finally { 
      this.setData({ loading: false });
    }
  },
  
  // 筛选订单
  filterOrders(e){
    const filter = e.currentTarget.dataset.filter;
    this.setData({ activeFilter: filter });
    this.applyFilter();
  },

  // 应用筛选
  applyFilter(){
    let filteredOrders = this.data.orders;
    if(this.data.activeFilter !== 'all'){
      const statusMap = {
        'pending': 0,
        'paid': 1,
        'shipped': 2,
        'completed': 3
      };
      const status = statusMap[this.data.activeFilter];
      filteredOrders = this.data.orders.filter(order => order.status === status);
    }
    this.setData({ filteredOrders: filteredOrders });
  },
  
  // 获取订单状态文本
  getStatusText(status){
    const statusMap = {
      0: '待支付',
      1: '已支付',
      2: '已发货',
      3: '已完成'
    };
    return statusMap[status] || '未知状态';
  },
  
  // 格式化时间
  formatTime(timeStr){
    if(!timeStr) return '';
    // 简单的时间格式化，实际项目中可以使用更复杂的格式化
    const date = new Date(timeStr);
    return `${date.getFullYear()}-${(date.getMonth()+1).toString().padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`;
  },
  
  // 查看订单详情
  viewOrder(e){
    const id = e.currentTarget.dataset.id;
    console.log('查看订单详情，订单ID：', id);
    wx.navigateTo({
      url: '/pages/order/order?id=' + id,
      success: function(res) {
        console.log('页面跳转成功');
      },
      fail: function(res) {
        console.error('页面跳转失败：', res);
        wx.showToast({
          title: '页面跳转失败',
          icon: 'error'
        });
      }
    });
  },
  
  // 去首页购物
  goToHome(){
    wx.switchTab({ url: '/pages/home/home' });
  },
  
  // 支付订单
  async handlePayOrder(e){
    const id = Number(e.currentTarget.dataset.id);
    
    // 使用统一的支付工具类处理支付
    await payOrder(id, 
      // 支付成功回调
      () => {
        console.log('支付成功，重新加载订单列表');
        this.loadOrders();
      }, 
      // 支付失败回调
      (error) => {
        console.error('支付失败:', error);
        // 这里可以添加额外的失败处理逻辑
      }, 
      // 支付完成回调
      () => {
        console.log('支付流程完成');
      }
    );
  },
  
  // 确认收货
  confirmReceipt(e){
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认收货',
      content: '确定要确认收货吗？',
      success: async (res) => {
        if(res.confirm){
          try {
            // 使用封装的post函数调用确认收货API，确保带上正确的User-Type头部
            await post('/api/order/confirm/' + id);
            wx.showToast({title: '确认收货成功'});
            // 重新加载订单列表
            this.loadOrders();
          } catch (error) {
            console.error('确认收货失败:', error);
            wx.showToast({title: '确认收货失败: ' + (error.response?.data?.message || '网络错误'), icon: 'error'});
          }
        }
      }
    })
  },
  
  // 取消订单
  async cancelOrder(e){ 
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '取消订单',
      content: '确定要取消订单吗？',
      success: async (res) => {
        if(res.confirm){
          try {
            // 使用封装的post函数调用取消订单API，确保带上正确的User-Type头部
            await post('/api/order/cancel/' + id);
            wx.showToast({title: '取消订单成功'});
            // 重新加载订单列表
            this.loadOrders();
          } catch (error) {
            console.error('取消订单失败:', error);
            wx.showToast({title: '取消订单失败: ' + (error.response?.data?.message || '网络错误'), icon: 'error'});
          }
        }
      }
    })
  },

  applyAfterSale(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/aftersale/create?orderId=${id}` })
  },

  // 去评价
  goToReview(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/review/review?id=${id}` })
  },

  // 联系客服
  goToService(e) {
    const id = e.currentTarget.dataset.id
    wx.navigateTo({ url: `/pages/chat/chat?orderId=${id}` })
  },

  onPullDownRefresh() {
    this.setData({ refreshing: true });
    this.loadOrders().finally(() => {
      this.setData({ refreshing: false });
      wx.stopPullDownRefresh();
    });
  }
})