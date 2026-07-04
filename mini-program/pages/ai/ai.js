const app = getApp();
const { post, get, resolveImageUrl } = require('../../utils/request');
// 商品默认占位图（后端 static/images/ 目录下）
const DEFAULT_PRODUCT_IMG = '/images/product-default.svg';
Page({
  data: {
    messages: [], // 消息列表
    inputContent: '', // 输入框内容
    activeType: 1, // 当前服务类型：1.商品查询，2.物流查询，3.售后咨询
    isTyping: false, // AI是否正在输入
    scrollToView: '', // 滚动到指定消息
    messageId: 0, // 消息ID计数器
    userId: 0 // 当前用户ID
  },

  onLoad() {
    // 页面加载时初始化数据
    this.initData();
  },

  // 初始化数据
  initData() {
    // 获取当前用户ID
    const userId = app.globalData.userInfo ? app.globalData.userInfo.id : 0;
    this.setData({ userId });
    
    // 可以从缓存中恢复历史消息，使用用户ID作为缓存键
    const cacheKey = `ai_history_messages_${userId}`;
    const historyMessages = wx.getStorageSync(cacheKey) || [];
    if (historyMessages.length > 0) {
      this.setData({
        messages: historyMessages,
        messageId: historyMessages.length > 0 ? parseInt(historyMessages[historyMessages.length - 1].id.split('_')[1]) : 0
      });
      // 滚动到底部
      this.scrollToBottom();
    }
  },

  // 切换服务类型
  switchServiceType(e) {
    const type = parseInt(e.currentTarget.dataset.type);
    this.setData({
      activeType: type
    });
    // 可以在这里添加服务类型切换的逻辑，比如清空消息或发送欢迎消息
    wx.showToast({
      title: `已切换到${this.getServiceTypeName(type)}`,
      icon: 'none'
    });
  },

  // 获取服务类型名称
  getServiceTypeName(type) {
    const typeMap = {
      1: '商品查询',
      2: '物流查询',
      3: '售后咨询'
    };
    return typeMap[type] || '商品查询';
  },

  // 输入内容变化
  onInput(e) {
    this.setData({
      inputContent: e.detail.value
    });
  },

  // 发送消息
  async sendMessage() {
    const content = this.data.inputContent.trim();
    if (!content) {
      return;
    }

    // 添加用户消息到消息列表
    const userMessage = {
      id: `msg_${++this.data.messageId}`,
      type: 'user',
      content: content,
      timestamp: Date.now()
    };

    const messages = [...this.data.messages, userMessage];
    this.setData({
      messages: messages,
      inputContent: '',
      isTyping: true
    });

    // 滚动到底部
    this.scrollToBottom();

    try {
      // 调用AI查询接口
      const aiResponse = await post('/api/ai/query', {
        query: content,
        serviceType: this.data.activeType,
        userId: this.data.userId // 使用当前用户ID
      });

      // 处理AI回复，将商品ID转换为可点击的格式
      let responseContent = aiResponse.response || '抱歉，暂时无法回答您的问题，请稍后重试';
      
      // 生成富文本内容，将商品信息转换为可点击的链接
      const richContent = this.generateRichContent(responseContent);
      
      // 解析出的商品信息列表
      const products = this.parseProductsFromResponse(responseContent);
      
      // 获取后端直接返回的商品卡片列表
      const productCards = (aiResponse.productCards || []).map(card => {
        // 兜底：URL 为空或解析失败时使用默认占位图
        card.coverUrl = resolveImageUrl(card.cover || card.image) || DEFAULT_PRODUCT_IMG;
        return card;
      });
      
      // 添加AI回复到消息列表
      const aiMessage = {
        id: `msg_${++this.data.messageId}`,
        type: 'ai',
        content: responseContent,
        richContent: richContent,
        timestamp: Date.now(),
        // 解析出的商品信息列表，用于生成商品卡片
        products: products,
        // 直接使用后端返回的商品卡片，用于显示商品卡片
        productDetails: productCards
      };
      
      // 先添加消息到列表
      messages.push(aiMessage);
      this.setData({
        messages: messages,
        isTyping: false
      });
      
      // 如果后端没有返回productCards，再获取商品详情
      if (products.length > 0 && productCards.length === 0) {
        this.loadProductDetails(products, aiMessage, messages);
      }
      
      // 保存历史消息到缓存，使用用户ID作为缓存键
      const cacheKey = `ai_history_messages_${this.data.userId}`;
      wx.setStorageSync(cacheKey, messages);

      // 滚动到底部
      this.scrollToBottom();
    } catch (error) {
      console.error('AI查询失败:', error);
      // 添加错误消息到消息列表
      let errorContent = '抱歉，暂时无法回答您的问题，请稍后重试';
      // 检查错误类型，提供更具体的错误信息
      if (error.statusCode === 500) {
        errorContent = 'AI服务暂时不可用，请稍后重试';
      } else if (error.statusCode === 404) {
        errorContent = 'AI服务未找到，请检查网络连接';
      } else if (error.statusCode === 403) {
        errorContent = 'AI服务访问受限，请稍后重试';
      } else if (error.errMsg) {
        errorContent = '网络请求失败：' + error.errMsg;
      }
      
      const errorMessage = {
        id: `msg_${++this.data.messageId}`,
        type: 'ai',
        content: errorContent,
        timestamp: Date.now()
      };

      messages.push(errorMessage);
      this.setData({
        messages: messages,
        isTyping: false
      });

      // 滚动到底部
      this.scrollToBottom();
    }
  },

  // 发送建议消息
  sendSuggestion(e) {
    const content = e.currentTarget.dataset.content;
    this.setData({
      inputContent: content
    });
    this.sendMessage();
  },

  // 滚动到底部
  scrollToBottom() {
    setTimeout(() => {
      this.setData({
        scrollToView: `msg_${this.data.messageId}`
      });
    }, 100);
  },

  // 清空消息历史
  clearHistory() {
    wx.showModal({
      title: '清空历史',
      content: '确定要清空所有聊天记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            messages: [],
            messageId: 0
          });
          // 使用用户ID作为缓存键
          const cacheKey = `ai_history_messages_${this.data.userId}`;
          wx.removeStorageSync(cacheKey);
          wx.showToast({
            title: '历史消息已清空',
            icon: 'success'
          });
        }
      }
    });
  },
  
  // 从AI回复中解析出商品信息
  parseProductsFromResponse(response) {
    const products = [];
    // 正则表达式匹配商品名称(ID:XXX)格式
    const productRegex = /([^，；。]+)\(ID:(\d+)\)/g;
    let match;
    while ((match = productRegex.exec(response)) !== null) {
      products.push({
        name: match[1].trim(),
        id: parseInt(match[2])
      });
    }
    return products;
  },
  
  // 生成富文本内容，将商品信息转换为可点击的链接
  generateRichContent(content) {
    // 正则表达式匹配商品名称(ID:XXX)格式
    const productRegex = /([^，；。]+)\(ID:(\d+)\)/g;
    let result = content;
    let match;
    const matches = [];
    
    // 先收集所有匹配的商品信息
    while ((match = productRegex.exec(content)) !== null) {
      matches.push({
        fullText: match[0],
        name: match[1].trim(),
        id: parseInt(match[2]),
        index: match.index
      });
    }
    
    // 从后往前替换，避免索引偏移
    for (let i = matches.length - 1; i >= 0; i--) {
      const match = matches[i];
      const before = result.substring(0, match.index);
      const after = result.substring(match.index + match.fullText.length);
      result = before + `<a href="javascript:void(0);" data-id="${match.id}" style="color: #4CAF50; text-decoration: underline;" class="product-link">${match.name}</a>(ID:${match.id})` + after;
    }
    
    return result;
  },
  
  // 获取商品详情，用于显示商品卡片
  async loadProductDetails(products, aiMessage, messages) {
    try {
      const productDetails = [];
      
      // 并行获取所有商品详情
      for (let product of products) {
        try {
          const productDetail = await get(`/api/product/${product.id}`);
          // 兜底：URL 为空或解析失败时使用默认占位图
          productDetail.coverUrl = resolveImageUrl(productDetail.cover) || DEFAULT_PRODUCT_IMG;
          productDetails.push(productDetail);
        } catch (error) {
          console.error(`获取商品${product.id}详情失败:`, error);
        }
      }
      
      // 更新消息中的商品详情
      aiMessage.productDetails = productDetails;
      
      // 更新消息列表
      this.setData({
        messages: messages
      });
      
      // 保存历史消息到缓存，使用用户ID作为缓存键
      const cacheKey = `ai_history_messages_${this.data.userId}`;
      wx.setStorageSync(cacheKey, messages);
      
      // 滚动到底部
      this.scrollToBottom();
    } catch (error) {
      console.error('获取商品详情失败:', error);
    }
  },
  
  // 处理富文本点击事件
  onRichTextTap(e) {
    // 微信小程序 rich-text 的 tap 事件可通过 e.detail 检查
    // 但实际上 rich-text 不传递自定义 data 属性
    // 商品链接点击已通过商品卡片实现，此处不再处理
  },
  
  // 跳转到商品详情页
  navigateToProductDetail(e) {
    const productId = e.currentTarget.dataset.id;
    wx.navigateTo({
      url: `/pages/product/product?id=${productId}`
    });
  },

  // 商品图片加载失败：兜底替换为默认占位图
  onProductImageError(e) {
    const { msgidx, pidx } = e.currentTarget.dataset;
    if (msgidx === undefined || pidx === undefined) return;
    const path = `messages[${msgidx}].productDetails[${pidx}].coverUrl`;
    if (!this.data.messages[msgidx] ||
        !this.data.messages[msgidx].productDetails ||
        !this.data.messages[msgidx].productDetails[pidx]) return;
    // 避免重复设置（防止循环触发）
    if (this.data.messages[msgidx].productDetails[pidx].coverUrl === DEFAULT_PRODUCT_IMG) return;
    this.setData({ [path]: DEFAULT_PRODUCT_IMG });
  }
});
