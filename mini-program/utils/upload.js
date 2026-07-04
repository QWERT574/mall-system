// utils/upload.js
/**
 * 图片上传工具类
 */

const app = getApp();

/**
 * 选择并上传图片
 * @param {Object} options 配置项
 * @param {Number} options.count 最多选择图片数量，默认 9
 * @param {String} options.sourceType 图片来源，默认 ['album', 'camera']
 * @param {String} options.subDir 上传子目录
 * @param {Function} options.onProgress 进度回调
 * @param {Function} options.onSuccess 成功回调
 * @param {Function} options.onFail 失败回调
 */
const uploadImages = (options = {}) => {
  const {
    count = 9,
    sourceType = ['album', 'camera'],
    subDir = 'products',
    onProgress,
    onSuccess,
    onFail
  } = options;

  // 选择图片
  wx.chooseImage({
    count: count,
    sourceType: sourceType,
    success: (res) => {
      const tempFilePaths = res.tempFilePaths;
      const uploadTasks = [];

      // 上传每张图片
      tempFilePaths.forEach((filePath, index) => {
        const uploadTask = uploadImage(filePath, subDir, onProgress);
        uploadTasks.push(uploadTask);
      });

      // 等待所有上传完成
      Promise.all(uploadTasks)
        .then((results) => {
          const successResults = results.filter(r => r.success);
          const failResults = results.filter(r => !r.success);

          if (onSuccess) {
            onSuccess({
              successCount: successResults.length,
              failCount: failResults.length,
              results: successResults,
              fails: failResults
            });
          }
        })
        .catch((error) => {
          console.error('批量上传失败:', error);
          if (onFail) {
            onFail(error);
          }
        });
    },
    fail: (error) => {
      console.error('选择图片失败:', error);
      if (onFail) {
        onFail(error);
      }
    }
  });
};

/**
 * 上传单张图片
 * @param {String} filePath 本地文件路径
 * @param {String} subDir 上传子目录
 * @param {Function} onProgress 进度回调
 * @returns {Promise}
 */
const uploadImage = (filePath, subDir, onProgress) => {
  return new Promise((resolve, reject) => {
    // 获取文件信息
    wx.getFileInfo({
      filePath: filePath,
      success: (fileInfo) => {
        // 验证文件大小（10MB）
        if (fileInfo.size > 10 * 1024 * 1024) {
          wx.showToast({
            title: '图片大小不能超过 10MB',
            icon: 'none'
          });
          resolve({
            success: false,
            message: '图片过大',
            size: fileInfo.size
          });
          return;
        }

        // 上传文件
        const uploadTask = wx.uploadFile({
          url: app.globalData.backendBaseUrl + '/api/upload/image',
          filePath: filePath,
          name: 'file',
          formData: {
            subDir: subDir
          },
          header: {
            'Authorization': wx.getStorageSync('token') ? `Bearer ${wx.getStorageSync('token')}` : ''
          },
          success: (res) => {
            try {
              const data = JSON.parse(res.data);
              if (data.code === 0) {
                resolve({
                  success: true,
                  url: data.data.url,
                  fileName: data.data.fileName,
                  originalName: data.data.originalName,
                  fileSize: data.data.fileSize,
                  path: data.data.path
                });
              } else {
                resolve({
                  success: false,
                  message: data.message || '上传失败'
                });
              }
            } catch (e) {
              reject(e);
            }
          },
          fail: (error) => {
            reject(error);
          }
        });

        // 监听上传进度
        if (onProgress) {
          uploadTask.onProgressUpdate((progressRes) => {
            onProgress(progressRes.progress);
          });
        }
      },
      fail: (error) => {
        reject(error);
      }
    });
  });
};

/**
 * 上传头像
 * @param {Object} options 配置项
 * @param {Function} options.onSuccess 成功回调
 * @param {Function} options.onFail 失败回调
 */
const uploadAvatar = (options = {}) => {
  const { onSuccess, onFail } = options;

  wx.chooseImage({
    count: 1,
    sourceType: ['album', 'camera'],
    success: (res) => {
      const filePath = res.tempFilePaths[0];

      // 获取文件信息
      wx.getFileInfo({
        filePath: filePath,
        success: (fileInfo) => {
          // 验证文件大小（5MB）
          if (fileInfo.size > 5 * 1024 * 1024) {
            wx.showToast({
              title: '头像大小不能超过 5MB',
              icon: 'none'
            });
            if (onFail) {
              onFail(new Error('图片过大'));
            }
            return;
          }

          // 上传头像
          wx.uploadFile({
            url: app.globalData.backendBaseUrl + '/api/upload/avatar',
            filePath: filePath,
            name: 'file',
            header: {
              'Authorization': wx.getStorageSync('token') ? `Bearer ${wx.getStorageSync('token')}` : ''
            },
            success: (res) => {
              try {
                const data = JSON.parse(res.data);
                if (data.code === 0) {
                  if (onSuccess) {
                    onSuccess(data.data);
                  }
                } else {
                  wx.showToast({
                    title: data.message || '上传失败',
                    icon: 'none'
                  });
                  if (onFail) {
                    onFail(new Error(data.message));
                  }
                }
              } catch (e) {
                if (onFail) {
                  onFail(e);
                }
              }
            },
            fail: (error) => {
              wx.showToast({
                title: '上传失败',
                icon: 'none'
              });
              if (onFail) {
                onFail(error);
              }
            }
          });
        }
      });
    },
    fail: (error) => {
      if (onFail) {
        onFail(error);
      }
    }
  });
};

/**
 * 预览图片
 * @param {Array} urls 图片 URL 列表
 * @param {Number} current 当前显示的图片索引
 */
const previewImages = (urls, current = 0) => {
  wx.previewImage({
    urls: urls,
    current: current
  });
};

/**
 * 删除图片
 * @param {String} url 图片 URL
 * @param {Function} callback 回调函数
 */
const deleteImage = (url, callback) => {
  wx.showModal({
    title: '提示',
    content: '确定要删除这张图片吗？',
    success: (res) => {
      if (res.confirm) {
        // 调用后端删除接口
        const token = wx.getStorageSync('token');
        
        wx.request({
          url: app.globalData.backendBaseUrl + '/api/upload/delete',
          method: 'POST',
          data: {
            url: url
          },
          header: {
            'Authorization': token ? `Bearer ${token}` : '',
            'Content-Type': 'application/json'
          },
          success: (res) => {
            if (res.data.code === 0) {
              wx.showToast({
                title: '删除成功',
                icon: 'success'
              });
              if (callback) {
                callback(true);
              }
            } else {
              wx.showToast({
                title: res.data.message || '删除失败',
                icon: 'none'
              });
              if (callback) {
                callback(false);
              }
            }
          },
          fail: () => {
            wx.showToast({
              title: '删除失败',
              icon: 'none'
            });
            if (callback) {
              callback(false);
            }
          }
        });
      }
    }
  });
};

module.exports = {
  uploadImages,
  uploadAvatar,
  previewImages,
  deleteImage
};
