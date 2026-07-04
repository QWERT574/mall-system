import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('seller_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 添加用户类型（商家端，按后端 DB 约定：0=买家, 1=卖家, 2=管理员）
    let user = null;
    try {
      user = JSON.parse(localStorage.getItem('seller_user') || '{}');
    } catch (e) {
      console.error('解析用户信息失败:', e);
    }
    
    if (user && user.userType !== undefined && user.userType !== null) {
      config.headers['User-Type'] = String(user.userType);
    } else {
      // 商家端未取到用户信息时默认为卖家
      config.headers['User-Type'] = '1';
    }
    
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    // 统一处理响应数据
    const data = response.data;
    console.log('API 响应数据:', data);

    // 检查是否有 code 字段
    if (data && typeof data.code !== 'undefined') {
      // 允许 code 为 0 或 200 作为成功状态
      if (data.code === 0 || data.code === 200) {
        return data.data;
      } else {
        // 业务错误，抛出异常
        return Promise.reject(new Error(data.message || '请求失败'));
      }
    }
    // 如果没有 code 字段，直接返回数据（可能是旧接口）
    return data;
  },
  error => {
    // 统一处理网络错误
    let message = '网络请求失败';
    if (error.response) {
      // HTTP 错误状态码处理
      switch (error.response.status) {
        case 401:
          message = '未登录或登录已过期';
          // 不在这里清除 localStorage 和跳转，让页面自己处理
          break;
        case 403:
          message = '没有权限访问该资源';
          break;
        case 404:
          message = '请求的资源不存在';
          break;
        case 500:
          message = '服务器内部错误';
          break;
        default:
          message = error.response.data?.message || `请求失败 (${error.response.status})`;
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      message = '服务器无响应，请稍后重试';
    }
    console.error('API 错误:', message, error.response);
    return Promise.reject(new Error(message));
  }
);

// 登录(支持 captcha 图形验证码)
api.login = (data) => api.post('/auth/login', data);

// 注册
api.register = (data) => api.post('/auth/register', data);

// 图形验证码: GET /api/captcha/image?key=xxx
api.getCaptcha = (key) => {
  if (key) {
    return api.get('/captcha/image', { params: { key } });
  }
  return api.get('/captcha/image');
};

// 发送短信验证码: POST /api/sms/send  body: { phone }
api.sendCode = (phone) => api.post('/sms/send', { phone });

// 重置密码
api.resetPassword = (data) => api.post('/auth/resetPassword', data);

// 通用GET请求
api.get = (url, config) => {
  const fullUrl = url;
  return api.request({
    method: 'GET',
    url: fullUrl,
    ...config
  });
};

// 通用POST请求
api.post = (url, data, config) => {
  const fullUrl = url;
  return api.request({
    method: 'POST',
    url: fullUrl,
    data: data,
    ...config
  });
};

// 通用DELETE请求
api.delete = (url, config) => {
  const fullUrl = url;
  return api.request({
    method: 'DELETE',
    url: fullUrl,
    ...config
  });
};

// 通用PUT请求
api.put = (url, data, config) => {
  const fullUrl = url;
  return api.request({
    method: 'PUT',
    url: fullUrl,
    data: data,
    ...config
  });
};

// 获取产品列表
api.getProducts = () => api.get('/product/list');

// 获取商品详情
api.getProductDetail = (id) => api.get(`/product/${id}`);

// 获取购物车列表
api.getCart = (userId) => api.get('/cart/list', { params: { userId } });

// 添加到购物车
api.addToCart = (data) => api.post('/cart/add', data);

// 更新购物车商品数量
api.updateCartItem = (id, quantity) => api.post(`/cart/${id}/update-quantity`, { quantity });

// 删除购物车商品
api.deleteCartItem = (id) => api.post(`/cart/${id}/delete`);

// 创建订单
api.createOrder = (data) => api.post('/order/create', data);

// 获取订单列表
api.getOrders = () => api.get('/order/list');

// 获取订单详情
api.getOrderDetail = (id) => api.get(`/order/detail/${id}`);

// 更新订单状态
api.updateOrderStatus = (data) => api.post('/order/updateStatus', data);

// 支付订单
api.payOrder = (orderId, paymentInfo) => api.post(`/order/pay/${orderId}`, paymentInfo);

// 获取用户信息
api.getUserInfo = () => api.get('/user/info');

// 更新用户信息
api.updateUserInfo = (data) => api.post('/user/update', data);

// 上传头像
api.uploadAvatar = (file) => {
  const formData = new FormData();
  formData.append('file', file);
  return api.post('/user/uploadAvatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 搜索商品
api.searchProducts = (params) => api.get('/product/search', { params });

// 获取搜索历史
api.getSearchHistory = (userId) => api.get('/product/search-history', { params: { userId } });

// 清空搜索历史
api.clearSearchHistory = (userId) => api.post('/product/search-history/clear', null, { params: { userId } });

// 商家相关 API
// 获取商家商品列表
api.getSellerProducts = (sellerId, page = 1, pageSize = 20) => api.get(`/product/seller/${sellerId}`, { params: { page, pageSize } });

// 获取商家商品列表（简化版）
api.getProductsBySellerId = (sellerId) => api.get(`/product/seller/${sellerId}`);

// 创建商品
api.createProduct = (data) => api.post('/product/create', data);

// 获取商品分类列表
api.getCategories = () => api.get('/category/list');

// 获取商品详情
api.getProductById = (id) => api.get(`/product/${id}`);

// 更新商品
api.updateProduct = (id, data) => api.put(`/product/${id}`, data);

// 删除商品
api.deleteProduct = (id) => api.delete(`/product/${id}`);

// 批量删除商品
api.batchDeleteProducts = (ids) => api.post('/product/batch-delete', ids);

// 获取商家订单列表
api.getSellerOrders = (sellerId) => api.get(`/order/seller/${sellerId}`);

// 分页获取商家订单
api.getSellerOrdersPage = (sellerId, page = 1, pageSize = 20) => api.get(`/order/seller/${sellerId}`, { params: { page, pageSize } });

// 发货
api.shipOrder = (orderId) => api.post(`/order/ship/${orderId}`);

// 获取商家信息
api.getSellerInfo = (sellerId) => api.get(`/user/seller/${sellerId}`);

// 更新商家信息
api.updateSellerInfo = (data) => api.post('/user/update-seller', data);

// 获取商家仪表盘数据
api.getSellerDashboard = (sellerId) => api.get(`/product/seller/${sellerId}/dashboard`);

// 获取商家信息
api.getSellerInfo = (sellerId) => api.get(`/seller/info?sellerId=${sellerId}`);

// 更新商家信息
api.updateSellerInfo = (data) => api.post('/seller/update', data);

// 获取商家状态
api.getSellerStatus = (sellerId) => api.get(`/seller/status?sellerId=${sellerId}`);

// ========== 客服聊天相关 API ==========
// 创建售后服务工单
api.createAfterSale = (data) => api.post('/aftersale', data);

// 获取售后服务详情
api.getAfterSaleById = (id) => api.get(`/aftersale/${id}`);

// 获取用户的售后服务列表
api.getAfterSalesByUserId = (userId) => api.get(`/aftersale/user/${userId}`);

// 获取订单的售后服务列表
api.getAfterSalesByOrderId = (orderId) => api.get(`/aftersale/order/${orderId}`);

// 分页获取售后服务列表
api.getAfterSalesPage = (page = 1, size = 10, status) => {
  const params = { page, size };
  if (status !== undefined && status !== null) {
    params.status = status;
  }
  return api.get('/aftersale/list', { params });
};

// 分页获取商家的售后服务列表
api.getSellerAfterSalesPage = (sellerId, page = 1, size = 10, status) => {
  const params = { sellerId, page, size };
  if (status !== undefined && status !== null) {
    params.status = status;
  }
  return api.get(`/aftersale/seller`, { params });
};

// 更新售后服务
api.updateAfterSale = (id, data) => api.put(`/aftersale/${id}`, data);

// 处理售后服务
api.processAfterSale = (id, status, serviceResult, operatorId, refundAmount) => {
  const params = { status, serviceResult, operatorId };
  if (refundAmount !== undefined && refundAmount !== null) {
    params.refundAmount = refundAmount;
  }
  return api.put(`/aftersale/${id}/process`, null, { params });
};

// 获取服务记录
api.getServiceRecords = (id) => api.get(`/aftersale/${id}/records`);

// 删除售后服务
api.deleteAfterSale = (id) => api.delete(`/aftersale/${id}`);

// 发送聊天消息
api.sendChatMessage = (data) => api.post('/aftersale/chat', data);

// 获取聊天记录
api.getChatMessages = (afterSaleId) => api.get(`/aftersale/chat/${afterSaleId}`);

// 获取未读消息数量
api.getUnreadCount = (afterSaleId, receiverType) => 
  api.get(`/aftersale/chat/${afterSaleId}/unread-count`, { 
    params: { receiverType } 
  });

// 标记为已读
api.markAsRead = (afterSaleId, receiverType) => 
  api.put(`/aftersale/chat/${afterSaleId}/read`, null, { 
    params: { receiverType } 
  });

// 删除聊天记录
api.deleteChat = (id) => api.delete(`/aftersale/chat/${id}`);

// ========== 评价管理相关 API ==========
// 获取商家评价列表
api.getSellerReviews = (sellerId, params) => api.get(`/review/seller/${sellerId}`, { params });

// 获取商家评价统计
api.getSellerReviewStats = (sellerId) => api.get(`/review/seller/${sellerId}/stats`);

// 创建评价回复
api.createReply = (data) => api.post('/review/reply', data);

// 更新评价回复
api.updateReply = (id, data) => api.put(`/review/reply/${id}`, data);

// 删除评价回复
api.deleteReply = (id) => api.delete(`/review/reply/${id}`);

export default api;
