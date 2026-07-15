const { get, post } = require('../../utils/request');

Page({
  data: {
    sessionId: null,
    defaultSellerId: null,
    sessionList: [],
    messages: [],
    inputText: '',
    currentSellerName: '在线客服',
    scrollToView: ''
  },

  onLoad(options) {
    // 支持 ?id=xxx (已有会话) 或 ?sellerId=xxx (新建会话) 或 ?orderId=xxx (从订单页进入)
    if (options.id) {
      this.setData({ sessionId: options.id });
      this.loadMessages();
    } else if (options.sellerId) {
      // 从商品/订单页带 sellerId 进入,自动创建客服会话
      this.createSession(options.sellerId, options.productId, options.orderId);
    } else {
      // 无参数:加载会话列表,允许用户在底部输入框直接发起咨询
      this.loadSessionList();
    }
  },

  // 创建/获取客服会话
  async createSession(sellerId, productId, orderId) {
    try {
      // 小程序的 post() 不支持 params,需手动拼 query string
      const query = ['sellerId=' + encodeURIComponent(sellerId)];
      if (productId) query.push('productId=' + encodeURIComponent(productId));
      if (orderId) query.push('orderId=' + encodeURIComponent(orderId));
      const url = '/api/chat/session' + (query.length ? '?' + query.join('&') : '');
      const session = await post(url, null, false);
      if (session && session.id) {
        this.setData({
          sessionId: session.id,
          currentSellerName: session.sellerName || '在线客服',
          defaultSellerId: sellerId
        });
        this.loadMessages();
      }
    } catch (error) {
      console.error('创建会话失败:', error);
      // 即使创建失败,仍记录 sellerId,让用户可继续输入,发送时再尝试
      this.setData({ defaultSellerId: sellerId });
      wx.showToast({ title: '会话创建失败,请稍后重试', icon: 'none' });
    }
  },

  async loadSessionList() {
    try {
      // 后端实际接口: GET /api/chat/sessions
      const list = await get('/api/chat/sessions', {}, false);
      this.setData({
        sessionList: list || []
      });
    } catch (error) {
      console.error('加载会话列表失败:', error);
      this.setData({ sessionList: [] });
    }
  },

  openSession(e) {
    const session = e.currentTarget.dataset.session;
    this.setData({
      sessionId: session.id,
      currentSellerName: session.sellerName || '在线客服'
    });
    this.loadMessages();
  },

  async loadMessages() {
    try {
      // 后端实际接口: GET /api/chat/messages/{sessionId}?afterId=0
      const messages = await get('/api/chat/messages/' + this.data.sessionId, { afterId: 0 }, false);
      this.setData({
        messages: messages || []
      });
      this.scrollToBottom();
    } catch (error) {
      console.error('加载消息失败:', error);
      this.setData({ messages: [] });
    }
  },

  async sendMessage() {
    const content = this.data.inputText.trim();
    if (!content) return;

    // 如果没有 sessionId,先用默认客服 seller 创建一个会话
    if (!this.data.sessionId) {
      const sellerId = this.data.defaultSellerId || 11; // 默认客服 seller
      try {
        wx.showLoading({ title: '正在连接客服...', mask: true });
        await this.createSession(sellerId);
        wx.hideLoading();
        if (!this.data.sessionId) {
          wx.showToast({ title: '会话创建失败,请稍后重试', icon: 'none' });
          return;
        }
      } catch (e) {
        wx.hideLoading();
        wx.showToast({ title: '会话创建失败,请稍后重试', icon: 'none' });
        return;
      }
    }

    const sessionId = this.data.sessionId;
    const tempMsg = {
      id: Date.now(),
      content,
      messageType: 1,
      senderType: 1
    };

    this.setData({
      messages: [...this.data.messages, tempMsg],
      inputText: ''
    });

    this.scrollToBottom();

    try {
      // 后端接口: POST /api/chat/message
      // 后端兼容: 优先读取 query, 否则读取 JSON body
      const data = { sessionId, content };
      await post('/api/chat/message', data, false);
      // 发送成功后, 重新加载消息以同步服务器状态
      this.loadMessages();
    } catch (error) {
      console.error('发送消息失败:', error);
      wx.showToast({ title: '发送失败', icon: 'none' });
    }
  },

  onInput(e) {
    this.setData({ inputText: e.detail.value });
  },

  goBack() {
    wx.navigateBack();
  },

  applyIntervention() {
    wx.showModal({
      title: '申请介入',
      content: '确定要申请平台介入处理吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            wx.showLoading({ title: '提交中...' });
            await post('/api/cs/request-intervention', {
              sessionId: this.data.currentSessionId || this.data.sessionId
            });
            wx.hideLoading();
            wx.showToast({ title: '介入申请已提交', icon: 'success' });
          } catch (e) {
            wx.hideLoading();
            wx.showToast({ title: '提交失败', icon: 'none' });
          }
        }
      }
    });
  },

  scrollToBottom() {
    setTimeout(() => {
      const msgs = this.data.messages;
      if (msgs.length > 0) {
        this.setData({ scrollToView: `msg-${msgs[msgs.length - 1].id}` });
      }
    }, 100);
  }
});
