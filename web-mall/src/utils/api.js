import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api',
  timeout: parseInt(import.meta.env.VITE_REQUEST_TIMEOUT) || 10000,
  headers: {
    'Content-Type': 'application/json'
  }
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token 和用户信息
    const token = localStorage.getItem('token');
    let userInfo = null;
    try {
      userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}');
    } catch (e) {
      console.error('解析用户信息失败:', e);
    }
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 添加用户类型请求头（与后端 DB 约定保持一致：0=买家, 1=卖家, 2=管理员）
    if (userInfo && userInfo.userType !== undefined && userInfo.userType !== null) {
      config.headers['User-Type'] = String(userInfo.userType);
    } else {
      // 未登录或未取到用户信息时默认为买家
      config.headers['User-Type'] = '0';
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
    if (data.code === 0 || data.code === 200) {
      return data;
    } else {
      // 业务错误，抛出异常
      return Promise.reject(new Error(data.message || '请求失败'));
    }
  },
  error => {
    // 统一处理网络错误
    let message = '网络请求失败';
    if (error.response) {
      // HTTP 错误状态码处理
      // 优先使用后端返回的错误消息
      const backendMessage = error.response.data?.message;
      const isSpringSecurityBlock = !backendMessage; // 响应体无 message 一般是 Spring Security 直接拦截

      switch (error.response.status) {
        case 401:
          message = backendMessage || '未登录或登录已过期，请重新登录';
          // 清除本地存储，跳转到登录页
          localStorage.removeItem('token');
          localStorage.removeItem('userInfo');
          if (!window.location.hash.includes('/login')) {
            window.location.href = '#/login';
          }
          break;
        case 403:
          // 403 且后端无 message，多半是 token 失效 / 未登录被 Spring Security 拦下
          if (isSpringSecurityBlock) {
            const hasToken = !!localStorage.getItem('token');
            message = hasToken ? '登录已过期或权限不足，请重新登录' : '请先登录后再操作';
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            if (!window.location.hash.includes('/login')) {
              window.location.href = '#/login';
            }
          } else {
            message = backendMessage;
          }
          break;
        case 404:
          message = backendMessage || '请求的资源不存在';
          break;
        case 500:
          message = backendMessage || '服务器内部错误';
          break;
        default:
          message = backendMessage || `请求失败 (${error.response.status})`;
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      message = '服务器无响应，请稍后重试';
    }
    console.error('API 错误详情:', error.response?.data);
    return Promise.reject(new Error(message));
  }
);

// 登录
api.login = (data) => api.post('/auth/login', data);

// 获取图形验证码:GET /api/captcha/image?key=xxx
// 不传 key 时由后端生成新 key,传 key 时用于刷新
api.getCaptcha = (key) => api.get('/captcha/image', { params: key ? { key } : {} });

// 注册
api.register = (data) => api.post('/auth/register', data);

// 发送短信验证码: POST /api/sms/send  body: { phone }
// 返回 {code:0, message:"...", data: { devCode: "1234" }}
// dev 模式下 data.devCode 直接返回验证码,方便测试
api.sendSmsCode = (phone) => api.post('/sms/send', { phone });

// 验证短信验证码: POST /api/sms/verify  body: { phone, code }
api.verifySmsCode = (phone, code) => api.post('/sms/verify', { phone, code });

// 发送验证码
api.sendCode = (phone) => api.get(`/auth/sendCode?phone=${phone}`);

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

// 获取产品列表
api.getProducts = (params) => api.get('/product/list', { params });

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

// 上传评价图片
api.uploadReviewImages = (files) => {
  const formData = new FormData();
  files.forEach((file, index) => {
    formData.append('files', file);
  });
  return api.post('/upload/review', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
};

// 通用图片上传
api.uploadImage = (file, subDir) => {
  const formData = new FormData();
  formData.append('file', file);
  if (subDir) {
    formData.append('subDir', subDir);
  }
  return api.post('/upload/image', formData, {
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

// 获取商家信息
api.getSellerInfo = (sellerId) => api.get(`/seller/public/${sellerId}`);

// ========== 客服聊天相关 API ==========
// 创建售后服务工单
api.createAfterSale = (data) => api.post('/aftersale', data);

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

// ========== 评价相关 API ==========
// 获取商品评价列表
api.getProductReviews = (productId, params) => api.get(`/review/product/${productId}`, { params });

// 创建评价
api.createReview = (data) => api.post('/review', data);

// 获取评价详情
api.getReviewDetail = (id) => api.get(`/review/${id}`);

// 更新评价
api.updateReview = (id, data) => api.put(`/review/${id}`, data);

// 删除评价
api.deleteReview = (id) => api.delete(`/review/${id}`);

// 创建评价回复
api.createReply = (data) => api.post('/review/reply', data);

// 获取评价回复
api.getReviewReplies = (reviewId) => api.get(`/review/reply/${reviewId}`);

// ========== 售后服务相关 API ==========
// 创建售后服务工单
api.createAfterSale = (data) => api.post('/aftersale', data);

// 获取售后服务详情
api.getAfterSaleById = (id) => api.get(`/aftersale/${id}`);

// 获取用户的售后服务列表
api.getAfterSalesByUserId = (userId) => api.get(`/aftersale/user/${userId}`);

// 分页获取用户的售后服务列表
api.getAfterSalesByUserIdWithPage = (userId, page = 1, size = 10, status) => {
  const params = { page, size };
  if (status !== undefined && status !== null) {
    params.status = status;
  }
  return api.get(`/aftersale/user/${userId}/page`, { params });
};

// 获取订单的售后服务列表
api.getAfterSalesByOrderId = (orderId) => api.get(`/aftersale/order/${orderId}`);

// 更新售后服务
api.updateAfterSale = (id, data) => api.put(`/aftersale/${id}`, data);

// 处理售后服务（商家用）
api.processAfterSale = (id, data) => api.post(`/aftersale/${id}/process`, data);

// 补充证据
api.addSupplementaryEvidence = (id, data) => api.post(`/aftersale/${id}/evidence`, data);

// 取消售后申请
api.cancelAfterSale = (id, data) => api.post(`/aftersale/${id}/cancel`, data);

// 填写退货物流
api.updateReturnLogistics = (id, data) => api.put(`/aftersale/${id}/logistics`, data);

// 获取服务记录
api.getServiceRecords = (id) => api.get(`/aftersale/${id}/records`);

// 删除售后服务
api.deleteAfterSale = (id) => api.delete(`/aftersale/${id}`);

export default api;
