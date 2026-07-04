// 支付工具类，封装统一的支付流程
const app = getApp();
const { get, post } = require('./request');

/**
 * 统一支付方法
 * 后端 /api/order/pay/{id} 直接完成支付（模拟支付，无需微信支付参数）
 * @param {number} orderId - 订单ID
 * @param {function} onSuccess - 支付成功回调
 * @param {function} onFail - 支付失败回调
 * @param {function} onComplete - 支付完成回调
 * @param {string} paymentMethod - 支付方式
 */
const payOrder = async (orderId, onSuccess, onFail, onComplete, paymentMethod = 'wechat') => {
  // 参数验证
  orderId = Number(orderId);
  if (!orderId || orderId <= 0) {
    const errorMsg = '无效的订单ID';
    wx.showToast({ title: errorMsg, icon: 'error', duration: 2000 });
    if (typeof onFail === 'function') onFail(new Error(errorMsg));
    if (typeof onComplete === 'function') onComplete();
    return false;
  }

  let isPaymentSuccess = false;
  let paymentError = null;

  try {
    wx.showLoading({ title: '支付中...', mask: true });

    // 调用后端支付接口，后端直接完成支付
    console.log(`开始支付订单 ${orderId}`);
    await post(`/api/order/pay/${orderId}`, { paymentMethod });

    wx.hideLoading();
    wx.showToast({ title: '支付成功', icon: 'success', duration: 2000 });

    if (typeof onSuccess === 'function') onSuccess();
    isPaymentSuccess = true;
    return true;
  } catch (error) {
    wx.hideLoading();

    console.error('支付失败:', error);
    paymentError = error;

    let errorMsg = '支付失败';
    if (error && error.message) {
      errorMsg = error.message;
    }
    wx.showToast({ title: errorMsg, icon: 'none', duration: 3000 });

    if (typeof onFail === 'function') onFail(error);
    return false;
  } finally {
    if (typeof onComplete === 'function') onComplete(isPaymentSuccess, paymentError);
  }
};

/**
 * 获取订单支付状态
 */
const getOrderPayStatus = async (orderId) => {
  orderId = Number(orderId);
  if (!orderId || orderId <= 0) {
    throw new Error('无效的订单ID');
  }
  try {
    const order = await get(`/api/order/detail/${orderId}`);
    return {
      orderId: order.id,
      status: order.status,
      statusText: getOrderStatusText(order.status),
      payTime: order.payTime,
      orderSn: order.orderSn
    };
  } catch (error) {
    console.error('获取订单支付状态失败:', error);
    throw error;
  }
};

/**
 * 获取订单状态文本
 */
const getOrderStatusText = (status) => {
  const statusMap = {
    0: '待支付',
    1: '已支付',
    2: '已发货',
    3: '已完成',
    4: '已取消'
  };
  return statusMap[status] || '未知状态';
};

/**
 * 检查订单是否可以支付
 */
const canPayOrder = (order) => {
  if (!order) return false;
  return order.status === 0;
};

module.exports = {
  payOrder,
  getOrderPayStatus,
  getOrderStatusText,
  canPayOrder
};
