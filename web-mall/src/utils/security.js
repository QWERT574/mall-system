/**
 * 安全工具函数
 * 防止 XSS 攻击
 */

/**
 * 转义 HTML 特殊字符
 * @param {string} str - 需要转义的字符串
 * @returns {string} - 转义后的字符串
 */
export const escapeHTML = (str) => {
  if (!str) return '';
  const escapeMap = {
    '<': '&lt;',
    '>': '&gt;',
    '&': '&amp;',
    '"': '&quot;',
    "'": '&#39;'
  };
  return str.replace(/[<>&"']/g, (match) => escapeMap[match]);
};

/**
 * 清理 HTML 标签（白名单模式）
 * @param {string} html - 原始 HTML 字符串
 * @returns {string} - 清理后的 HTML
 */
export const sanitizeHTML = (html) => {
  if (!html) return '';
  
  // 允许的标签白名单
  const allowedTags = ['p', 'br', 'strong', 'em', 'u', 'b', 'i', 'ul', 'ol', 'li'];
  
  // 移除所有不允许的标签
  let sanitized = html.replace(/<\/?(\w+)[^>]*>/g, (match, tagName) => {
    return allowedTags.includes(tagName.toLowerCase()) ? match : '';
  });
  
  // 移除所有 on* 事件处理器
  sanitized = sanitized.replace(/\s*on\w+\s*=\s*["'][^"']*["']/gi, '');
  
  return sanitized;
};

/**
 * 清理搜索关键词
 * @param {string} keyword - 搜索关键词
 * @returns {string} - 清理后的关键词
 */
export const sanitizeKeyword = (keyword) => {
  if (!keyword) return '';
  // 移除特殊字符和 HTML 标签
  return keyword.replace(/[<>"'&]/g, '').trim();
};

/**
 * 验证手机号格式
 * @param {string} phone - 手机号
 * @returns {boolean} - 是否有效
 */
export const validatePhone = (phone) => {
  const regex = /^[1][3-9][0-9]{9}$/;
  return regex.test(phone);
};

/**
 * 验证密码强度
 * @param {string} password - 密码
 * @returns {Object} - 验证结果
 */
export const validatePassword = (password) => {
  if (!password || password.length < 6 || password.length > 20) {
    return { valid: false, message: '密码长度必须在 6-20 位之间' };
  }
  
  // 检查是否包含字母和数字
  const hasLetter = /[a-zA-Z]/.test(password);
  const hasNumber = /\d/.test(password);
  
  if (!hasLetter || !hasNumber) {
    return { valid: false, message: '密码必须包含字母和数字' };
  }
  
  return { valid: true, message: '密码强度合格' };
};
