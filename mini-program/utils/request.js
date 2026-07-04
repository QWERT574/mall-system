// 网络请求封装
const app = getApp();

// 默认请求头
const DEFAULT_HEADERS = {
  'content-type': 'application/json',
  'User-Type': '0' // 默认买家角色
};

// 登录锁，防止并发重复登录
let loginPromise = null;

// 强制登录获取token
const ensureLogin = () => {
  if (loginPromise) return loginPromise;

  loginPromise = new Promise((resolve) => {
    // 先检查是否已有token
    const existingToken = wx.getStorageSync('token') || app.globalData.token;
    if (existingToken) {
      loginPromise = null;
      resolve(existingToken);
      return;
    }

    // 用默认账号登录
    wx.request({
      url: app.globalData.backendBaseUrl + '/api/auth/login',
      method: 'POST',
      data: { username: 'user', password: '123456' },
      header: { 'content-type': 'application/json' },
      success: (res) => {
        if (res.statusCode === 200 && res.data && res.data.code === 0 && res.data.data && res.data.data.token) {
          const token = res.data.data.token;
          app.globalData.token = token;
          app.globalData.userInfo = res.data.data.user;
          wx.setStorageSync('token', token);
          wx.setStorageSync('userInfo', res.data.data.user);
          console.log('自动登录成功，token已保存');
          loginPromise = null;
          resolve(token);
        } else {
          console.warn('自动登录失败:', res.data);
          loginPromise = null;
          resolve('');
        }
      },
      fail: (err) => {
        console.warn('自动登录网络错误:', err.errMsg);
        loginPromise = null;
        resolve('');
      }
    });
  });

  return loginPromise;
};

/**
 * 网络请求封装函数（支持403自动重试）
 */
const request = (url, data = {}, method = 'GET', headers = {}, showLoading = true, loadingText = '加载中...', _retryCount = 0) => {
  return new Promise((resolve, reject) => {
    // 获取token
    const token = wx.getStorageSync('token') || app.globalData.token;

    // 如果没有token，先登录
    const doRequest = (currentToken) => {
      if (showLoading) {
        wx.showLoading({ title: loadingText, mask: true });
      }

      const requestHeaders = { ...DEFAULT_HEADERS, ...headers };
      if (currentToken) {
        requestHeaders['Authorization'] = `Bearer ${currentToken}`;
      }

      wx.request({
        url: app.globalData.backendBaseUrl + url,
        data,
        method,
        header: requestHeaders,
        timeout: 30000,
        success: (res) => {
          if (res.statusCode === 200) {
            if (res.data && res.data.code === 0) {
              resolve(res.data.data);
            } else {
              wx.showToast({ title: (res.data && res.data.message) || '请求失败', icon: 'none', duration: 2000 });
              reject(res.data);
            }
          } else if ((res.statusCode === 401 || res.statusCode === 403) && _retryCount < 1) {
            // token无效或过期，清除旧token，重新登录后重试
            console.log(`请求${url}返回${res.statusCode}，尝试重新登录`);
            app.globalData.token = '';
            wx.removeStorageSync('token');
            ensureLogin().then(newToken => {
              if (newToken) {
                // 重试请求
                request(url, data, method, headers, showLoading, loadingText, _retryCount + 1)
                  .then(resolve).catch(reject);
              } else {
                wx.showToast({ title: '登录失败，请手动登录', icon: 'none', duration: 2000 });
                reject(res);
              }
            });
          } else if (res.statusCode === 401) {
            wx.showToast({ title: '未授权访问', icon: 'none', duration: 2000 });
            reject(res);
          } else if (res.statusCode === 403) {
            wx.showToast({ title: '没有权限访问', icon: 'none', duration: 2000 });
            reject(res);
          } else {
            wx.showToast({ title: `请求失败(${res.statusCode})`, icon: 'none', duration: 2000 });
            reject(res);
          }
        },
        fail: (err) => {
          wx.showToast({ title: '网络请求失败', icon: 'none', duration: 2000 });
          reject(err);
        },
        complete: () => {
          if (showLoading) {
            wx.hideLoading();
          }
        }
      });
    };

    if (token) {
      doRequest(token);
    } else {
      ensureLogin().then(doRequest);
    }
  });
};

// GET请求封装
const get = (url, data = {}, showLoading = true, loadingText = '加载中...') => {
  return request(url, data, 'GET', {}, showLoading, loadingText);
};

// POST请求封装
const post = (url, data = {}, showLoading = true, loadingText = '提交中...') => {
  return request(url, data, 'POST', {}, showLoading, loadingText);
};

// PUT请求封装
const put = (url, data = {}, showLoading = true, loadingText = '更新中...') => {
  return request(url, data, 'PUT', {}, showLoading, loadingText);
};

// DELETE请求封装
const del = (url, data = {}, showLoading = true, loadingText = '删除中...') => {
  return request(url, data, 'DELETE', {}, showLoading, loadingText);
};

// 将相对路径图片URL转为完整URL
const resolveImageUrl = (url) => {
  if (!url) return '';
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url;
  if (url.startsWith('/')) return app.globalData.backendBaseUrl + url;
  return url;
};

module.exports = {
  get,
  post,
  put,
  del,
  request,
  resolveImageUrl
};
